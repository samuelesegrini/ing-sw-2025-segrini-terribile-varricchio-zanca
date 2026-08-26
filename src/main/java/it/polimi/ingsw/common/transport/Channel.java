package it.polimi.ingsw.common.transport;

import java.io.Serializable;

/**
 * A two-way connection to the other side, whatever it is made of.
 *
 * <p>The point of this interface is what it does <em>not</em> say. Nothing here mentions
 * sockets, streams, registries or stubs, so the controller that sends events through it
 * cannot find out which transport a player is using — which is what requirement S5 asks for:
 * one game, some players on sockets, some on RMI, none of them special.
 *
 * <p><b>Sending to a closed channel does nothing.</b> It is not an error and it does not
 * throw. A player whose connection dropped is a normal event in this game and the flight
 * carries on without them; a server that threw on every event sent to a departed player
 * would need a null check at every call site, and one of them would be missing.
 *
 * @param <O> what this side sends: events on the server, commands on the client
 * @param <I> what this side receives
 */
public interface Channel<O extends Serializable, I extends Serializable> extends AutoCloseable {

    /**
     * Sends a message, if there is still anywhere to send it.
     *
     * <p>Never throws. If the connection has gone, this does nothing and the listener has
     * already been told — or is about to be.
     *
     * @param message what to send
     */
    void send(O message);

    /**
     * Tells whether this channel still works.
     *
     * <p>Only ever a statement about the past: a channel can drop between this returning
     * {@code true} and the next send. Useful for reporting, not for deciding.
     *
     * @return {@code true} while the connection is up
     */
    boolean isOpen();

    /**
     * Closes the connection and tells the listener once.
     *
     * <p>Doing this twice is harmless, which matters because the usual way a channel closes
     * is that both ends decide to close it at the same moment.
     */
    @Override
    void close();
}
