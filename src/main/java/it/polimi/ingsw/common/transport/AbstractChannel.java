package it.polimi.ingsw.common.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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

    private static final Logger LOG = LoggerFactory.getLogger(AbstractChannel.class);

    private static final ScheduledExecutorService CLOCK =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "channel-liveness");
                thread.setDaemon(true);
                return thread;
            });

    private final AtomicReference<ChannelListener<I>> listener = new AtomicReference<>();
    private final Class<I> expected;
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final AtomicLong lastHeard = new AtomicLong(System.nanoTime());
    private final AtomicReference<ScheduledFuture<?>> heartbeat = new AtomicReference<>();
    private final Liveness liveness;

    /**
     * What this connection is called in the trace.
     *
     * <p>The class names the door it came through — a stream, a registry, or neither — and the
     * identity distinguishes two connections of the same kind, which is the whole point when
     * four players are in one process.
     */
    private final String id = getClass().getSimpleName() + "@"
            + Integer.toHexString(System.identityHashCode(this));

    /**
     * Builds a channel that will watch for silence once it is started.
     *
     * @param expected what this side receives, so that a message of the wrong kind is
     *                 reported as a protocol error rather than thrown deep inside a handler
     * @param liveness how hard to try to notice the other end going away
     * @throws NullPointerException if either argument is {@code null}
     */
    protected AbstractChannel(Class<I> expected, Liveness liveness) {
        if (expected == null || liveness == null) {
            throw new NullPointerException("a channel needs a message type and liveness");
        }
        this.expected = expected;
        this.liveness = liveness;
    }

    /**
     * Builds a channel that does not watch for silence.
     *
     * <p>For connections inside one process, where the other end cannot go away without this
     * one going with it, and there is nothing a heartbeat could discover.
     *
     * @param expected what this side receives
     * @throws NullPointerException if it is {@code null}
     */
    protected AbstractChannel(Class<I> expected) {
        if (expected == null) {
            throw new NullPointerException("a channel needs a message type");
        }
        this.expected = expected;
        this.liveness = null;
    }

    /**
     * Says what to do with what arrives.
     *
     * <p>Separate from the constructor, and it has to be. A server accepting a connection
     * usually wants a listener that can answer on the very channel being built, so the
     * listener cannot exist before the channel does. Both transports do the same three
     * things in the same order: build, install, start.
     *
     * @param listener what to do with what arrives
     * @throws NullPointerException  if the listener is {@code null}
     * @throws IllegalStateException if one has already been installed
     */
    protected final void listenWith(ChannelListener<I> listener) {
        if (listener == null) {
            throw new NullPointerException("a channel needs a listener");
        }
        if (!this.listener.compareAndSet(null, listener)) {
            throw new IllegalStateException("this channel already has a listener");
        }
    }

    /**
     * Starts the channel, once the subclass is ready to be used.
     *
     * <p>Separate from the constructor because the heartbeat is a timer that calls
     * {@link #transmit} on another thread, and a constructor that started one would be
     * calling into a subclass whose own fields are not assigned yet. Every transport here is
     * built by a static factory that calls this as its last act, so there is nowhere for a
     * caller to forget it.
     *
     * <p>Harmless to call twice.
     *
     * @throws IllegalStateException if no listener has been installed, which would mean
     *                               whatever arrived first had nowhere to go
     */
    protected final void start() {
        if (listener.get() == null) {
            throw new IllegalStateException("this channel has no listener, so nothing can arrive");
        }
        if (liveness == null || heartbeat.get() != null) {
            return;
        }
        LOG.debug("{} open, {}", id, liveness == null ? "no heartbeat" : liveness);
        long period = liveness.keepAliveEvery().toMillis();
        heartbeat.set(CLOCK.scheduleAtFixedRate(
                this::beat, period, period, TimeUnit.MILLISECONDS));
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
        if (!open.get()) {
            return;
        }
        // No default arm, on purpose. This used to be two instanceof checks ending in a cast
        // to Message, which meant a fourth kind of envelope — a resend, a version handshake,
        // a flow-control frame — would compile everywhere and throw ClassCastException on the
        // reading thread of every live connection. That is the worst place in the project for
        // an unchecked cast to be: this is the module whose whole job is that a dropped
        // connection never throws, and nothing up there is catching. Now it stops the build.
        switch (envelope) {
            case Envelope.KeepAlive ignored -> {
                LOG.trace("{} <- keep-alive", id);
                // Consumed here. Nothing above the transport ever learns that the connection
                // has to keep proving it works.
            }
            case Envelope.Goodbye ignored -> {
                LOG.trace("{} <- goodbye", id);
                shutdown("the other end said goodbye");
            }
            case Envelope.Message message -> {
                // Parameterised, so a payload's toString is never built when nobody is
                // listening — which on a StateChanged is a whole projection.
                LOG.debug("{} <- {}", id, message.payload().getClass().getSimpleName());
                passUp(message.payload());
            }
        }
    }

    /**
     * Checks what arrived is the kind this side expects, and hands it to the listener.
     *
     * <p>Split out so that {@link #deliver} is three arms and nothing else: what an envelope
     * means and what is inside one are two questions, and reading them together was part of
     * why the cast went unnoticed.
     */
    private void passUp(Serializable payload) {
        if (!expected.isInstance(payload)) {
            failed("expected a " + expected.getSimpleName() + " and got a "
                    + payload.getClass().getSimpleName());
            return;
        }
        listener.get().received(expected.cast(payload));
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
            LOG.debug("{} -> {}", id, message.getClass().getSimpleName());
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
        return liveness == null
                ? Optional.empty()
                : Optional.of(Duration.ofNanos(System.nanoTime() - lastHeard.get()));
    }

    private void beat() {
        if (!open.get()) {
            return;
        }
        Duration silenceAllowed = liveness.silenceAllowed();
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
        ScheduledFuture<?> beating = heartbeat.get();
        if (beating != null) {
            beating.cancel(false);
        }
        try {
            // Best effort, and before letting go of the connection: on a transport where a
            // close is otherwise invisible, this is the only thing that tells the other end
            // promptly. It goes through transmit rather than send because send has already
            // been switched off by the line above.
            transmit(new Envelope.Goodbye());
        } catch (Exception alreadyGone) {
            // The connection is closing and may well be why. Nothing to report.
        }
        try {
            release();
        } catch (RuntimeException problem) {
            // Already closing. There is nowhere useful for this to go.
        }
        LOG.debug("{} closed: {}", id, reason);
        ChannelListener<I> told = listener.get();
        if (told != null) {
            told.closed(reason);
        }
    }
}
