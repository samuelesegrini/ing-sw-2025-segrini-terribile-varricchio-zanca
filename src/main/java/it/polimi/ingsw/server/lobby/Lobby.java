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
import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.model.game.Game;
import it.polimi.ingsw.server.model.game.Seat;

import java.time.Duration;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.HashMap;
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
import java.util.random.RandomGenerator;

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

    private final GameData data;
    private final RandomGenerator random;
    private final InstantSource clock;
    private final DisconnectionPolicy onDisconnection;
    private final ExecutorService queue;
    private final AtomicInteger nextGame = new AtomicInteger(1);

    private final Map<String, Connection> loggedIn = new HashMap<>();
    private final Map<String, PendingGame> waiting = new LinkedHashMap<>();
    private final Map<String, Seated> playing = new HashMap<>();
    private final Map<String, GameController> games = new LinkedHashMap<>();

    /** Where a nickname belongs once its game has started. */
    private record Seated(GameController controller, PlayerColor colour) {
    }

    /**
     * Opens a lobby whose games survive a dropped connection.
     *
     * @param data   the tiles, cards and boards games will be made of
     * @param random where the shuffling and the dice come from
     * @param clock  where hourglasses read the time
     */
    public Lobby(GameData data, RandomGenerator random, InstantSource clock) {
        this(data, random, clock, DisconnectionPolicy.GAME_CARRIES_ON);
    }

    /**
     * Opens a lobby.
     *
     * @param data            the tiles, cards and boards games will be made of
     * @param random          where the shuffling and the dice come from
     * @param clock           where hourglasses read the time
     * @param onDisconnection what a dropped connection does to a game in progress
     */
    public Lobby(GameData data, RandomGenerator random, InstantSource clock,
                 DisconnectionPolicy onDisconnection) {
        this.data = data;
        this.random = random;
        this.clock = clock;
        this.onDisconnection = onDisconnection;
        this.queue = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "lobby");
            thread.setDaemon(true);
            return thread;
        });
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
        run(() -> handle(from, command));
    }

    void disconnected(Connection from) {
        run(() -> forget(from));
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
                running.addAll(games.values());
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

    private void handle(Connection from, Command command) {
        if (!(command instanceof LobbyCommand lobby)) {
            refuse(from, command, "you are not in a game yet");
            return;
        }
        if (!from.isLoggedIn() && !(lobby instanceof LobbyCommand.Login)) {
            refuse(from, command, "say who you are first");
            return;
        }
        switch (lobby) {
            case LobbyCommand.Login login -> login(from, login);
            case LobbyCommand.ListGames ignored ->
                    from.send(new LobbyEvent.GamesListed(waiting.values().stream()
                            .filter(PendingGame::hasRoom)
                            .map(PendingGame::summary)
                            .toList()));
            case LobbyCommand.CreateGame create -> create(from, create);
            case LobbyCommand.JoinGame join -> join(from, join);
            case LobbyCommand.LeaveGame ignored -> leave(from);
        }
    }

    private void login(Connection from, LobbyCommand.Login login) {
        if (from.isLoggedIn()) {
            refuse(from, login, "you are already " + from.nickname());
            return;
        }
        String name = login.nickname();
        Seated seat = playing.get(name);
        if (seat != null) {
            comeBack(from, name, seat);
            return;
        }
        if (loggedIn.containsKey(name)) {
            // Client-side choice, server-side enforcement (requirement L1). The message names
            // the problem so a client can ask for another name rather than guessing.
            refuse(from, login, "somebody is already called " + name);
            return;
        }
        from.nameYourself(name);
        loggedIn.put(name, from);
        from.send(new LobbyEvent.LoggedIn(name));
    }

    /**
     * Puts somebody back in the game they dropped out of.
     *
     * <p>There is no reconnect command, and this is why: logging in with a name that belongs to
     * a seat nobody is attached to <em>is</em> reconnecting. A separate code path would be a
     * second way of doing the same thing, and the two would eventually disagree.
     */
    private void comeBack(Connection from, String name, Seated seat) {
        if (seat.controller().isAttached(seat.colour())) {
            refuse(from, new LobbyCommand.Login(name), "somebody is already called " + name);
            return;
        }
        from.nameYourself(name);
        loggedIn.put(name, from);
        from.send(new LobbyEvent.LoggedIn(name));
        from.send(new LobbyEvent.JoinedGame(seat.controller().game().id(), seat.colour()));
        // bind sends the whole picture, which is all a returning player needs and exactly what
        // a new one gets. There is no third case to write.
        from.handOverTo(seat.colour(), seat.controller().bind(seat.colour(), from.channel()));
    }

    private void create(Connection from, LobbyCommand.CreateGame create) {
        if (alreadySeated(from)) {
            return;
        }
        PendingGame table = new PendingGame(
                "game-" + nextGame.getAndIncrement(), create.level(), create.seats());
        waiting.put(table.id(), table);
        take(from, table);
    }

    private void join(Connection from, LobbyCommand.JoinGame join) {
        if (alreadySeated(from)) {
            return;
        }
        PendingGame table = waiting.get(join.gameId());
        if (table == null) {
            refuse(from, join, "there is no game called " + join.gameId() + " waiting for players");
            return;
        }
        if (!table.hasRoom()) {
            refuse(from, join, "that game is full");
            return;
        }
        take(from, table);
    }

    private boolean alreadySeated(Connection from) {
        Optional<PendingGame> already = tableOf(from);
        if (already.isEmpty()) {
            return false;
        }
        from.send(new GameEvent.Rejected("JoinGame",
                "you are already waiting at " + already.get().id()));
        return true;
    }

    private void take(Connection from, PendingGame table) {
        PlayerColor colour = table.seat(from);
        from.send(new LobbyEvent.JoinedGame(table.id(), colour));
        table.players().stream()
                .filter(other -> other != from)
                .forEach(other -> other.send(new LobbyEvent.PlayerEntered(from.nickname(), colour)));
        if (table.isFull()) {
            start(table);
        }
    }

    private void leave(Connection from) {
        tableOf(from).ifPresent(table -> stepAwayFrom(table, from));
    }

    private void stepAwayFrom(PendingGame table, Connection from) {
        if (!table.remove(from)) {
            return;
        }
        table.players().forEach(other -> other.send(new LobbyEvent.PlayerLeft(from.nickname())));
        if (table.isEmpty()) {
            waiting.remove(table.id());
        }
    }

    /**
     * Turns a full table into a game.
     *
     * <p>The tiles are shuffled and the cards dealt here, at the last possible moment. Doing it
     * when the table was created would mean shuffling a hundred and fifty tiles for every
     * player who joined and changed their mind.
     */
    private void start(PendingGame table) {
        waiting.remove(table.id());
        List<Connection> players = table.players();
        List<Seat> seats = new ArrayList<>();
        for (Connection player : players) {
            seats.add(new Seat(player.nickname(), table.colourOf(player)));
        }

        Game game = Game.create(table.id(), table.level(), seats, data, random, clock);
        GameController controller = new GameController(game);
        games.put(table.id(), controller);

        // Everybody is attached before anybody is announced. Announcing as each is bound would
        // mean the second player never hears about the first, and four players would end up
        // with four different accounts of the same moment.
        for (Connection player : players) {
            PlayerColor colour = table.colourOf(player);
            playing.put(player.nickname(), new Seated(controller, colour));
            player.handOverTo(colour, controller.attach(colour, player.channel()));
        }
        players.forEach(player -> controller.announceArrival(table.colourOf(player)));
    }

    // ------------------------------------------------------------------ leaving

    private void forget(Connection gone) {
        String name = gone.nickname();
        if (name == null) {
            return;
        }
        loggedIn.remove(name, gone);
        if (gone.isPlaying()) {
            if (onDisconnection == DisconnectionPolicy.ENDS_THE_GAME) {
                abandon(playing.get(name));
                return;
            }
            // The seat stays in `playing`, unattached, so that logging in again finds it. The
            // game itself carries on without them: the controller has already been told, and a
            // flight that stopped every time somebody's laptop did would be a worse game.
            return;
        }
        tableOf(gone).ifPresent(table -> stepAwayFrom(table, gone));
    }

    /**
     * Ends a game because somebody left it, under the baseline policy.
     *
     * <p>Everyone still connected is told the game is over before their connection is closed,
     * because a client that is simply hung up on cannot tell the difference between a game that
     * ended and a network that failed.
     */
    private void abandon(Seated seat) {
        if (seat == null) {
            return;
        }
        GameController controller = seat.controller();
        String id = controller.game().id();
        controller.seats().forEach(sitting -> playing.remove(sitting.nickname()));
        games.remove(id);
        controller.announceToEveryone(new GameEvent.GameEnded());
        controller.awaitQuiet(Duration.ofSeconds(1));
        controller.close();
    }

    private Optional<PendingGame> tableOf(Connection player) {
        return waiting.values().stream()
                .filter(table -> table.players().contains(player))
                .findFirst();
    }

    private static void refuse(Connection from, Command command, String reason) {
        from.send(new GameEvent.Rejected(command.getClass().getSimpleName(), reason));
    }

    // ------------------------------------------------------------------ looking in

    /**
     * Returns the games that are still filling up.
     *
     * @return their identifiers, oldest first
     */
    public List<String> tablesWaiting() {
        return List.copyOf(waiting.keySet());
    }

    /**
     * Returns the games that have started.
     *
     * @return their identifiers, oldest first
     */
    public List<String> gamesRunning() {
        return List.copyOf(games.keySet());
    }

    @Override
    public void close() {
        queue.shutdown();
        games.values().forEach(GameController::close);
        games.clear();
    }
}
