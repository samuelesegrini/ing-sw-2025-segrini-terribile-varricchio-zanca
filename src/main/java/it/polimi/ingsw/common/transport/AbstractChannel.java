package it.polimi.ingsw.common.transport;

import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Everything a channel does that has nothing to do with how the bytes move.
 *
 * <p>There are two transports and they share more than they differ by. Both have to wrap
 * outgoing messages, unwrap incoming ones, answer keep-alives without bothering anybody
 * above, notice when the other end has gone quiet, close exactly once however many things
 * decide to close them, and refuse to blow up when somebody sends to a connection that is
 * already gone. Written twice, those are two chances to get the double-close wrong.
 *
 * <p>So a transport implements two methods — {@link #transmit} and {@link #release} — and
 * calls two — {@link #deliver} and {@link #failed}. Everything else is here.
 *
 * <p><b>On threads.</b> A subclass reads on whatever thread it likes and calls
 * {@code deliver} from there; the heartbeat runs on a scheduler. The state that matters
 * — open or not, when something last arrived — is atomic, and {@link #close()} is written
 * so that the first caller does the work and the rest return.
 *
 * @param <O> what this side sends
 * @param <I> what this side receives
 */
public abstract class AbstractChannel<O extends Serializable, I extends Serializable>
        implements Channel<O, I> {

    private static final ScheduledExecutorService CLOCK =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "channel-liveness");
                thread.setDaemon(true);
                return thread;
            });

    private final ChannelListener<I> listener;
    private final Class<I> expected;
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final AtomicLong lastHeard = new AtomicLong(System.nanoTime());
    private final ScheduledFuture<?> heartbeat;
    private final Duration silenceAllowed;

    /**
     * Opens a channel that watches for silence.
     *
     * @param listener what to do with what arrives
     * @param expected what this side receives, so that a message of the wrong kind is
     *                 reported as a protocol error rather than thrown deep inside a handler
     * @param liveness how hard to try to notice the other end going away
     * @throws NullPointerException if any argument is {@code null}
     */
    protected AbstractChannel(ChannelListener<I> listener, Class<I> expected, Liveness liveness) {
        if (listener == null || expected == null || liveness == null) {
            throw new NullPointerException("a channel needs a listener, a message type and liveness");
        }
        this.listener = listener;
        this.expected = expected;
        this.silenceAllowed = liveness.silenceAllowed();
        long period = liveness.keepAliveEvery().toMillis();
        this.heartbeat = CLOCK.scheduleAtFixedRate(
                this::beat, period, period, TimeUnit.MILLISECONDS);
    }

    /**
     * Opens a channel that does not watch for silence.
     *
     * <p>For connections inside one process, where the other end cannot go away without this
     * one going with it, and there is nothing a heartbeat could discover.
     *
     * @param listener what to do with what arrives
     * @param expected what this side receives
     * @throws NullPointerException if either argument is {@code null}
     */
    protected AbstractChannel(ChannelListener<I> listener, Class<I> expected) {
        if (listener == null || expected == null) {
            throw new NullPointerException("a channel needs a listener and a message type");
        }
        this.listener = listener;
        this.expected = expected;
        this.silenceAllowed = null;
        this.heartbeat = null;
    }

    // ------------------------------------------------------------------ what a transport implements

    /**
     * Puts one envelope on the wire.
     *
     * @param envelope what to send
     * @throws Exception if the connection failed; the channel closes and reports it
     */
    protected abstract void transmit(Envelope envelope) throws Exception;

    /**
     * Lets go of whatever the connection was made of.
     *
     * <p>Called once, by {@link #close()}, and only by the thread that won the race to close.
     * An implementation may assume it is alone and does not need to be idempotent.
     */
    protected abstract void release();

    // ------------------------------------------------------------------ what a transport calls

    /**
     * Takes delivery of one envelope from the wire.
     *
     * <p>Keep-alives are consumed here and never reach the listener. Anything else is
     * unwrapped, checked and passed up.
     *
     * @param envelope what arrived
     */
    protected final void deliver(Envelope envelope) {
        lastHeard.set(System.nanoTime());
        if (!open.get() || envelope instanceof Envelope.KeepAlive) {
            return;
        }
        Serializable payload = ((Envelope.Message) envelope).payload();
        if (!expected.isInstance(payload)) {
            failed("expected a " + expected.getSimpleName() + " and got a "
                    + payload.getClass().getSimpleName());
            return;
        }
        listener.received(expected.cast(payload));
    }

    /**
     * Reports that the connection has failed and closes it.
     *
     * @param reason what went wrong, in a sentence
     */
    protected final void failed(String reason) {
        shutdown(reason);
    }

    // ------------------------------------------------------------------ the channel itself

    @Override
    public final void send(O message) {
        if (message == null) {
            throw new NullPointerException("there is no message to send");
        }
        if (!open.get()) {
            return;
        }
        try {
            transmit(new Envelope.Message(message));
        } catch (Exception problem) {
            shutdown("could not send: " + problem);
        }
    }

    @Override
    public final boolean isOpen() {
        return open.get();
    }

    @Override
    public final void close() {
        shutdown("closed");
    }

    /**
     * Returns how long it has been since anything arrived.
     *
     * <p>For reporting. Nothing decides anything from this except the heartbeat.
     *
     * @return the silence so far, or empty on a channel that is not watching for it
     */
    protected final Optional<Duration> silence() {
        return silenceAllowed == null
                ? Optional.empty()
                : Optional.of(Duration.ofNanos(System.nanoTime() - lastHeard.get()));
    }

    private void beat() {
        if (!open.get()) {
            return;
        }
        if (System.nanoTime() - lastHeard.get() > silenceAllowed.toNanos()) {
            shutdown("nothing heard for " + silenceAllowed);
            return;
        }
        try {
            transmit(new Envelope.KeepAlive());
        } catch (Exception problem) {
            shutdown("could not send a keep-alive: " + problem);
        }
    }

    private void shutdown(String reason) {
        if (!open.compareAndSet(true, false)) {
            // Both ends deciding to close at the same moment is the normal case, not a race
            // worth reporting. The first one through does the work.
            return;
        }
        if (heartbeat != null) {
            heartbeat.cancel(false);
        }
        try {
            release();
        } catch (RuntimeException problem) {
            // Already closing. There is nowhere useful for this to go.
        }
        listener.closed(reason);
    }
}
