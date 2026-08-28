package it.polimi.ingsw.server.network;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import it.polimi.ingsw.common.protocol.LobbyEvent;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.common.transport.Liveness;
import it.polimi.ingsw.common.transport.socket.SocketConnector;
import it.polimi.ingsw.server.lobby.ServerSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Requirement AF3, through the thing that ships.
 *
 * <p>{@code PersistenceTest} already proves a {@code Lobby} keeps its games and picks them up
 * again. That was never the doubt. The doubt — and the defect, #157 — was that every
 * {@code Server.start} overload funnelled into the one {@code Lobby} constructor that keeps
 * nothing, so the running jar had never written a snapshot in its life while a green suite
 * said persistence worked.
 *
 * <p>So this one starts a real {@link Server}, connects over a real socket, and restarts it.
 * A test that reached past {@code Server} to build its own {@code Lobby} would prove exactly
 * what the old ones proved, which is not enough.
 *
 * <p>The acceptance criterion in the requirements is "kill the server mid-flight, restart,
 * resume with identical state". This closes the server rather than killing the process: an
 * orderly close still leaves the last snapshot on disk and nothing in memory, which is what
 * recovery has to work from either way. What a {@code kill -9} adds is an interrupted write,
 * and {@code SnapshotStore} tests that directly by corrupting a partial file.
 *
 * <p>Components involved: {@link Server}, {@link LobbyEvent}, {@link ServerSettings}.
 */
class ServerPersistenceTest {

    private static final long PATIENCE_MS = 10_000;

    private final List<Caller> callers = new ArrayList<>();
    private final List<Server> servers = new ArrayList<>();

    @AfterEach
    void shutDown() {
        callers.forEach(Caller::hangUp);
        callers.clear();
        servers.forEach(Server::close);
        servers.clear();
    }

    /** Somebody dialling in over a real socket, who remembers what they were told. */
    private final class Caller implements ChannelListener<Event> {

        private final List<Event> heard = new CopyOnWriteArrayList<>();
        private volatile Channel<Command, Event> channel;

        Caller(int port) {
            callers.add(this);
            this.channel = SocketConnector.connect("localhost", port, this, Liveness.DEFAULT);
        }

        @Override
        public void received(Event event) {
            heard.add(event);
        }

        @Override
        public void closed(String reason) {
            // Not what this suite is about.
        }

        Caller send(Command command) {
            channel.send(command);
            return this;
        }

        void hangUp() {
            channel.close();
        }

        <E extends Event> List<E> only(Class<E> kind) {
            return List.copyOf(heard).stream().filter(kind::isInstance).map(kind::cast).toList();
        }

        <E extends Event> E await(Class<E> kind) {
            assertTrue(waitFor(() -> !only(kind).isEmpty()),
                    "never heard a " + kind.getSimpleName());
            List<E> seen = only(kind);
            return seen.get(seen.size() - 1);
        }
    }

    private Server start(ServerSettings settings) {
        Server server = Server.start(0, 0, settings);
        servers.add(server);
        return server;
    }

    private static boolean waitFor(BooleanSupplier condition) {
        long deadline = System.nanoTime() + PATIENCE_MS * 1_000_000L;
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return condition.getAsBoolean();
    }

    private static long snapshotsIn(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return 0;
        }
        try (var files = Files.list(directory)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".snapshot"))
                    .count();
        }
    }

    @Test
    @DisplayName("a server told to keep its games writes them down")
    void aKeepingServerWritesSnapshots(@TempDir Path directory) throws IOException {
        Server server = start(ServerSettings.defaults().keeping(directory));

        seatATableOfTwo(server.socketPort());

        assertTrue(waitFor(() -> {
            try {
                return snapshotsIn(directory) > 0;
            } catch (IOException unreadable) {
                return false;
            }
        }), "a game was dealt and nothing was written down");
    }

    @Test
    @DisplayName("and the default server writes nothing, so a test never litters")
    void theDefaultServerWritesNothing(@TempDir Path directory) throws IOException {
        Server server = start(ServerSettings.defaults());

        seatATableOfTwo(server.socketPort());

        assertEquals(0, snapshotsIn(directory));
    }

    @Test
    @DisplayName("a game outlives the server, and the seat is there under the old nickname")
    void aGameSurvivesTheServerStopping(@TempDir Path directory) {
        ServerSettings keeping = ServerSettings.defaults().keeping(directory);
        Server before = start(keeping);
        String gameId = seatATableOfTwo(before.socketPort());
        before.close();

        // A different server, on a different port, reading the same directory — which is what
        // starting the jar again amounts to.
        Server after = start(keeping);
        Caller returning = new Caller(after.socketPort());
        returning.send(new LobbyCommand.Login("samuele"));

        LobbyEvent.JoinedGame back = returning.await(LobbyEvent.JoinedGame.class);
        assertEquals(gameId, back.gameId(), "the seat should be the one they left");
        assertFalse(returning.only(GameEvent.StateChanged.class).isEmpty(),
                "and coming back should be answered with the whole picture");
    }

    @Test
    @DisplayName("a server that keeps nothing has nothing to give back")
    void aForgetfulServerLosesTheGame(@TempDir Path directory) {
        Server before = start(ServerSettings.defaults());
        seatATableOfTwo(before.socketPort());
        before.close();

        Server after = start(ServerSettings.defaults().keeping(directory));
        Caller returning = new Caller(after.socketPort());
        returning.send(new LobbyCommand.Login("samuele"));
        returning.await(LobbyEvent.LoggedIn.class);

        // This is what `java -jar server.jar` did before #157: the login is accepted, and the
        // player is a stranger at an empty desk.
        assertTrue(returning.only(LobbyEvent.JoinedGame.class).isEmpty(),
                "there was no game to come back to");
    }

    /**
     * Opens a two-seat table and fills it, which deals the game and is a point worth keeping.
     *
     * @param port where to dial
     * @return what the game is called
     */
    private String seatATableOfTwo(int port) {
        Caller host = new Caller(port);
        host.send(new LobbyCommand.Login("samuele"));
        host.await(LobbyEvent.LoggedIn.class);
        host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
        String gameId = host.await(LobbyEvent.JoinedGame.class).gameId();

        Caller guest = new Caller(port);
        guest.send(new LobbyCommand.Login("chiara"));
        guest.await(LobbyEvent.LoggedIn.class);
        guest.send(new LobbyCommand.JoinGame(gameId));
        guest.await(GameEvent.StateChanged.class);
        host.await(GameEvent.StateChanged.class);
        return gameId;
    }
}
