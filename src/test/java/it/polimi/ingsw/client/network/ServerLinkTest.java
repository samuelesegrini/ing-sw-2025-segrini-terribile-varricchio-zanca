package it.polimi.ingsw.client.network;

import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import it.polimi.ingsw.common.transport.TransportException;
import it.polimi.ingsw.server.network.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a client can reach a server whichever way it chooses, and that the choice is the
 * last thing anybody knows about it.
 *
 * <p>The tests are written once and run against both transports, which is the same trick the
 * conformance suite plays on the server side and for the same reason. A test that only ran
 * over a socket would be worth very little for a requirement whose whole content is that both
 * work.
 */
class ServerLinkTest {

    private static final Duration PATIENCE = Duration.ofSeconds(5);

    private Server server;
    private ServerLink link;

    @AfterEach
    void shutDown() {
        if (link != null) {
            link.close();
        }
        if (server != null) {
            server.close();
        }
    }

    private int portFor(Transport transport) {
        return transport == Transport.SOCKET ? server.socketPort() : server.rmiPort();
    }

    @ParameterizedTest(name = "over {0}")
    @EnumSource(Transport.class)
    @DisplayName("a client connects, logs in, and is told its name is its own")
    void connectingAndLoggingIn(Transport transport) {
        server = Server.start(0, 0);
        ClientState state = new ClientState();

        link = ServerLink.connect(transport, "localhost", portFor(transport), state);
        link.send(new LobbyCommand.Login("samuele"));

        waitUntil(() -> state.nickname().isPresent());
        assertEquals("samuele", state.nickname().orElseThrow());
        assertTrue(link.isOpen());
    }

    @ParameterizedTest(name = "over {0}")
    @EnumSource(Transport.class)
    @DisplayName("a refusal reaches the client with the reason attached")
    void refusalsArrive(Transport transport) {
        server = Server.start(0, 0);
        ClientState first = new ClientState();
        ServerLink taken = ServerLink.connect(transport, "localhost", portFor(transport), first);
        taken.send(new LobbyCommand.Login("samuele"));
        waitUntil(() -> first.nickname().isPresent());

        ClientState second = new ClientState();
        link = ServerLink.connect(transport, "localhost", portFor(transport), second);
        link.send(new LobbyCommand.Login("samuele"));

        waitUntil(() -> second.lastRefusal().isPresent());
        assertEquals("somebody is already called samuele", second.lastRefusal().orElseThrow());
        taken.close();
    }

    @ParameterizedTest(name = "over {0}")
    @EnumSource(Transport.class)
    @DisplayName("a client that opens a table is sent the picture unasked")
    void theStateArrives(Transport transport) {
        server = Server.start(0, 0);
        ClientState state = new ClientState();
        link = ServerLink.connect(transport, "localhost", portFor(transport), state);

        link.send(new LobbyCommand.Login("samuele"));
        waitUntil(() -> state.nickname().isPresent());
        link.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
        waitUntil(() -> state.gameId().isPresent());

        assertEquals("game-1", state.gameId().orElseThrow());
        assertTrue(state.game().isEmpty(),
                "a table that is still filling up is not a game, and there is no board yet");
    }

    @ParameterizedTest(name = "over {0}")
    @EnumSource(Transport.class)
    @DisplayName("a server that goes away is noticed, and says so rather than throwing")
    void losingTheServer(Transport transport) {
        server = Server.start(0, 0);
        ClientState state = new ClientState();
        link = ServerLink.connect(transport, "localhost", portFor(transport), state);
        link.send(new LobbyCommand.Login("samuele"));
        waitUntil(() -> state.nickname().isPresent());

        server.close();
        server = null;

        waitUntil(() -> !state.isConnected());
        assertFalse(state.isConnected());
        // Typing into a connection that has gone is something a person will do, and it must not
        // be the thing that ends their client.
        link.send(new LobbyCommand.ListGames());
    }

    @ParameterizedTest(name = "over {0}")
    @EnumSource(Transport.class)
    @DisplayName("connecting to nothing fails where the client can do something about it")
    void nobodyThere(Transport transport) {
        assertThrows(TransportException.class,
                () -> ServerLink.connect(transport, "localhost", 1, new ClientState()));
    }

    @Test
    @DisplayName("a transport is named the way a person would type it")
    void namingATransport() {
        assertEquals(Transport.SOCKET, Transport.of("socket"));
        assertEquals(Transport.SOCKET, Transport.of("SOCKET"));
        assertEquals(Transport.RMI, Transport.of("rmi"));
        assertEquals("there is no carrier pigeon transport; try socket or rmi",
                assertThrows(IllegalArgumentException.class,
                        () -> Transport.of("carrier pigeon")).getMessage());
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) {
        long deadline = System.nanoTime() + PATIENCE.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                // A sleep rather than a spin: a build machine has two cores and other tests
                // are using them.
                Thread.sleep(1);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("that never happened within " + PATIENCE);
    }
}
