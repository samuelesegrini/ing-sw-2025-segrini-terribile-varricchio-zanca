package it.polimi.ingsw.client.network;

import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.common.transport.Liveness;
import it.polimi.ingsw.common.transport.TransportException;
import it.polimi.ingsw.common.transport.rmi.RmiConnector;
import it.polimi.ingsw.common.transport.socket.SocketConnector;

/**
 * The client's end of the wire, and the last place that knows what it is made of.
 *
 * <p>One {@code switch} over two cases, in a constructor, and after it nothing in the client
 * can tell a socket from a registry. That is requirement C4 satisfied by having somewhere for
 * the choice to be made once rather than by threading a flag through everything.
 *
 * <p>Everything that arrives goes into a {@link ClientState}. A screen reads the state; it
 * never listens to the wire.
 */
public final class ServerLink implements AutoCloseable {

    private final Channel<Command, Event> channel;

    private ServerLink(Channel<Command, Event> channel) {
        this.channel = channel;
    }

    /**
     * Connects to a server.
     *
     * @param transport which way to talk to it
     * @param host      where it is
     * @param port      the socket port, or the registry port
     * @param state     where everything that arrives should go
     * @return the connection
     * @throws TransportException if there is nothing there to connect to
     */
    public static ServerLink connect(Transport transport, String host, int port,
                                     ClientState state) {
        ChannelListener<Event> listener = new ChannelListener<>() {
            @Override
            public void received(Event event) {
                state.apply(event);
            }

            @Override
            public void closed(String reason) {
                state.disconnected(reason);
            }
        };
        Channel<Command, Event> channel = switch (transport) {
            case SOCKET -> SocketConnector.connect(host, port, listener, Liveness.DEFAULT);
            case RMI -> RmiConnector.connect(host, port, listener, Liveness.DEFAULT);
        };
        return new ServerLink(channel);
    }

    /**
     * Sends a command.
     *
     * <p>Does nothing if the connection has gone, which is the same thing the transport does
     * and for the same reason: a client that threw every time it typed into a dead connection
     * would be harder to use than one that quietly told you it had dropped.
     *
     * @param command what to send
     */
    public void send(Command command) {
        channel.send(command);
    }

    /**
     * Tells whether the connection is still up.
     *
     * @return {@code true} while it is
     */
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void close() {
        channel.close();
    }
}
