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
 *
 * <p>Components involved: {@link AbstractChannel}, {@link Command}, {@link Liveness}.
 */
class AbstractChannelTest {

    /** A channel whose wire is a list, so a test can look at it and write to it. */
    private static final class Scripted extends AbstractChannel<Event, Command> {

        private final List<Envelope> sent = new CopyOnWriteArrayList<>();
        private final boolean transmissionFails;
        private boolean released;

        Scripted(ChannelListener<Command> listener, Liveness liveness, boolean transmissionFails) {
            super(Command.class, liveness);
            this.transmissionFails = transmissionFails;
            listenWith(listener);
            start();
        }

        Scripted(ChannelListener<Command> listener) {
            super(Command.class);
            this.transmissionFails = false;
            listenWith(listener);
            start();
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

        void arriveGoodbye() {
            deliver(new Envelope.Goodbye());
        }

        void listenAgain(ChannelListener<Command> listener) {
            listenWith(listener);
        }
    }

    private final ChannelContract.Recorder<Command> heard = new ChannelContract.Recorder<>();

    @Nested
    @DisplayName("the three kinds of envelope")
    class ThreeKindsOfEnvelope {

        // deliver used to be two instanceof checks ending in a cast to Message, so a fourth
        // kind of envelope would have compiled and thrown ClassCastException on a live
        // connection's reading thread. It is a switch with no default now, which the compiler
        // holds exhaustive; these pin what each of the three arms actually does, and the
        // goodbye arm had no test at all before this.

        @Test
        @DisplayName("a message is unwrapped and passed up")
        void aMessageReachesTheListener() {
            Scripted channel = new Scripted(heard);

            channel.arrive(new LobbyCommand.ListGames());

            assertEquals(new LobbyCommand.ListGames(), heard.next());
            assertTrue(channel.isOpen(), "a message is not a reason to hang up");
        }

        @Test
        @DisplayName("a keep-alive is swallowed, and nothing above the transport hears it")
        void aKeepAliveIsSwallowed() {
            Scripted channel = new Scripted(heard);

            channel.arriveKeepAlive();

            assertTrue(heard.isEmpty(),
                    "the listener was told the connection is proving it still works");
            assertTrue(channel.isOpen());
        }

        @Test
        @DisplayName("a goodbye closes the channel and says so")
        void aGoodbyeCloses() {
            Scripted channel = new Scripted(heard);

            channel.arriveGoodbye();

            assertFalse(channel.isOpen(), "the other end said it was going");
            assertTrue(heard.closure().contains("goodbye"),
                    "and the listener should have been told why");
            assertTrue(channel.released, "the connection should have been let go of");
        }

        @Test
        @DisplayName("a goodbye is not passed up as a message")
        void aGoodbyeIsNotAMessage() {
            Scripted channel = new Scripted(heard);

            channel.arriveGoodbye();

            // The cast this replaced would have made a Goodbye a ClassCastException, not a
            // message — but a default arm that fell through to passUp would have made it one.
            assertTrue(heard.isEmpty());
        }
    }

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

        @Test
        @DisplayName("a channel started without a listener says so, rather than dropping the first message")
        void startingWithNowhereToDeliver() {
            AbstractChannel<Event, Command> unlistened = new AbstractChannel<>(Command.class) {
                @Override
                protected void transmit(Envelope envelope) {
                    // Nothing is ever sent through this one.
                }

                @Override
                protected void release() {
                    // Nothing to let go of.
                }
            };

            assertThrows(IllegalStateException.class, unlistened::start);
        }

        @Test
        @DisplayName("a listener is installed once")
        void oneListenerOnly() {
            Scripted channel = new Scripted(heard);

            assertThrows(IllegalStateException.class, () -> channel.listenAgain(heard));
        }
    }
}
