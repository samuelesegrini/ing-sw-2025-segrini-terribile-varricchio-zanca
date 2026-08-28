package it.polimi.ingsw.server.lobby;

import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import it.polimi.ingsw.common.protocol.LobbyEvent;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.server.controller.GameController;
import it.polimi.ingsw.server.model.game.Seat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The front desk: names, tables, and getting people to the right one.
 *
 * <p>Everything here happens on one thread, for the same reason a game does. Nicknames are
 * unique, seats are finite, and a table fills at the moment somebody takes the last one — all
 * three are the kind of thing two connections arriving together get wrong, and a queue settles
 * them by arrival order rather than by whoever won a race.
 *
 * <p>It stops being involved the moment a game starts. From then on a player's commands go
 * straight to that game's own queue; the lobby keeps only enough to answer the one question it
 * is still asked, which is where somebody belongs when they come back.
 */
public final class Lobby implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Lobby.class);

    private final GameArchive archive;
    private final DisconnectionPolicy onDisconnection;
    /** How long to let the worker finish what it was doing before shutting the desk. */
    private static final Duration SHUTDOWN_PATIENCE = Duration.ofSeconds(2);

    private final ExecutorService queue;
    private final AtomicInteger nextGame = new AtomicInteger(1);

    private final Roster roster = new Roster();

    /**
     * Opens a lobby.
     *
     * @param settings the catalogue, the shuffle, the clock, what a dropped connection does,
     *                 how long a game waits for its last player, and where games are kept
     */
    public Lobby(ServerSettings settings) {
        this.archive = new GameArchive(settings);
        this.onDisconnection = settings.onDisconnection();
        this.queue = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "lobby");
            thread.setDaemon(true);
            return thread;
        });
        // Before anybody can connect. A player logging in while games were still being put back
        // could be told their name was free and then find it was not.
        recoverWhatWasKept();
    }

    /**
     * Takes charge of a new connection.
     *
     * <p>Shaped to fit what both transports ask for, so that a server accepting a player reads
     * the same whichever they connected over:
     *
     * <pre>{@code
     * SocketServer.listening(port, lobby::welcome, Liveness.DEFAULT);
     * }</pre>
     *
     * @param channel the connection
     * @return what to do with what arrives on it
     */
    public ChannelListener<Command> welcome(Channel<Event, Command> channel) {
        return new Connection(this, channel);
    }

    // ------------------------------------------------------------------ the queue

    void submit(Connection from, Command command) {
        run(() -> from.apply(command, this));
    }

    void disconnected(Connection from) {
        run(() -> from.gone(this));
    }

    /**
     * Asks the worker a question and waits for the answer.
     *
     * <p>Both of the questions below are asked from whatever thread wants to know, and read
     * books the worker is writing to. Reading them here would be a plain map being walked on
     * one thread while another puts into it — which is the one thing the single-worker
     * arrangement exists to make impossible, and it does not stop being true because the
     * caller is a test.
     *
     * @param question what to ask, run on the worker
     * @param <T>      what comes back
     * @return the answer, or an empty list if the desk has already shut
     */
    private <T> List<T> askTheDesk(java.util.function.Supplier<List<T>> question) {
        List<T> answer = new ArrayList<>();
        CountDownLatch asked = new CountDownLatch(1);
        try {
            queue.execute(() -> {
                answer.addAll(question.get());
                asked.countDown();
            });
            if (!asked.await(SHUTDOWN_PATIENCE.toMillis(), TimeUnit.MILLISECONDS)) {
                return List.of();
            }
        } catch (RejectedExecutionException closing) {
            return List.of();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return List.of();
        }
        return List.copyOf(answer);
    }

    private void run(Runnable work) {
        try {
            queue.execute(work);
        } catch (RejectedExecutionException closing) {
            // Shutting down. Whatever this was, it is not going to matter.
        }
    }

    /**
     * Waits until everything queued so far has been dealt with, here and in every game.
     *
     * <p>For orderly shutdown, and for tests. The games are included because most of what the
     * lobby does ends in a game's queue rather than its own — starting a table hands four
     * connections over, and each hand-over queues a state to send. Waiting only for the lobby
     * would report a quiet desk while four clients had not been told the game had begun.
     *
     * <p>It works by queueing behind the work it is waiting for, which is only meaningful
     * because one thread does the work in each place.
     *
     * @param patience how long to wait for each queue
     * @return {@code true} if everything caught up
     */
    public boolean awaitQuiet(Duration patience) {
        List<GameController> running = new ArrayList<>();
        CountDownLatch deskIsClear = new CountDownLatch(1);
        try {
            queue.execute(() -> {
                running.addAll(roster.running());
                deskIsClear.countDown();
            });
            if (!deskIsClear.await(patience.toMillis(), TimeUnit.MILLISECONDS)) {
                return false;
            }
        } catch (RejectedExecutionException closing) {
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
        return running.stream().allMatch(controller -> controller.awaitQuiet(patience));
    }

    // ------------------------------------------------------------------ commands

    /**
     * Deals with a command from a connection that has already said who it is.
     *
     * <p>The two guards that used to open this method have moved onto
     * {@link ConnectionState}: whether a command belongs at the desk at all, and whether a
     * connection may send it yet, are both facts about where the connection has got to, and
     * asking a null field about that was the last state machine in this project not written
     * as one.
     *
     * @param from    who sent it
     * @param command what they want
     */
    void fromSomebodyNamed(Connection from, LobbyCommand command) {
        switch (command) {
            // Reached only from a connection that is already named — an anonymous one is
            // logged in by its own state — so this is somebody logging in twice.
            case LobbyCommand.Login ignored ->
                    refuse(from, command, "you are already " + from.nickname());
            case LobbyCommand.ListGames ignored ->
                    from.send(new LobbyEvent.GamesListed(roster.withRoom().stream()
                            .map(PendingGame::summary)
                            .toList()));
            case LobbyCommand.CreateGame create -> create(from, create);
            case LobbyCommand.JoinGame join -> join(from, join);
            case LobbyCommand.LeaveGame ignored -> leave(from);
        }
    }

    /**
     * Gives a connection a nickname, or puts it back where that nickname was sitting.
     *
     * @param from  the connection
     * @param login the name it is claiming
     */
    void login(Connection from, LobbyCommand.Login login) {
        String name = login.nickname();
        Optional<Roster.Seated> seat = roster.seatOf(name);
        if (seat.isPresent()) {
            comeBack(from, name, seat.get());
            return;
        }
        if (roster.isTaken(name)) {
            // Client-side choice, server-side enforcement (requirement L1). The message names
            // the problem so a client can ask for another name rather than guessing.
            refuse(from, login, "somebody is already called " + name);
            return;
        }
        from.nameYourself(name);
        roster.name(name, from);
        from.send(new LobbyEvent.LoggedIn(name));
        LOG.debug("{} logged in", name);
    }

    /**
     * Puts somebody back in the game they dropped out of.
     *
     * <p>There is no reconnect command, and this is why: logging in with a name that belongs to
     * a seat nobody is attached to <em>is</em> reconnecting. A separate code path would be a
     * second way of doing the same thing, and the two would eventually disagree.
     */
    private void comeBack(Connection from, String name, Roster.Seated seat) {
        if (seat.controller().isAttached(seat.colour())) {
            refuse(from, new LobbyCommand.Login(name), "somebody is already called " + name);
            return;
        }
        from.nameYourself(name);
        roster.name(name, from);
        from.send(new LobbyEvent.LoggedIn(name));
        from.send(new LobbyEvent.JoinedGame(seat.controller().game().id(), seat.colour()));
        // bind sends the whole picture, which is all a returning player needs and exactly what
        // a new one gets. There is no third case to write.
        LOG.debug("{} rejoined {} as {}", name, seat.controller().game().id(), seat.colour());
        from.handOverTo(seat.colour(), seat.controller().bind(seat.colour(), from.channel()));
    }

    private void create(Connection from, LobbyCommand.CreateGame create) {
        if (alreadySeated(from)) {
            return;
        }
        PendingGame table = new PendingGame(
                "game-" + nextGame.getAndIncrement(), create.level(), create.seats());
        roster.open(table);
        LOG.debug("{} opened {} for {} at {}", from.nickname(), table.id(), create.seats(),
                create.level());
        take(from, table);
    }

    private void join(Connection from, LobbyCommand.JoinGame join) {
        if (alreadySeated(from)) {
            return;
        }
        Optional<PendingGame> table = roster.table(join.gameId());
        if (table.isEmpty()) {
            refuse(from, join, "there is no game called " + join.gameId() + " waiting for players");
            return;
        }
        if (!table.get().hasRoom()) {
            refuse(from, join, "that game is full");
            return;
        }
        take(from, table.get());
    }

    private boolean alreadySeated(Connection from) {
        Optional<PendingGame> already = roster.tableOf(from);
        if (already.isEmpty()) {
            return false;
        }
        from.send(new GameEvent.Rejected("JoinGame",
                "you are already waiting at " + already.get().id()));
        return true;
    }

    private void take(Connection from, PendingGame table) {
        PlayerColor colour = roster.join(table, from);
        from.send(new LobbyEvent.JoinedGame(table.id(), colour));
        table.players().stream()
                .filter(other -> other != from)
                .forEach(other -> other.send(new LobbyEvent.PlayerEntered(from.nickname(), colour)));
        LOG.debug("{} took a seat at {} as {}", from.nickname(), table.id(), colour);
        if (table.isFull()) {
            start(table);
        }
    }

    private void leave(Connection from) {
        roster.tableOf(from).ifPresent(table -> stepAwayFrom(table, from));
    }

    private void stepAwayFrom(PendingGame table, Connection from) {
        if (onDisconnection == DisconnectionPolicy.ENDS_THE_GAME) {
            // Nobody is taken off first, so that the player who left is told along with the
            // rest. Reaching here at all means the roster had them at this table.
            endBeforeItStarted(table, from);
            return;
        }
        if (!roster.leave(table, from)) {
            return;
        }
        LOG.debug("{} left {}", from.nickname(), table.id());
        table.players().forEach(other -> other.send(new LobbyEvent.PlayerLeft(from.nickname())));
    }

    /**
     * Ends a table that never became a game.
     *
     * <p>The baseline rule this policy models says a game ends when somebody leaves it or their
     * connection drops, <em>"anche se in fase di avvio"</em> — even while it is still filling —
     * and that every player is told. Only the started case was honoured before, which left the
     * others sitting at a table waiting for a seat that was never going to be taken, told
     * nothing except that somebody had gone.
     *
     * <p>They keep their connections and their nicknames. The requirement asks that the game
     * end and that they hear about it, not that they be hung up on; the started-game path
     * closes channels because a controller is holding them, and there is none here.
     *
     * @param table who is at it, the departing player included
     * @param who   the player who left or dropped
     */
    private void endBeforeItStarted(PendingGame table, Connection who) {
        LOG.debug("{} ended before it started: {} left", table.id(), who.nickname());
        // Everybody at it, which still includes whoever just left — sending to a connection
        // that has already gone does nothing, so the dropped case costs an event nobody
        // receives rather than a special case nobody tests.
        table.players().forEach(player -> player.send(new GameEvent.GameEnded()));
        roster.close(table);
    }

    /**
     * Turns a full table into a game.
     *
     * <p>The tiles are shuffled and the cards dealt here, at the last possible moment. Doing it
     * when the table was created would mean shuffling a hundred and fifty tiles for every
     * player who joined and changed their mind.
     */
    private void start(PendingGame table) {
        List<Connection> players = table.players();
        List<Seat> seats = new ArrayList<>();
        for (Connection player : players) {
            seats.add(new Seat(player.nickname(), table.colourOf(player)));
        }

        GameController controller = archive.deal(table.id(), table.level(), seats);
        roster.close(table);
        roster.started(controller);

        // Everybody is attached before anybody is announced. Announcing as each is bound would
        // mean the second player never hears about the first, and four players would end up
        // with four different accounts of the same moment.
        for (Connection player : players) {
            PlayerColor colour = table.colourOf(player);
            player.handOverTo(colour, controller.attach(colour, player.channel()));
        }
        LOG.debug("{} started, {} at {}", table.id(), seats.size(), table.level());
        players.forEach(player -> controller.announceArrival(table.colourOf(player)));
    }

    /**
     * Picks up every game that was running when the server stopped.
     *
     * <p>They come back with nobody attached: the seats are held under the old nicknames, and
     * logging in with one puts that player back at their table. This is the same path a player
     * takes after their own connection drops, which is why there is not a second one.
     */
    private void recoverWhatWasKept() {
        // Which games came back, and whether one of them could not, is the archive's business.
        // The desk's business is that the seats are held under the old nicknames, so that
        // logging in with one finds them.
        archive.recoverAll().forEach(roster::started);
    }

    // ------------------------------------------------------------------ leaving

    /**
     * Forgets a named connection that was not in a game.
     *
     * <p>It gives up its nickname and its place at whatever table it was waiting at. A
     * connection that never said who it was is not here at all: its own state forgets nothing,
     * because it was never written down.
     *
     * @param name what it was called
     * @param gone the connection
     */
    void leftTheDesk(String name, Connection gone) {
        roster.release(name, gone);
        roster.tableOf(gone).ifPresent(table -> stepAwayFrom(table, gone));
    }

    /**
     * Forgets a connection that was in a game, without forgetting its seat.
     *
     * @param name what it was called
     * @param gone the connection
     */
    void leftAGame(String name, Connection gone) {
        roster.release(name, gone);
        Optional<Roster.Seated> seat = roster.seatOf(name);
        if (seat.isEmpty()) {
            return;
        }
        if (onDisconnection == DisconnectionPolicy.ENDS_THE_GAME) {
            abandon(seat.get().controller());
            return;
        }
        // The seat stays, unattached, so that logging in again finds it. The game itself
        // carries on without them: the controller has already been told, and a flight that
        // stopped every time somebody's laptop did would be a worse game.
        //
        // Unless they were the last one. A game with nobody connected to it has nobody to
        // carry on for, and holding it open costs a thread, a game, and every nickname at
        // that table — for as long as the server runs.
        if (!roster.anybodyLeftAt(seat.get().controller())) {
            abandon(seat.get().controller());
        }
    }

    /**
     * Ends a game because somebody left it, under the baseline policy.
     *
     * <p>Everyone still connected is told the game is over before their connection is closed,
     * because a client that is simply hung up on cannot tell the difference between a game that
     * ended and a network that failed.
     */
    private void abandon(GameController controller) {
        String id = controller.game().id();
        roster.reclaim(controller);
        // A finished game is not worth keeping, and one left behind would come back from the
        // dead the next time the server started.
        archive.forget(id);
        LOG.debug("{} reclaimed, nobody left at it", id);
        controller.announceToEveryone(new GameEvent.GameEnded());
        controller.awaitQuiet(Duration.ofSeconds(1));
        controller.close();
    }

    /**
     * Tells one client that a command of theirs did nothing.
     *
     * @param from    who sent it
     * @param command what they sent, named back to them
     * @param reason  why, in a sentence
     */
    static void refuse(Connection from, Command command, String reason) {
        from.send(new GameEvent.Rejected(command.getClass().getSimpleName(), reason));
    }

    // ------------------------------------------------------------------ looking in

    /**
     * Returns the games that are still filling up.
     *
     * @return their identifiers, oldest first
     */
    public List<String> tablesWaiting() {
        return askTheDesk(roster::tablesWaiting);
    }

    /**
     * Returns the games that have started.
     *
     * @return their identifiers, oldest first
     */
    public List<String> gamesRunning() {
        return askTheDesk(roster::gamesRunning);
    }

    @Override
    public void close() {
        // Wait for the worker before touching anything it owns. Shutdown lets already-queued
        // work run, and one of those may be a disconnection that reclaims a game — which would
        // be a second thread removing from `games` while this one walks it.
        queue.shutdown();
        try {
            if (!queue.awaitTermination(SHUTDOWN_PATIENCE.toMillis(),
                    java.util.concurrent.TimeUnit.MILLISECONDS)) {
                queue.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            queue.shutdownNow();
        }
        // A copy even so: closing a controller can call back in, and a collection being walked
        // is a poor place to be modified from.
        roster.running().forEach(GameController::close);
        roster.forgetEverything();
    }
}
