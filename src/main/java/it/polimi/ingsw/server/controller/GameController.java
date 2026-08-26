package it.polimi.ingsw.server.controller;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
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

    private final Game game;
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
        PlayerSession session = sessionOf(player);
        Channel<Event, Command> replaced = session.attach(channel);
        if (replaced != null && replaced != channel) {
            replaced.close();
        }
        run(() -> {
            game.connectionChanged(player, true);
            announce(new GameEvent.ConnectionChanged(player, true));
            // The whole picture, which is all a returning player needs and exactly what a new
            // one needs too. There is no third case to write.
            session.send(new GameEvent.StateChanged(game.viewFor(player)));
        });
        return new SeatListener(player, channel);
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
        Reaction reaction = game.apply(player, command);
        if (reaction instanceof Reaction.Refused refused) {
            // Only to the sender, and no state after it: nothing changed, so there is no new
            // truth to send and nobody else has anything to learn.
            sessionOf(player).send(new GameEvent.Rejected(nameOf(command), refused.reason()));
            return;
        }
        publish(((Reaction.Accepted) reaction).narration());
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
        announcePhaseChange();
        narration.forEach(this::announce);
        sessions.values().forEach(session ->
                session.send(new GameEvent.StateChanged(game.viewFor(session.colour()))));
        if (game.phase() == GamePhase.FINISHED && !ended) {
            ended = true;
            announce(new GameEvent.GameEnded());
        }
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

    private void announce(Event event) {
        sessions.values().forEach(session -> session.send(event));
    }

    // ------------------------------------------------------------------ running the queue

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
        queue.shutdown();
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
