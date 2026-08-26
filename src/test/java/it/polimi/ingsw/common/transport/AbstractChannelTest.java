package it.polimi.ingsw.common.transport;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the half of a transport that has nothing to do with moving bytes.
 *
 * <p>{@link ChannelContract} covers what a transport looks like from outside, which is what
 * matters for requirement S5. It cannot reach three things that both real transports will
 * inherit and neither will re-implement: the heartbeat, which needs a connection that can go
 * quiet; the type guard, which needs a peer willing to send the wrong thing; and what happens
 * when transmission itself fails.
 *
 * <p>So this drives {@link AbstractChannel} through a stand-in with no I/O in it, where all
 * three can simply be arranged.
 */
class AbstractChannelTest {

    /** A channel whose wire is a list, so a test can look at it and write to it. */
    private static final class Scripted extends AbstractChannel<Event, Command> {

        private final List<Envelope> sent = new CopyOnWriteArrayList<>();
        private final boolean transmissionFails;
        private boolean released;

        Scripted(ChannelListener<Command> listener, Liveness liveness, boolean transmissionFails) {
            super(listener, Command.class, liveness);
            this.transmissionFails = transmissionFails;
            start();
        }

        Scripted(ChannelListener<Command> listener) {
            super(listener, Command.class);
            this.transmissionFails = false;
        }

        @Override
        protected void transmit(Envelope envelope) throws Exception {
            if (transmissionFails) {
                throw new java.io.IOException("the wire is cut");
            }
            sent.add(envelope);
        }

        @Override
        protected void release() {
            released = true;
        }

        void arrive(Serializable payload) {
            deliver(new Envelope.Message(payload));
        }

        void arriveKeepAlive() {
            deliver(new Envelope.KeepAlive());
        }
    }

    private final ChannelContract.Recorder<Command> heard = new ChannelContract.Recorder<>();

    @Nested
    @DisplayName("keep-alives")
    class KeepAlives {

        private final Liveness impatient =
                new Liveness(Duration.ofMillis(40), Duration.ofMillis(120));

        @Test
        @DisplayName("a connection that goes quiet is given up on")
        void silenceCloses() throws InterruptedException {
            Scripted channel = new Scripted(heard, impatient, false);

            Thread.sleep(500);

            assertFalse(channel.isOpen(), "nothing arrived for four times the patience");
            assertTrue(heard.closure().contains("nothing heard"),
                    "and the listener should have been told why");
            assertTrue(channel.released, "the connection should have been let go of");
        }

        @Test
        @DisplayName("a connection that keeps talking stays open")
        void chatterKeepsItAlive() throws InterruptedException {
            Scripted channel = new Scripted(heard, impatient, false);

            for (int beat = 0; beat < 10; beat++) {
                Thread.sleep(40);
                channel.arriveKeepAlive();
            }

            assertTrue(channel.isOpen(), "the other end never stopped talking");
        }

        @Test
        @DisplayName("something is actually sent, or the other end would give up on us")
        void beatsAreSent() throws InterruptedException {
            Scripted channel = new Scripted(heard, impatient, false);

            Thread.sleep(110);

            assertTrue(channel.sent.stream().anyMatch(Envelope.KeepAlive.class::isInstance),
                    "a heartbeat that only listens is half a heartbeat");
        }

        @Test
        @DisplayName("a keep-alive is answered here and never passed up")
        void beatsStayBelow() {
            Scripted channel = new Scripted(heard);

            channel.arriveKeepAlive();
            channel.arrive(new LobbyCommand.ListGames());

            assertEquals(new LobbyCommand.ListGames(), heard.next(),
                    "the keep-alive should not have arrived first");
        }

        @Test
        @DisplayName("allowing less silence than the beat would close every working connection")
        void impossibleSettings() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Liveness(Duration.ofSeconds(2), Duration.ofSeconds(1)));
            assertThrows(IllegalArgumentException.class,
                    () -> new Liveness(Duration.ofSeconds(2), Duration.ofSeconds(2)));
            assertThrows(IllegalArgumentException.class,
                    () -> new Liveness(Duration.ZERO, Duration.ofSeconds(2)));
            assertThrows(NullPointerException.class, () -> new Liveness(null, Duration.ofSeconds(2)));

            assertEquals(3, Liveness.DEFAULT.missedBeatsAllowed());
        }
    }

    @Nested
    @DisplayName("what arrives")
    class Arrivals {

        @Test
        @DisplayName("a message of the wrong kind closes the connection instead of surfacing later")
        void theWrongKindOfMessage() {
            Scripted channel = new Scripted(heard);

            channel.arrive(new GameEvent.PhaseBegan(GamePhase.FLIGHT));

            assertFalse(channel.isOpen(), "a client sending events is not a client");
            assertTrue(heard.closure().contains("expected a Command"),
                    "and the reason should say what was wrong");
        }

        @Test
        @DisplayName("anything arriving after the close is dropped")
        void arrivalsAfterClosing() {
            Scripted channel = new Scripted(heard);
            channel.close();

            channel.arrive(new LobbyCommand.ListGames());

            assertTrue(heard.isEmpty(), "a closed channel delivered a message anyway");
        }

        @Test
        @DisplayName("an envelope carries something")
        void emptyEnvelope() {
            assertThrows(NullPointerException.class, () -> new Envelope.Message(null));
        }
    }

    @Nested
    @DisplayName("what goes wrong")
    class Failures {

        @Test
        @DisplayName("a send that fails is a disconnection, not an exception for the caller")
        void transmissionFailure() {
            Scripted channel = new Scripted(heard, Liveness.DEFAULT, true);

            channel.send(new GameEvent.GameEnded());

            assertFalse(channel.isOpen());
            assertTrue(heard.closure().contains("the wire is cut"),
                    "the reason should carry what actually happened");
        }

        @Test
        @DisplayName("sending nothing is a mistake in the caller, and says so")
        void sendingNothing() {
            Scripted channel = new Scripted(heard);

            assertThrows(NullPointerException.class, () -> channel.send(null));
        }

        @Test
        @DisplayName("a channel needs a listener and a message type")
        void incompleteChannel() {
            assertThrows(NullPointerException.class, () -> new Scripted(null));
            assertThrows(NullPointerException.class,
                    () -> new Scripted(heard, null, false));
        }
    }
}
