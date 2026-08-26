package it.polimi.ingsw.common.transport.socket;

import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.common.transport.Liveness;
import it.polimi.ingsw.common.transport.TransportException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * The client end: opens a socket to a server and returns a channel over it.
 *
 * <p>One method, because that is all a client needs. Reconnecting is not handled here — it is
 * connecting again, and the protocol makes the two the same thing on purpose.
 */
public final class SocketConnector {

    /** How long to wait for a server that may not be there. */
    private static final int CONNECT_TIMEOUT_MS = 5_000;

    private SocketConnector() {
    }

    /**
     * Connects to a server.
     *
     * @param host     where the server is
     * @param port     what it is listening on
     * @param listener what to do with the events that come back
     * @param liveness how hard to try to notice the server going away
     * @return a channel to the server
     * @throws TransportException if there is nothing there to connect to
     */
    public static Channel<Command, Event> connect(String host, int port,
                                                  ChannelListener<Event> listener,
                                                  Liveness liveness) {
        if (listener == null || liveness == null) {
            throw new NullPointerException("connecting needs a listener and liveness");
        }
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        } catch (IOException problem) {
            throw new TransportException("could not reach a server at " + host + ":" + port, problem);
        }
        return StreamChannel.over(socket, Command.class, Event.class, channel -> listener, liveness);
    }
}
