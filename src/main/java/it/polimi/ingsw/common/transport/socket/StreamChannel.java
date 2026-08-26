package it.polimi.ingsw.common.transport.socket;

import it.polimi.ingsw.common.transport.AbstractChannel;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.common.transport.Envelope;
import it.polimi.ingsw.common.transport.Liveness;
import it.polimi.ingsw.common.transport.TransportException;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Function;

/**
 * A channel made of a socket and a pair of object streams.
 *
 * <p>Three things about Java object streams have to be got right here, and none of them is
 * obvious from the outside. Getting any of them wrong produces a bug that only appears
 * during a long game.
 *
 * <p><b>The output stream is built first, and flushed.</b> Constructing an
 * {@link ObjectInputStream} blocks until it has read the header the other end's
 * {@link ObjectOutputStream} writes. Two ends that both build their input stream first wait
 * for each other for ever, and the failure looks like a hung connection rather than a
 * mistake in a constructor.
 *
 * <p><b>The stream is reset after every write.</b> An {@code ObjectOutputStream} remembers
 * every object it has ever written, so that a repeated reference can be sent as a
 * back-reference. That is useful for object graphs and ruinous here: this connection sends a
 * whole game state after every command, and without a reset the sender holds on to all of
 * them until somebody runs out of memory. It also means a second write of a mutated object
 * sends nothing — harmless with immutable records, and not a property worth relying on.
 *
 * <p><b>Writes are serialised.</b> The heartbeat runs on a timer thread and events are sent
 * from the game's thread, so two envelopes can be written at once, and a half-written object
 * is a stream that never recovers.
 *
 * @param <O> what this side sends
 * @param <I> what this side receives
 */
public final class StreamChannel<O extends Serializable, I extends Serializable>
        extends AbstractChannel<O, I> {

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Object writing = new Object();
    private Thread reader;

    private StreamChannel(Socket socket, ObjectOutputStream out, ObjectInputStream in,
                          ChannelListener<I> listener, Class<I> expected, Liveness liveness) {
        super(listener, expected, liveness);
        this.socket = socket;
        this.out = out;
        this.in = in;
    }

    /**
     * Wraps an open socket.
     *
     * <p>The listener is built from the channel rather than passed in, because a server
     * accepting a connection usually wants a listener that can answer on it. Nothing is read
     * until the listener exists, so there is no window in which a message could arrive with
     * nowhere to go.
     *
     * @param socket   an open, connected socket; this channel owns it and will close it
     * @param sent     what this side sends
     * @param received what this side receives
     * @param listener how to build the listener, given the channel
     * @param liveness how hard to try to notice the other end going away
     * @param <O>      what this side sends
     * @param <I>      what this side receives
     * @return the channel, already reading
     * @throws TransportException if the streams cannot be built over the socket
     */
    public static <O extends Serializable, I extends Serializable> Channel<O, I> over(
            Socket socket, Class<O> sent, Class<I> received,
            Function<Channel<O, I>, ChannelListener<I>> listener, Liveness liveness) {

        ObjectOutputStream out;
        ObjectInputStream in;
        try {
            socket.setTcpNoDelay(true);
            // Output first, and flushed: the other end's input stream is blocked on this
            // header, and if both ends waited to read one nobody would ever send one.
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException problem) {
            closeQuietly(socket);
            throw new TransportException("could not open a channel over " + socket, problem);
        }

        Deferred<I> deferred = new Deferred<>();
        StreamChannel<O, I> channel =
                new StreamChannel<>(socket, out, in, deferred, received, liveness);
        deferred.to(listener.apply(channel));

        channel.reader = new Thread(channel::read, "channel-reader-" + socket.getPort());
        channel.reader.setDaemon(true);
        channel.reader.start();
        channel.start();
        return channel;
    }

    @Override
    protected void transmit(Envelope envelope) throws IOException {
        synchronized (writing) {
            out.writeObject(envelope);
            out.flush();
            // Forget what was just written. Without this the stream keeps every game state
            // it has ever sent, which for a long game is the whole game.
            out.reset();
        }
    }

    @Override
    protected void release() {
        closeQuietly(socket);
        if (reader != null) {
            // The read is blocked on a socket that is now closed, so it is about to throw.
            // Interrupting as well costs nothing and covers the case where it is not.
            reader.interrupt();
        }
    }

    private void read() {
        try {
            while (isOpen()) {
                deliver((Envelope) in.readObject());
            }
        } catch (EOFException ended) {
            failed("the other end hung up");
        } catch (IOException | ClassNotFoundException problem) {
            failed(isOpen() ? "the connection failed: " + problem : "closed");
        } catch (ClassCastException wrong) {
            failed("something that is not an envelope arrived");
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Already going away. There is nothing useful to do with this.
        }
    }

    /**
     * A listener that is chosen a moment after the channel is built.
     *
     * <p>Exists only to close the loop between a channel and a listener that wants to answer
     * on it. Set once, before anything is read, so nothing is ever dropped or queued.
     *
     * @param <I> what this end receives
     */
    private static final class Deferred<I> implements ChannelListener<I> {

        private volatile ChannelListener<I> real;

        void to(ChannelListener<I> listener) {
            if (listener == null) {
                throw new NullPointerException("a channel needs a listener");
            }
            this.real = listener;
        }

        @Override
        public void received(I message) {
            real.received(message);
        }

        @Override
        public void closed(String reason) {
            ChannelListener<I> listener = real;
            if (listener != null) {
                listener.closed(reason);
            }
        }
    }
}
