package it.polimi.ingsw.common.transport;

/**
 * What to do with what arrives.
 *
 * <p>Supplied when a channel is opened, because a channel that could be listened to later
 * would have to buffer whatever arrived in the meantime, and a channel that buffers is a
 * channel with a memory leak in it.
 *
 * @param <I> what this side receives: commands on the server, events on the client
 */
public interface ChannelListener<I> {

    /**
     * Called when a message arrives.
     *
     * <p>Called on the channel's own thread, so an implementation that takes its time holds
     * up everything else arriving on that connection. Hand the work somewhere else if it is
     * not quick.
     *
     * @param message what arrived
     */
    void received(I message);

    /**
     * Called once, when the channel closes for any reason.
     *
     * <p>Deliberately does not distinguish a clean goodbye from a dropped connection. From
     * the other end they are the same event, and code that treats them differently is code
     * that behaves differently depending on how politely a player's laptop ran out of
     * battery.
     *
     * @param reason why, in a sentence, for the log
     */
    void closed(String reason);
}
