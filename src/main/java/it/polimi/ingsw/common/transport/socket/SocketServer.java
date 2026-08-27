package it.polimi.ingsw.common.transport.socket;

import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.transport.AbstractListeningPost;
import it.polimi.ingsw.common.transport.Doorman;
import it.polimi.ingsw.common.transport.Liveness;
import it.polimi.ingsw.common.transport.TransportException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Listens on a port and turns everything that connects into a channel.
 *
 * <p>Deliberately knows nothing about games, players or nicknames. It takes a function that
 * says what to do with a new connection and calls it; whether that function starts a session,
 * refuses the connection or writes it down is somebody else's business.
 *
 * <p>Every accepted connection is kept so that closing the server closes them too — by
 * {@link AbstractListeningPost}, which is also where the counting and the closing live,
 * because the RMI door does exactly the same things with them.
 */
public final class SocketServer extends AbstractListeningPost {

    private final ServerSocket listening;
    private final Thread acceptor;

    private volatile boolean running = true;

    private SocketServer(ServerSocket listening, Doorman doorman, Liveness liveness) {
        super(doorman, liveness);
        this.listening = listening;
        this.acceptor = new Thread(this::accept, "socket-acceptor-" + listening.getLocalPort());
        this.acceptor.setDaemon(true);
    }

    /**
     * Starts listening.
     *
     * @param port      the port to bind, or zero to be given a free one
     *
     * <p>The handler runs on a transport thread and must return promptly. On RMI it runs
     * inside the connecting client's own call, so a handler that waited for something else to
     * happen would leave that client waiting for it; on a socket it runs on the thread that
     * accepts, so the same handler would stop anybody else connecting. Register the session
     * and return.
     *
     * @param doorman  what to do with each new connection, given the channel to answer on
     * @param liveness how hard each connection should try to notice a client going away
     * @return the running server
     * @throws TransportException if the port cannot be bound
     */
    public static SocketServer listening(int port, Doorman doorman, Liveness liveness) {
        ServerSocket listening;
        try {
            listening = new ServerSocket(port);
        } catch (IOException problem) {
            throw new TransportException("could not listen on port " + port, problem);
        }
        SocketServer server = new SocketServer(listening, doorman, liveness);
        server.acceptor.start();
        return server;
    }

    /**
     * Returns the port actually being listened on.
     *
     * <p>Worth having because a server started on port zero is given one, which is how a test
     * runs without picking a number and hoping.
     *
     * @return the bound port
     */
    @Override
    public int port() {
        return listening.getLocalPort();
    }

    @Override
    protected void stopListening() {
        running = false;
        try {
            listening.close();
        } catch (IOException ignored) {
            // Shutting down. Nothing useful to do with this.
        }
    }

    private void accept() {
        while (running) {
            Socket socket;
            try {
                socket = listening.accept();
            } catch (IOException stopped) {
                // Either the server was closed, in which case this is how it ends, or the
                // listening socket failed, in which case there is nothing to fall back to.
                return;
            }
            try {
                StreamChannel.over(socket, Event.class, Command.class, this::welcome, liveness());
            } catch (TransportException refused) {
                // One connection that could not be set up. The others are unaffected, and a
                // server that stopped accepting because of one bad handshake would be worse.
                continue;
            }
            forgetClosed();
        }
    }
}
