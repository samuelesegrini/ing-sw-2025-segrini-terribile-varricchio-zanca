package it.polimi.ingsw.common.transport.socket;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelContract;
import it.polimi.ingsw.common.transport.Liveness;
import it.polimi.ingsw.common.transport.TransportException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The conformance suite, run over a real loopback socket.
 *
 * <p>Same assertions as the in-process channel, which is the whole point: requirement S5 puts
 * both transports in one game, and that is only safe if they behave identically. Two suites
 * would drift.
 *
 * <p>The tests below the inherited ones are the socket's own — the things that can only go
 * wrong when there is an operating system in the middle.
 */
@DisplayName("a socket channel")
class SocketChannelTest extends ChannelContract {

    private final List<SocketServer> servers = new ArrayList<>();

    @Override
    protected Connected connect(Recorder<Event> atClient, Recorder<Command> atServer)
            throws Exception {

        BlockingQueue<Channel<Event, Command>> accepted = new LinkedBlockingQueue<>();
        SocketServer server = SocketServer.listening(0, channel -> {
            // A queue that does not wait for a taker. On RMI the connection handler runs
            // inside the client's own connect call, so a handler that blocked for a
            // rendezvous would block the client that is waiting for it to return.
            accepted.add(channel);
            return atServer;
        }, Liveness.DEFAULT);
        servers.add(server);

        Channel<Command, Event> client =
                SocketConnector.connect("localhost", server.port(), atClient, Liveness.DEFAULT);
        Channel<Event, Command> serverEnd = accepted.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(serverEnd, "the server never saw the connection");

        return new Connected(client, serverEnd, atClient, atServer);
    }

    @AfterEach
    void closeServers() {
        servers.forEach(SocketServer::close);
        servers.clear();
    }

    // ------------------------------------------------------------------ the socket's own

    @Test
    @DisplayName("connecting to nothing fails at once, rather than hanging")
    void nobodyListening() {
        assertThrows(TransportException.class,
                () -> SocketConnector.connect("localhost", 1, new Recorder<>(), Liveness.DEFAULT));
    }

    @Test
    @DisplayName("a server serves several clients at once")
    void severalClients() throws Exception {
        Connected first = open();
        Connected second = open();

        first.client().send(new LobbyCommand.Login("samuele"));
        second.client().send(new LobbyCommand.Login("chiara"));

        assertEquals(new LobbyCommand.Login("samuele"), first.atServer().next());
        assertEquals(new LobbyCommand.Login("chiara"), second.atServer().next());
    }

    @Test
    @DisplayName("closing the server closes the connections it accepted")
    void closingTheServerHangsUp() throws Exception {
        BlockingQueue<Channel<Event, Command>> accepted = new LinkedBlockingQueue<>();
        Recorder<Event> atClient = new Recorder<>();
        Recorder<Command> atServer = new Recorder<>();

        SocketServer server = SocketServer.listening(0, channel -> {
            // A queue that does not wait for a taker. On RMI the connection handler runs
            // inside the client's own connect call, so a handler that blocked for a
            // rendezvous would block the client that is waiting for it to return.
            accepted.add(channel);
            return atServer;
        }, Liveness.DEFAULT);

        Channel<Command, Event> client =
                SocketConnector.connect("localhost", server.port(), atClient, Liveness.DEFAULT);
        assertNotNull(accepted.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, server.connectionCount());

        server.close();

        assertNotNull(atClient.closure(), "the client was never told the server had gone");
        assertFalse(client.isOpen());
    }

    @Test
    @DisplayName("a client that vanishes is noticed, without it having said anything")
    void silenceIsNoticed() throws Exception {
        Liveness impatient = new Liveness(Duration.ofMillis(60), Duration.ofMillis(200));
        BlockingQueue<Channel<Event, Command>> accepted = new LinkedBlockingQueue<>();
        Recorder<Command> atServer = new Recorder<>();

        SocketServer server = SocketServer.listening(0, channel -> {
            // A queue that does not wait for a taker. On RMI the connection handler runs
            // inside the client's own connect call, so a handler that blocked for a
            // rendezvous would block the client that is waiting for it to return.
            accepted.add(channel);
            return atServer;
        }, impatient);
        servers.add(server);

        // A client that never sends a keep-alive: the whole reason liveness exists is that
        // this is indistinguishable, at the socket level, from a player who is thinking.
        try (java.net.Socket mute = new java.net.Socket("localhost", server.port())) {
            new java.io.ObjectOutputStream(mute.getOutputStream()).flush();
            assertNotNull(accepted.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));

            assertTrue(atServer.closure().contains("nothing heard"),
                    "a connection that says nothing for four beats should be given up on");
        }
    }

    @Test
    @DisplayName("a port already in use is refused, not silently taken over")
    void portAlreadyTaken() throws Exception {
        Connected pair = open();
        int taken = servers.get(0).port();

        assertThrows(TransportException.class,
                () -> SocketServer.listening(taken, channel -> pair.atServer(), Liveness.DEFAULT));
    }

    @Test
    @DisplayName("the server stops counting a client that hung up")
    void connectionsAreForgotten() throws Exception {
        Connected pair = open();
        SocketServer server = servers.get(0);
        assertEquals(1, server.connectionCount());

        pair.client().close();
        assertNotNull(pair.atServer().closure(), "the server should have noticed");

        assertEquals(0, server.connectionCount());
    }

    @Test
    @DisplayName("a client that sends events instead of commands has its connection closed")
    void aHostileClient() throws Exception {
        BlockingQueue<Channel<Event, Command>> accepted = new LinkedBlockingQueue<>();
        Recorder<Command> atServer = new Recorder<>();
        SocketServer server = SocketServer.listening(0, channel -> {
            // A queue that does not wait for a taker. On RMI the connection handler runs
            // inside the client's own connect call, so a handler that blocked for a
            // rendezvous would block the client that is waiting for it to return.
            accepted.add(channel);
            return atServer;
        }, Liveness.DEFAULT);
        servers.add(server);

        // Underneath the typed API on purpose: over a socket, nothing stops somebody writing
        // whatever they like, and the guard has to hold against real serialization rather
        // than against a compiler.
        try (java.net.Socket raw = new java.net.Socket("localhost", server.port())) {
            java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(raw.getOutputStream());
            out.flush();
            assertNotNull(accepted.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));

            out.writeObject(new it.polimi.ingsw.common.transport.Envelope.Message(
                    new GameEvent.PhaseBegan(GamePhase.FLIGHT)));
            out.flush();

            assertTrue(atServer.closure().contains("expected a Command"),
                    "a client sending events is not a client");
        }
    }

    @Test
    @DisplayName("a long game does not accumulate what it has already sent")
    void streamsAreReset() throws Exception {
        Connected pair = open();

        // The same object sent twice. Without the reset the second write would be a
        // back-reference, and the sender would still be holding the first.
        GameEvent.PhaseBegan repeated = new GameEvent.PhaseBegan(GamePhase.FLIGHT);
        for (int sent = 0; sent < 50; sent++) {
            pair.server().send(repeated);
        }

        for (int received = 0; received < 50; received++) {
            assertEquals(repeated, pair.atClient().next(), "message " + received + " went missing");
        }
    }
}
