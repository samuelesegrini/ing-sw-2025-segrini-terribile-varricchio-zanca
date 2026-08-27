package it.polimi.ingsw.server.controller;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.FlightEvent;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.server.model.game.Game;
import it.polimi.ingsw.server.model.game.Reaction;
import it.polimi.ingsw.server.model.game.Seat;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * One game, one thread, one command at a time.
 *
 * <p>Every command a game will ever see arrives here, goes on a queue, and is applied on a
 * single thread. That one decision is why nothing in the model has a lock in it, and why two
 * players reaching for the same face-up tile is settled by which command arrived first rather
 * than by which thread happened to run (architecture § 4).
 *
 * <p>Nothing about a transport reaches this far. A command is a command whether it came off a
 * socket or out of an RMI call, which is what requirement S5 needs and what stops the phases
 * from ever growing a special case.
 *
 * <p><b>Facts, then truth.</b> An accepted command is followed by whatever it narrated and then
 * by one {@code StateChanged} per player, built for that player. A refused one is followed by a
 * {@code Rejected} to the sender and nothing else, because a refused command changed nothing
 * and there is no new truth to send.
 */
public final class GameController implements AutoCloseable {

    /** How often to let the hourglass notice that it has run out. */
    private static final Duration TICK = Duration.ofMillis(500);

    /** How long to let the queue finish what it was doing before giving up on it. */
    private static final Duration SHUTDOWN_PATIENCE = Duration.ofSeconds(2);

    private final Game game;
    private final java.util.function.Consumer<Game> keeper;
    private final Map<PlayerColor, PlayerSession> sessions = new EnumMap<>(PlayerColor.class);
    private final ExecutorService queue;
    private final ScheduledExecutorService clock;

    private GamePhase lastAnnounced;
    private boolean ended;

    /**
     * Takes charge of a game.
     *
     * @param game the game to run
     */
    public GameController(Game game) {
        this(game, snapshot -> { });
    }

    /**
     * Runs a game, keeping it somewhere it can be found again.
     *
     * @param game    the game
     * @param keeper  told to write a snapshot whenever the game reaches a point worth keeping;
     *                a consumer rather than the store itself, because that is all this needs
     *                and a test can then watch when it is called
     */
    public GameController(Game game, java.util.function.Consumer<Game> keeper) {
        this.keeper = keeper;
        this.game = game;
        this.lastAnnounced = game.phase();
        game.seats().forEach(seat ->
                sessions.put(seat.colour(), new PlayerSession(seat.nickname(), seat.colour())));
        this.queue = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "game-" + game.id());
            thread.setDaemon(true);
            return thread;
        });
        this.clock = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "game-clock-" + game.id());
            thread.setDaemon(true);
            return thread;
        });
        // Written down as soon as it exists. A game is recoverable from the moment it is
        // dealt, not from the moment somebody first does something in it — a server that
        // stopped between the two would otherwise lose a table that was already seated.
        run(this::keep);
        this.clock.scheduleAtFixedRate(this::tick, TICK.toMillis(), TICK.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    // ------------------------------------------------------------------ connections

    /**
     * Points a seat at a connection and returns what should be done with what arrives on it.
     *
     * <p>Shaped to fit what both transports ask for, so that binding a player to a game reads
     * the same whichever they connected over:
     *
     * <pre>{@code
     * StreamChannel.over(socket, Event.class, Command.class,
     *                    channel -> controller.bind(colour, channel), liveness);
     * }</pre>
     *
     * <p>Binding a seat that is already attached replaces the connection and hangs up on the
     * old one. That is what a reconnection looks like from here, and doing anything else would
     * leave a game talking to a laptop that is not there.
     *
     * @param player  which seat
     * @param channel where to send that player's events
     * @return the listener for that connection
     * @throws IllegalArgumentException if there is no such player in this game
     */
    public ChannelListener<Command> bind(PlayerColor player, Channel<Event, Command> channel) {
        ChannelListener<Command> listener = attach(player, channel);
        announceArrival(player);
        return listener;
    }

    /**
     * Points a seat at a connection without telling anybody yet.
     *
     * <p>For seating a whole table at once. Announcing as each player is bound means the second
     * player never hears about the first, because the first was announced before the second had
     * a channel — so four players end up with four different accounts of the same moment.
     * Attaching everybody and then announcing gives them all the same one.
     *
     * @param player  which seat
     * @param channel where to send that player's events
     * @return the listener for that connection
     * @throws IllegalArgumentException if there is no such player in this game
     */
    public ChannelListener<Command> attach(PlayerColor player, Channel<Event, Command> channel) {
        PlayerSession session = sessionOf(player);
        Channel<Event, Command> replaced = session.attach(channel);
        if (replaced != null && replaced != channel) {
            replaced.close();
        }
        return new SeatListener(player, channel);
    }

    /**
     * Tells everybody a player is here, and tells that player everything.
     *
     * @param player who has arrived, or come back
     */
    public void announceArrival(PlayerColor player) {
        PlayerSession session = sessionOf(player);
        run(() -> {
            game.connectionChanged(player, true);
            announce(new GameEvent.ConnectionChanged(player, true));
            // The whole picture, which is all a returning player needs and exactly what a new
            // one needs too. There is no third case to write.
            session.send(new GameEvent.StateChanged(game.viewFor(player)));
        });
    }

    private void dropped(PlayerColor player, Channel<Event, Command> channel) {
        if (!sessionOf(player).detach(channel)) {
            // A late notice from a connection this seat has already moved on from. Hanging up
            // on the new one because the old one finally noticed it was dead is exactly the
            // bug this check exists to avoid.
            return;
        }
        run(() -> {
            game.connectionChanged(player, false);
            announce(new GameEvent.ConnectionChanged(player, false));
        });
    }

    /**
     * Tells whether anybody is currently attached to a seat.
     *
     * @param player which seat
     * @return {@code true} while a connection is bound to it
     */
    public boolean isAttached(PlayerColor player) {
        return sessionOf(player).isAttached();
    }

    // ------------------------------------------------------------------ commands

    /**
     * Queues a command.
     *
     * <p>Returns at once. The transport's reading thread must not be held up by a game, and a
     * game must not be at the mercy of how quickly a client's connection can be written to.
     *
     * @param player  who sent it, established from the connection rather than from the message
     * @param command what they want
     */
    public void submit(PlayerColor player, Command command) {
        run(() -> handle(player, command));
    }

    private void handle(PlayerColor player, Command command) {
        // Both arms written out, with no default and no cast. A third kind of Reaction would
        // otherwise have been read as an acceptance and published as one.
        switch (game.apply(player, command)) {
            // Only to the sender, and no state after it: nothing changed, so there is no new
            // truth to send and nobody else has anything to learn.
            case Reaction.Refused refused -> sessionOf(player)
                    .send(new GameEvent.Rejected(nameOf(command), refused.reason()));
            case Reaction.Accepted accepted -> publish(accepted.narration());
        }
    }

    private void tick() {
        run(() -> publish(game.tick()));
    }

    /**
     * Sends out what happened, and then what is true.
     *
     * <p>The state goes last and goes to everybody, built for each of them. A client that
     * ignores every narration event and reads only the state is still correct, which is what
     * makes reconnecting the same operation as joining.
     */
    private void publish(List<Event> narration) {
        boolean phaseChanged = game.phase() != lastAnnounced;
        announcePhaseChange();
        narration.forEach(this::announce);
        keepIfWorthKeeping(narration, phaseChanged);
        if (game.phase() == GamePhase.FINISHED && !ended) {
            ended = true;
            announce(new GameEvent.GameEnded());
        }
        // Last, and to everybody. "Every batch ends with the state" is the rule the whole
        // protocol rests on, and GameEnded is a fact about what happened like any other — it
        // used to go after the state, which meant the one batch that mattered most was the one
        // batch that broke the rule.
        sessions.values().forEach(session ->
                session.send(new GameEvent.StateChanged(game.viewFor(session.colour()))));
    }

    private void announcePhaseChange() {
        if (game.phase() == lastAnnounced) {
            return;
        }
        lastAnnounced = game.phase();
        // Redundant with the state that follows, and worth sending anyway: a phase change is
        // the one moment a view has to restructure rather than redraw.
        announce(new GameEvent.PhaseBegan(game.phase()));
    }

    /**
     * Sends one event to everybody still attached.
     *
     * <p>Public because a lobby ending a game under the baseline policy has to say so before it
     * hangs up: a client that is simply cut off cannot tell a finished game from a failed
     * network.
     *
     * @param event what to send
     */
    public void announceToEveryone(Event event) {
        run(() -> announce(event));
    }

    private void announce(Event event) {
        sessions.values().forEach(session -> session.send(event));
    }

    // ------------------------------------------------------------------ running the queue

    /**
     * Writes a snapshot at the points a game can be picked up from.
     *
     * <p>After a card is resolved and at every phase change — the moments when nothing is
     * half-done. Saving after every command would be a great deal of writing to save at most
     * one command's worth of progress; saving less often would mean losing a whole card.
     *
     * <p>On the game's own thread, like everything else here, so a snapshot is never taken
     * while the model is mid-change.
     */
    private void keepIfWorthKeeping(List<Event> narration, boolean phaseChanged) {
        // The phase change is asked about separately rather than looked for in the narration,
        // because it is announced by announcePhaseChange and never appears in that list. Reading
        // it from the list looked right and kept nothing at all.
        boolean quiescent = phaseChanged || narration.stream()
                .anyMatch(event -> event instanceof FlightEvent.CardResolved);
        if (quiescent) {
            keep();
        }
    }

    /**
     * Writes the game down, and carries on if that fails.
     *
     * <p><b>What an unguarded failure actually broke.</b> The keeper is called from
     * {@link #publish}, part-way through — after the narration has gone out and before the
     * {@code StateChanged} that closes the batch. An exception there abandoned the rest of
     * {@code publish}, so the players were sent a phase change and a story about what had
     * happened and then never told what was true. "Every batch ends with the state" is the
     * rule the whole protocol rests on, and a failing disk broke it precisely on a phase
     * change, which is the one batch a view has to restructure for.
     *
     * <p>The thread was never the problem, despite what this looks like: a single-thread
     * executor quietly replaces a worker that dies, so the game went on being played. What it
     * did instead was discard and rebuild its thread at every save point and print a stack
     * trace each time.
     *
     * <p>So a failed write now costs the ability to recover this game after a restart, and
     * nothing else. That is much the smaller loss: the table playing right now is worth more
     * than the ability to resume it later, and a full disk does not make a flight unplayable.
     *
     * <p>Reported to {@code System.err} because that is where this project puts things nobody
     * has anywhere better for yet; it belongs in the log #136 will add.
     */
    private void keep() {
        try {
            keeper.accept(game);
        } catch (RuntimeException failed) {
            System.err.println("could not keep " + game.id() + ": " + failed.getMessage());
        }
    }

    private void run(Runnable work) {
        try {
            queue.execute(work);
        } catch (RejectedExecutionException closing) {
            // The game is shutting down. Whatever this was, it is not going to matter.
        }
    }

    /**
     * Waits until everything queued so far has been dealt with.
     *
     * <p>For orderly shutdown, and for tests, which would otherwise have to guess how long a
     * command takes. It works by queueing behind the work it is waiting for, which is only
     * meaningful because there is exactly one thread doing the work.
     *
     * @param patience how long to wait
     * @return {@code true} if the queue caught up
     */
    public boolean awaitQuiet(Duration patience) {
        CountDownLatch caughtUp = new CountDownLatch(1);
        try {
            queue.execute(caughtUp::countDown);
            return caughtUp.await(patience.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException closing) {
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void close() {
        clock.shutdownNow();
        // Waited for, not just asked to stop. Shutdown lets already-queued work run and
        // returns at once, so a caller taking close() to mean "this game has stopped" was
        // wrong: a snapshot queued a moment earlier would still be written, and could land
        // after whoever closed the game had already taken away the directory it writes into.
        queue.shutdown();
        try {
            if (!queue.awaitTermination(SHUTDOWN_PATIENCE.toMillis(), TimeUnit.MILLISECONDS)) {
                queue.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            queue.shutdownNow();
        }
        sessions.values().forEach(session -> {
            Channel<Event, Command> connection = session.attach(null);
            if (connection != null) {
                connection.close();
            }
        });
    }

    private PlayerSession sessionOf(PlayerColor player) {
        PlayerSession session = sessions.get(player);
        if (session == null) {
            throw new IllegalArgumentException("there is no " + player + " player in this game");
        }
        return session;
    }

    private static String nameOf(Command command) {
        return command.getClass().getSimpleName();
    }

    /**
     * What one connection's arrivals mean.
     *
     * <p>Holds the player it belongs to, which is the whole of how the server knows who sent a
     * command: the message never says. A client that claims to be somebody else has to lie to
     * a socket it is not connected to.
     */
    private final class SeatListener implements ChannelListener<Command> {

        private final PlayerColor player;
        private final Channel<Event, Command> channel;

        SeatListener(PlayerColor player, Channel<Event, Command> channel) {
            this.player = player;
            this.channel = channel;
        }

        @Override
        public void received(Command command) {
            submit(player, command);
        }

        @Override
        public void closed(String reason) {
            // The reason is not passed on. From the other players' side a clean goodbye and a
            // laptop lid are the same event, which is what ConnectionChanged says and why it
            // carries no explanation.
            dropped(player, channel);
        }
    }

    /**
     * Returns the game being run, for a lobby that needs to know what it is showing.
     *
     * <p>Read-only in practice: anything that changes the game has to go through
     * {@link #submit}, or it is changing it on the wrong thread.
     *
     * @return the game
     */
    public Game game() {
        return game;
    }

    /**
     * Returns who is playing.
     *
     * @return the seats, in seating order
     */
    public List<Seat> seats() {
        return game.seats();
    }
}
