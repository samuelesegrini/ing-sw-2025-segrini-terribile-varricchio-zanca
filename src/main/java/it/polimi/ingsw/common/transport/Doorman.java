package it.polimi.ingsw.common.transport;

import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;

/**
 * What to do with a connection that has just arrived.
 *
 * <p>Written out six times as {@code Function<Channel<Event, Command>, ChannelListener<Command>>}
 * before it had a name, which is long enough that the three files carrying it read as though
 * they were doing three different things. They were doing this one.
 *
 * <p><b>It runs on a transport thread and must return promptly.</b> On RMI it runs inside the
 * connecting client's own call, so a doorman that waited for something else to happen would
 * leave that client waiting for it; on a socket it runs on the thread that accepts, so the same
 * doorman would stop anybody else connecting. Register the session and return.
 */
@FunctionalInterface
public interface Doorman {

    /**
     * Takes charge of a new connection.
     *
     * @param connection the channel to answer on
     * @return what to do with what arrives on it
     */
    ChannelListener<Command> answer(Channel<Event, Command> connection);
}
