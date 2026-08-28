package it.polimi.ingsw.common.transport;

import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every door has to do, whatever clients reach it through.
 *
 * <p>The companion to {@link ChannelContract}, one layer up, and it exists for the same
 * reason: requirement S5 puts a socket player and an RMI player in one game, which is only
 * safe if the two doors are interchangeable, and the only way to know they are is to hold
 * them to the same assertions rather than to two suites that drifted apart.
 *
 * <p>Before {@link Doorway} there was no type to write this against. The two servers were
 * separately tested through their channels, and the promise that they had the same shape was
 * a sentence in {@code RmiServer}'s Javadoc.
 *
 * <p>Components involved: {@link Doorway}, {@link Doorman}, {@link AbstractListeningPost}.
 */
public abstract class DoorwayContract {

    /** How long to wait for something that ought to happen almost at once. */
    protected static final long TIMEOUT_MS = 2_000;

    private final List<AutoCloseable> opened = new ArrayList<>();

    /**
     * Opens a door of the kind under test, on a port nobody is using.
     *
     * @param doorman  what to do with each new connection
     * @param liveness how hard connections should watch for silence
     * @return the door
     */
    protected abstract Doorway open(Doorman doorman, Liveness liveness);

    /**
     * Connects a client to a door of the kind under test.
     *
     * @param door     the door to knock on
     * @param listener what to do with what comes back
     * @return the client's end
     */
    protected abstract Channel<Command, Event> connectTo(Doorway door,
                                                         ChannelListener<Event> listener);

    /** Remembers something so that it is shut whatever the test does. */
    protected final <T extends AutoCloseable> T track(T closeable) {
        opened.add(closeable);
        return closeable;
    }

    @AfterEach
    void closeWhatWasOpened() {
        for (int i = opened.size() - 1; i >= 0; i--) {
            try {
                opened.get(i).close();
            } catch (Exception ignored) {
                // A test that already failed should not also fail to tidy up.
            }
        }
        opened.clear();
    }

    /** A listener that does nothing, for a client end whose arrivals are not the point. */
    protected static ChannelListener<Event> deaf() {
        return new ChannelListener<>() {
            @Override
            public void received(Event event) {
                // Not what this suite is about.
            }

            @Override
            public void closed(String reason) {
                // Not what this suite is about.
            }
        };
    }

    @Test
    @DisplayName("the port is the one actually bound, so a test never has to pick a number")
    void thePortIsTheOneActuallyBound() {
        Doorway door = track(open(channel -> deafServerSide(), Liveness.DEFAULT));

        assertNotEquals(0, door.port());
    }

    @Test
    @DisplayName("a connection is counted before the doorman returns, not after")
    void aConnectionIsCountedBeforeTheDoormanReturns() throws Exception {
        AtomicReference<Doorway> door = new AtomicReference<>();
        AtomicInteger seenFromInside = new AtomicInteger(-1);
        CountDownLatch answered = new CountDownLatch(1);

        door.set(track(open(channel -> {
            // A doorman is entitled to start using the channel at once, and to hand it to
            // something else that does. A door that counted the connection only once the
            // doorman returned would spend that whole window claiming to have none.
            seenFromInside.set(door.get().connectionCount());
            answered.countDown();
            return deafServerSide();
        }, Liveness.DEFAULT)));

        track(connectTo(door.get(), deaf()));

        assertTrue(answered.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "nobody was let in");
        assertEquals(1, seenFromInside.get());
    }

    @Test
    @DisplayName("closing the door closes what came through it")
    void closingTheDoorClosesTheConnections() throws Exception {
        CountDownLatch arrived = new CountDownLatch(1);
        Doorway door = track(open(channel -> {
            arrived.countDown();
            return deafServerSide();
        }, Liveness.DEFAULT));
        Channel<Command, Event> client = track(connectTo(door, deaf()));
        assertTrue(arrived.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        door.close();

        // A door that stopped listening but left its connections open would look shut down
        // and would not be.
        assertEquals(0, door.connectionCount());
        assertTrue(waitUntilClosed(client), "the client was left holding an open channel");
    }

    @Test
    @DisplayName("a connection that has dropped stops being counted")
    void aDroppedConnectionStopsBeingCounted() throws Exception {
        CountDownLatch arrived = new CountDownLatch(1);
        Doorway door = track(open(channel -> {
            arrived.countDown();
            return deafServerSide();
        }, Liveness.DEFAULT));
        Channel<Command, Event> client = track(connectTo(door, deaf()));
        assertTrue(arrived.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, door.connectionCount());

        client.close();

        assertTrue(waitFor(() -> door.connectionCount() == 0),
                "a door that never forgets a dropped client holds every one it ever had");
    }

    private static ChannelListener<Command> deafServerSide() {
        return new ChannelListener<>() {
            @Override
            public void received(Command command) {
                // Not what this suite is about.
            }

            @Override
            public void closed(String reason) {
                // Not what this suite is about.
            }
        };
    }

    private static boolean waitUntilClosed(Channel<Command, Event> channel) {
        return waitFor(() -> !channel.isOpen());
    }

    /** Polls, because a close travels over a wire and does not land on the calling thread. */
    private static boolean waitFor(java.util.function.BooleanSupplier condition) {
        long deadline = System.nanoTime() + TIMEOUT_MS * 1_000_000L;
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
}
