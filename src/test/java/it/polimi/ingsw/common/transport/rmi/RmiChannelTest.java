package it.polimi.ingsw.common.transport.rmi;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelContract;
import it.polimi.ingsw.common.transport.Envelope;
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
 * The conformance suite, run over RMI.
 *
 * <p>Third and last. The same nine assertions have now passed against a direct handoff inside
 * one process, a socket, and a registry — which is what requirement S5 needs, because a game
 * with players on both transports is only safe if the transports are indistinguishable from
 * above.
 *
 * <p>Nothing here is adapted for RMI. If it had to be, the abstraction would be leaking.
 *
 * <p>Components involved: {@link RmiChannel}, {@link RmiServer}, {@link RmiConnector}.
 */
@DisplayName("an RMI channel")
class RmiChannelTest extends ChannelContract {

    private final List<RmiServer> servers = new ArrayList<>();

    @Override
    protected Connected connect(Recorder<Event> atClient, Recorder<Command> atServer)
            throws Exception {

        BlockingQueue<Channel<Event, Command>> accepted = new LinkedBlockingQueue<>();
        RmiServer server = start(accepted, atServer, Liveness.DEFAULT);

        Channel<Command, Event> client =
                RmiConnector.connect("localhost", server.port(), atClient, Liveness.DEFAULT);
        Channel<Event, Command> serverEnd = accepted.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(serverEnd, "the server never saw the connection");

        return new Connected(client, serverEnd, atClient, atServer);
    }

    private RmiServer start(BlockingQueue<Channel<Event, Command>> accepted,
                            Recorder<Command> atServer, Liveness liveness) {
        RmiServer server = RmiServer.listening(0, channel -> {
            // A queue that does not wait for a taker. On RMI the connection handler runs
            // inside the client's own connect call, so a handler that blocked for a
            // rendezvous would block the client that is waiting for it to return.
            accepted.add(channel);
            return atServer;
        }, liveness);
        servers.add(server);
        return server;
    }

    @AfterEach
    void closeServers() {
        servers.forEach(RmiServer::close);
        servers.clear();
    }

    // ------------------------------------------------------------------ RMI's own

    @Test
    @DisplayName("a registry with nothing in it is not a server")
    void nothingBound() {
        assertThrows(TransportException.class,
                () -> RmiConnector.connect("localhost", 1, new Recorder<>(), Liveness.DEFAULT));
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
        Connected pair = open();
        assertEquals(1, servers.get(0).connectionCount());

        servers.get(0).close();

        assertNotNull(pair.atServer().closure(), "the server end should have been closed");
        assertFalse(pair.server().isOpen());
    }

    @Test
    @DisplayName("a client that stops calling is noticed, which is the case RMI cannot see by itself")
    void silenceIsNoticed() throws Exception {
        // The reason liveness is not left to RMI. A remote call fails promptly when the far
        // side has gone — but only if there is a call to make, and a player who is thinking
        // makes none. Without a heartbeat a vanished client is indistinguishable from a quiet
        // one for as long as nobody sends them anything.
        Liveness impatient = new Liveness(Duration.ofMillis(60), Duration.ofMillis(200));
        BlockingQueue<Channel<Event, Command>> accepted = new LinkedBlockingQueue<>();
        Recorder<Command> atServer = new Recorder<>();
        RmiServer server = start(accepted, atServer, impatient);

        // An endpoint that is exported and reachable, and never says anything.
        RmiChannel<Command, Event> mute =
                RmiChannel.exported(Event.class, impatient, ignored -> new Recorder<Event>());
        java.rmi.registry.Registry registry =
                java.rmi.registry.LocateRegistry.getRegistry("localhost", server.port());
        ((RemoteGateway) registry.lookup(RemoteGateway.NAME)).connect(mute);
        assertNotNull(accepted.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(atServer.closure().contains("nothing heard"),
                "a client that never calls should still be given up on");
    }

    @Test
    @DisplayName("a client that sends events instead of commands has its connection closed")
    void aHostileClient() throws Exception {
        BlockingQueue<Channel<Event, Command>> accepted = new LinkedBlockingQueue<>();
        Recorder<Command> atServer = new Recorder<>();
        RmiServer server = start(accepted, atServer, Liveness.DEFAULT);

        // Straight at the remote interface, which is all a hostile client would have to do:
        // the type parameters that make this impossible in Java are gone by the time an
        // envelope is on the wire.
        RmiChannel<Command, Event> mine =
                RmiChannel.exported(Event.class, Liveness.DEFAULT, ignored -> new Recorder<Event>());
        java.rmi.registry.Registry registry =
                java.rmi.registry.LocateRegistry.getRegistry("localhost", server.port());
        RemoteEndpoint serverEnd =
                ((RemoteGateway) registry.lookup(RemoteGateway.NAME)).connect(mine);
        assertNotNull(accepted.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        serverEnd.accept(new Envelope.Message(new GameEvent.PhaseBegan(GamePhase.FLIGHT)));

        assertTrue(atServer.closure().contains("expected a Command"),
                "a client sending events is not a client, over RMI either");
    }

    @Test
    @DisplayName("sending does not wait for the other end to finish with the message")
    void sendingDoesNotBlockOnTheHandler() throws Exception {
        // A remote call blocks until the far side returns, so the obvious implementation
        // would leave a sender waiting on the receiver's handler. A socket does not do that,
        // and a transport that did would not be interchangeable with one that does not.
        BlockingQueue<Channel<Event, Command>> accepted = new LinkedBlockingQueue<>();
        java.util.concurrent.BlockingQueue<Command> eventually =
                new java.util.concurrent.LinkedBlockingQueue<>();

        RmiServer server = RmiServer.listening(0, channel -> {
            accepted.add(channel);
            return new it.polimi.ingsw.common.transport.ChannelListener<Command>() {
                @Override
                public void received(Command message) {
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    eventually.add(message);
                }

                @Override
                public void closed(String reason) {
                    // Not what this test is about.
                }
            };
        }, Liveness.DEFAULT);
        servers.add(server);

        Channel<Command, Event> client =
                RmiConnector.connect("localhost", server.port(), new Recorder<>(), Liveness.DEFAULT);
        assertNotNull(accepted.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        long before = System.nanoTime();
        client.send(new LobbyCommand.ListGames());
        long elapsed = Duration.ofNanos(System.nanoTime() - before).toMillis();

        assertTrue(elapsed < 200, "send waited " + elapsed + "ms for a handler that takes 400ms");
        assertEquals(new LobbyCommand.ListGames(),
                eventually.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS), "and it still arrived");
    }
}
