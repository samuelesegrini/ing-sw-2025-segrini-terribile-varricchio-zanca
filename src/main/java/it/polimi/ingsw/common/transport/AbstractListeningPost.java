package it.polimi.ingsw.common.transport;

import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Everything a door does that has nothing to do with how clients reach it.
 *
 * <p>The same arrangement {@link AbstractChannel} uses one layer down, and for the same
 * reason. Both doors have to hold on to what has connected so that closing the server closes
 * them too, prune what has since dropped before answering how many there are, and register a
 * channel <em>before</em> the doorman sees it. Written twice, those are two chances to get the
 * registration order wrong — and the socket server carried a four-line comment explaining that
 * order, which the RMI gateway then repeated.
 *
 * <p>So a door implements one method, {@link #stopListening}, and calls one, {@link #welcome}.
 * Everything else is here.
 *
 * <p><b>On threads.</b> {@link #welcome} is called from whatever thread the transport accepts
 * on; the set is concurrent because {@link #connectionCount} and {@link #close} are called
 * from elsewhere.
 */
public abstract class AbstractListeningPost implements Doorway {

    private final Set<Channel<Event, Command>> connected = ConcurrentHashMap.newKeySet();
    private final Doorman doorman;
    private final Liveness liveness;

    /**
     * Builds a door that is not open yet.
     *
     * @param doorman  what to do with each new connection
     * @param liveness how hard each connection should try to notice a client going away
     * @throws NullPointerException if either is {@code null}
     */
    protected AbstractListeningPost(Doorman doorman, Liveness liveness) {
        if (doorman == null || liveness == null) {
            throw new NullPointerException("a door needs to know what to do with a connection");
        }
        this.doorman = doorman;
        this.liveness = liveness;
    }

    /**
     * Returns how hard connections through this door should watch for silence.
     *
     * @return the liveness settings this door was opened with
     */
    protected final Liveness liveness() {
        return liveness;
    }

    /**
     * Takes charge of a channel that has just connected.
     *
     * <p>Registered before the doorman sees it, not after. A doorman is entitled to start
     * using the channel immediately — and to hand it to something else that does — so a door
     * that only counted the connection once the doorman returned would spend that whole window
     * claiming to have none.
     *
     * @param channel the new connection
     * @return what the doorman wants done with what arrives on it
     */
    protected final ChannelListener<Command> welcome(Channel<Event, Command> channel) {
        connected.add(channel);
        return doorman.answer(channel);
    }

    /**
     * Lets go of the connections that have since dropped.
     *
     * <p>For a door to call while it is accepting. Nothing depends on it having been called —
     * {@link #connectionCount} does it too — but a door that never did it would hold every
     * channel that had ever connected to it, and a long-running server is exactly where that
     * adds up.
     */
    protected final void forgetClosed() {
        connected.removeIf(channel -> !channel.isOpen());
    }

    @Override
    public final int connectionCount() {
        forgetClosed();
        return connected.size();
    }

    @Override
    public final void close() {
        connected.forEach(Channel::close);
        connected.clear();
        stopListening();
    }

    /**
     * Stops accepting, and lets go of whatever the door was made of.
     *
     * <p>Called once, by {@link #close}, after every connection through this door has already
     * been closed.
     */
    protected abstract void stopListening();
}
