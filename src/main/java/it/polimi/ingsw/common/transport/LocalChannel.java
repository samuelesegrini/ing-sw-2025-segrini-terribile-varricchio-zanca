package it.polimi.ingsw.common.transport;

import java.io.Serializable;

/**
 * A pair of channels wired to each other inside one process.
 *
 * <p>Not a third transport. It exists so that the controller can be driven without a
 * network at all — hand it commands through one end, assert on the events that come out of
 * the other — which is the testability architecture § 3.2 claimed for the command-and-event
 * design and which would otherwise be a claim nobody checked.
 *
 * <p>It is also what lets the conformance suite in {@code ChannelContract} exist before
 * either real transport does. A suite written against the first implementation tends to
 * describe that implementation; written against something with no I/O in it at all, it can
 * only describe the interface.
 *
 * <p>Delivery is direct: sending calls the other end's listener on the calling thread. That
 * is not what a socket does, and the suite is written not to depend on it.
 *
 * @param <O> what the near end sends
 * @param <I> what the near end receives
 */
public final class LocalChannel<O extends Serializable, I extends Serializable>
        extends AbstractChannel<O, I> {

    private LocalChannel<I, O> peer;

    private LocalChannel(ChannelListener<I> listener, Class<I> expected) {
        super(listener, expected);
    }

    /**
     * Two ends of one connection.
     *
     * @param near what the caller holds
     * @param far  what the other side holds
     * @param <O>  what the near end sends
     * @param <I>  what the near end receives
     */
    public record Pair<O extends Serializable, I extends Serializable>(
            Channel<O, I> near, Channel<I, O> far) {
    }

    /**
     * Wires two channels together.
     *
     * <p>The class tokens are the same ones any transport needs: they let a message of the
     * wrong kind be reported as a protocol error rather than surface as a class cast
     * somewhere inside a handler.
     *
     * @param sent          what the near end sends and the far end receives
     * @param received      what the near end receives and the far end sends
     * @param nearListener  what the near end does with what arrives
     * @param farListener   what the far end does with what arrives
     * @param <O>           what the near end sends
     * @param <I>           what the near end receives
     * @return the two ends, already connected
     * @throws NullPointerException if any argument is {@code null}
     */
    public static <O extends Serializable, I extends Serializable> Pair<O, I> connect(
            Class<O> sent, Class<I> received,
            ChannelListener<I> nearListener, ChannelListener<O> farListener) {

        LocalChannel<O, I> near = new LocalChannel<>(nearListener, received);
        LocalChannel<I, O> far = new LocalChannel<>(farListener, sent);
        near.peer = far;
        far.peer = near;
        return new Pair<>(near, far);
    }

    @Override
    protected void transmit(Envelope envelope) {
        peer.deliver(envelope);
    }

    @Override
    protected void release() {
        // Closing one end closes the other, which is the one thing an in-process pair has to
        // get right: a test that closed the client and then waited for the server to notice
        // would wait for ever.
        if (peer.isOpen()) {
            peer.failed("the other end closed");
        }
    }
}
