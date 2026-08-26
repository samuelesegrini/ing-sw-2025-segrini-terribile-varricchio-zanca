package it.polimi.ingsw.common.transport.rmi;

import it.polimi.ingsw.common.transport.AbstractChannel;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.common.transport.Envelope;
import it.polimi.ingsw.common.transport.Liveness;
import it.polimi.ingsw.common.transport.TransportException;

import java.io.Serializable;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * A channel that is itself a remote object, and holds a stub for the other end.
 *
 * <p>Two decisions here are worth explaining, because both are places where the obvious
 * implementation behaves differently from the socket one — and requirement S5 says the two
 * transports must be interchangeable.
 *
 * <p><b>Sending does not wait for the other end to finish with it.</b> A remote call blocks
 * until the far side returns, so a straightforward {@code transmit} would leave the sender
 * waiting while the receiver's handler ran. A socket does not do that: it returns once the
 * bytes are flushed. So sends go through one thread per channel, which returns immediately,
 * keeps them in order, and stops a slow handler at one end from holding up the other.
 *
 * <p><b>That one thread is also what keeps delivery ordered at the far end.</b> RMI serves
 * incoming calls from a pool and promises nothing about the order of concurrent ones. Because
 * every envelope on this connection is sent from a single thread, and each call blocks until
 * it returns, the far side's {@code accept} is never called twice at once — so the ordering
 * the protocol relies on comes from the sender rather than from a guarantee RMI does not make.
 *
 * @param <O> what this side sends
 * @param <I> what this side receives
 */
public final class RmiChannel<O extends Serializable, I extends Serializable>
        extends AbstractChannel<O, I> implements RemoteEndpoint {

    private final ExecutorService sending;
    private volatile RemoteEndpoint peer;
    private volatile boolean exported;

    private RmiChannel(Class<I> expected, Liveness liveness) {
        super(expected, liveness);
        this.sending = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "rmi-sender");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Builds a channel and exports it, so the other end has something to call.
     *
     * <p>The listener is built from the channel rather than passed in, because a server
     * accepting a connection wants a listener that can answer on it.
     *
     * @param received what this side receives
     * @param liveness how hard to try to notice the other end going away
     * @param listener how to build the listener, given the channel
     * @param <O>      what this side sends
     * @param <I>      what this side receives
     * @return the channel, exported and ready to receive but not yet able to send
     * @throws TransportException if it cannot be exported
     */
    static <O extends Serializable, I extends Serializable> RmiChannel<O, I> exported(
            Class<I> received, Liveness liveness,
            java.util.function.Function<Channel<O, I>, ChannelListener<I>> listener) {

        RmiChannel<O, I> channel = new RmiChannel<>(received, liveness);
        try {
            UnicastRemoteObject.exportObject(channel, 0);
        } catch (RemoteException problem) {
            throw new TransportException("could not export an endpoint", problem);
        }
        channel.exported = true;
        channel.listenWith(listener.apply(channel));
        return channel;
    }

    /**
     * Points this channel at the other end and starts it.
     *
     * <p>Separate from building it because the two ends have to be exported before they can
     * be introduced, and something has to happen first. Nothing can be sent before this is
     * called — the heartbeat has not started either — so there is no window in which a
     * message could be sent to nobody.
     *
     * @param peer where to send
     */
    void attachTo(RemoteEndpoint peer) {
        this.peer = peer;
        start();
    }

    @Override
    public void accept(Envelope envelope) {
        deliver(envelope);
    }

    @Override
    protected void transmit(Envelope envelope) {
        RemoteEndpoint destination = peer;
        if (destination == null) {
            throw new IllegalStateException("this channel has not been introduced to anybody yet");
        }
        try {
            sending.execute(() -> push(destination, envelope));
        } catch (RejectedExecutionException closing) {
            // The channel is shutting down and this envelope is not going anywhere. That is
            // the same thing a socket does with a write after close.
        }
    }

    @Override
    protected void release() {
        // shutdown, not shutdownNow: the goodbye was queued a moment ago and dropping it
        // would put back the very delay it exists to remove. The thread is a daemon, so
        // letting it drain costs nothing and blocks nobody.
        sending.shutdown();
        if (!exported) {
            return;
        }
        exported = false;
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException alreadyGone) {
            // Nothing to withdraw. Fine.
        }
    }

    private void push(RemoteEndpoint destination, Envelope envelope) {
        try {
            destination.accept(envelope);
        } catch (RemoteException gone) {
            // The far side is unreachable. Reported the same way a broken socket is, because
            // to everything above this they are the same event.
            failed("the other end is unreachable: " + gone.getMessage());
        }
    }
}
