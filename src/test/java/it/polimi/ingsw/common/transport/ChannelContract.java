package it.polimi.ingsw.common.transport;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every transport has to do, whatever it is made of.
 *
 * <p>Extended once per transport, so that the socket and the RMI implementations are held to
 * the same behaviour by the same assertions rather than by two suites that drifted apart.
 * Requirement S5 puts both in one game; the only way that is safe is if they are
 * interchangeable, and the only way to know they are interchangeable is to test them the
 * same way.
 *
 * <p>Nothing here assumes delivery is synchronous, immediate or ordered with respect to the
 * calling thread. Everything waits on a queue with a timeout, because the in-process channel
 * delivers on the caller's thread and a socket does not, and a suite that worked for one and
 * not the other would be worth very little.
 */
public abstract class ChannelContract {

    /** How long to wait for something that ought to happen almost at once. */
    protected static final long TIMEOUT_MS = 2_000;

    /**
     * How long to wait before concluding that nothing is going to happen.
     *
     * <p>Much shorter than {@link #TIMEOUT_MS}, and for the opposite reason. Waiting two
     * seconds to prove a closed channel delivered nothing costs two seconds per assertion
     * per transport, and buys nothing: if a message were going to arrive it would already
     * have done.
     */
    protected static final long QUIET_MS = 150;

    private final List<AutoCloseable> opened = new ArrayList<>();

    /**
     * Both ends of a connection, and what each of them has heard.
     *
     * @param client  the end that sends commands
     * @param server  the end that sends events
     * @param atClient what the client end has been told
     * @param atServer what the server end has been told
     */
    public record Connected(Channel<Command, Event> client, Channel<Event, Command> server,
                               Recorder<Event> atClient, Recorder<Command> atServer) {
    }

    /**
     * Opens one connection of whatever kind is under test.
     *
     * @param atClient what the client end should do with what arrives
     * @param atServer what the server end should do with what arrives
     * @return both ends, connected
     * @throws Exception if the connection cannot be made
     */
    protected abstract Connected connect(Recorder<Event> atClient, Recorder<Command> atServer)
            throws Exception;

    /**
     * Opens a connection and remembers to close it.
     *
     * @return both ends, connected
     * @throws Exception if the connection cannot be made
     */
    protected final Connected open() throws Exception {
        Connected pair = connect(new Recorder<>(), new Recorder<>());
        opened.add(pair.client());
        opened.add(pair.server());
        return pair;
    }

    @AfterEach
    final void closeEverything() {
        opened.forEach(channel -> {
            try {
                channel.close();
            } catch (Exception ignored) {
                // Closing a test fixture. Nothing useful to do about a failure here.
            }
        });
        opened.clear();
    }

    // ------------------------------------------------------------------ carrying messages

    @Test
    @DisplayName("a command sent by the client arrives at the server")
    void commandsTravelUp() throws Exception {
        Connected pair = open();

        pair.client().send(new LobbyCommand.Login("samuele"));

        assertEquals(new LobbyCommand.Login("samuele"), pair.atServer().next());
    }

    @Test
    @DisplayName("an event sent by the server arrives at the client")
    void eventsTravelDown() throws Exception {
        Connected pair = open();

        pair.server().send(new GameEvent.PhaseBegan(GamePhase.BUILDING));

        assertEquals(new GameEvent.PhaseBegan(GamePhase.BUILDING), pair.atClient().next());
    }

    @Test
    @DisplayName("messages arrive in the order they were sent")
    void orderIsKept() throws Exception {
        Connected pair = open();

        pair.client().send(new LobbyCommand.ListGames());
        pair.client().send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 3));
        pair.client().send(new LobbyCommand.LeaveGame());

        assertEquals(new LobbyCommand.ListGames(), pair.atServer().next());
        assertEquals(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 3), pair.atServer().next());
        assertEquals(new LobbyCommand.LeaveGame(), pair.atServer().next());
    }

    @Test
    @DisplayName("a message crosses as a copy, not as the object that was sent")
    void messagesAreCopied() throws Exception {
        Connected pair = open();
        LobbyCommand.Login sent = new LobbyCommand.Login("samuele");

        pair.client().send(sent);
        Command received = pair.atServer().next();

        assertEquals(sent, received, "it has to be the same message");
        assertTrue(received instanceof LobbyCommand.Login, "and the same kind of message");
    }

    @Test
    @DisplayName("a keep-alive is answered by the transport and never seen above it")
    void keepAlivesStayBelow() throws Exception {
        Connected pair = open();

        pair.client().send(new LobbyCommand.ListGames());
        pair.client().send(new LobbyCommand.LeaveGame());

        // If keep-alives were passed up, they would land between these two.
        assertEquals(new LobbyCommand.ListGames(), pair.atServer().next());
        assertEquals(new LobbyCommand.LeaveGame(), pair.atServer().next());
    }

    // ------------------------------------------------------------------ closing

    @Test
    @DisplayName("closing one end is noticed at the other")
    void closingIsNoticed() throws Exception {
        Connected pair = open();

        pair.client().close();

        assertNotNull(pair.atServer().closure(), "the server was never told the client had gone");
        assertFalse(pair.client().isOpen());
    }

    @Test
    @DisplayName("the listener is told exactly once, however many times it is closed")
    void closingTwiceTellsOnce() throws Exception {
        Connected pair = open();

        pair.client().close();
        pair.client().close();
        pair.client().close();

        assertEquals(1, pair.atClient().closures(), "a channel closes once");
    }

    @Test
    @DisplayName("sending to a closed channel does nothing and does not throw")
    void sendingAfterClosingIsHarmless() throws Exception {
        Connected pair = open();
        pair.client().close();

        pair.client().send(new LobbyCommand.ListGames());

        assertFalse(pair.client().isOpen());
        assertTrue(pair.atServer().isEmpty(), "a closed channel delivered a message anyway");
    }

    @Test
    @DisplayName("a channel is open until it is not")
    void openUntilClosed() throws Exception {
        Connected pair = open();

        assertTrue(pair.client().isOpen());
        assertTrue(pair.server().isOpen());

        pair.server().close();

        assertFalse(pair.server().isOpen());
    }

    // ------------------------------------------------------------------ recording

    /**
     * A listener that keeps what it was told, so a test can wait for it.
     *
     * @param <M> what this end receives
     */
    public static final class Recorder<M> implements ChannelListener<M> {

        private final BlockingQueue<M> received = new LinkedBlockingQueue<>();
        private final BlockingQueue<String> closures = new LinkedBlockingQueue<>();

        @Override
        public void received(M message) {
            received.add(message);
        }

        @Override
        public void closed(String reason) {
            closures.add(reason);
        }

        /**
         * Waits for the next message.
         *
         * @return what arrived
         * @throws AssertionError if nothing arrives in time
         */
        public M next() {
            M message = poll(received);
            if (message == null) {
                throw new AssertionError("nothing arrived within " + TIMEOUT_MS + "ms");
            }
            return message;
        }

        /**
         * Waits for the channel to close.
         *
         * @return why it closed, or {@code null} if it did not
         */
        public String closure() {
            return poll(closures);
        }

        /**
         * Returns how many times this end was told the channel had closed.
         *
         * <p>Waits for one, then waits a little longer to see whether a second turns up.
         *
         * @return the number of closures reported
         */
        public int closures() {
            if (poll(closures, TIMEOUT_MS) == null) {
                return 0;
            }
            int seen = 1;
            while (poll(closures, QUIET_MS) != null) {
                seen++;
            }
            return seen;
        }

        /**
         * Tells whether anything arrived, after waiting long enough that it would have.
         *
         * @return {@code true} when nothing came
         */
        public boolean isEmpty() {
            return poll(received, QUIET_MS) == null;
        }

        private <T> T poll(BlockingQueue<T> queue) {
            return poll(queue, TIMEOUT_MS);
        }

        private <T> T poll(BlockingQueue<T> queue, long millis) {
            try {
                return queue.poll(millis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted while waiting", interrupted);
            }
        }
    }
}
