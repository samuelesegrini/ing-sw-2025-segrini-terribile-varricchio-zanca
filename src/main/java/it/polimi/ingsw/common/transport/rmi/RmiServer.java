package it.polimi.ingsw.common.transport.rmi;

import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.transport.AbstractListeningPost;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.common.transport.Doorman;
import it.polimi.ingsw.common.transport.Liveness;
import it.polimi.ingsw.common.transport.TransportException;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * A registry with one thing in it, and a channel for everybody who looks it up.
 *
 * <p>The same shape as the socket door, and now the same type: both are
 * {@link it.polimi.ingsw.common.transport.Doorway Doorways} over
 * {@link AbstractListeningPost}, so a server holding one of each holds them identically. That
 * used to be a promise made in this comment, which is the sort of promise nothing checks —
 * and the point of requirement S5 is that the two are not special cases of each other.
 */
public final class RmiServer extends AbstractListeningPost {

    private final Registry registry;
    private final Gateway gateway;
    private final int port;

    private RmiServer(Registry registry, Gateway gateway, int port,
                      Doorman doorman, Liveness liveness) {
        super(doorman, liveness);
        this.registry = registry;
        this.gateway = gateway;
        this.port = port;
    }

    /**
     * Starts a registry and offers a gateway through it.
     *
     * @param port      the port for the registry, or zero to be given a free one
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
     * @throws TransportException if the registry cannot be created or the gateway bound
     */
    public static RmiServer listening(int port, Doorman doorman, Liveness liveness) {
        int chosen = port == 0 ? freePort() : port;
        // The gateway needs somewhere to send a connection before there is a server to send it
        // to, so it is given the door's own welcome once the door exists. It used to be handed
        // the connection set the same way — a non-final field, null for a moment — which is
        // the wart that having a base class to put the set in removes.
        Gateway gateway = new Gateway(liveness);
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(chosen);
            UnicastRemoteObject.exportObject(gateway, 0);
            registry.rebind(RemoteGateway.NAME, gateway);
        } catch (RemoteException problem) {
            throw new TransportException("could not start an RMI registry on port " + chosen, problem);
        }
        RmiServer server = new RmiServer(registry, gateway, chosen, doorman, liveness);
        gateway.welcome = server::welcome;
        return server;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    protected void stopListening() {
        try {
            registry.unbind(RemoteGateway.NAME);
        } catch (Exception ignored) {
            // Shutting down, and an unbound name is the state we wanted anyway.
        }
        unexport(gateway);
        unexport(registry);
    }

    private static void unexport(java.rmi.Remote remote) {
        try {
            UnicastRemoteObject.unexportObject(remote, true);
        } catch (NoSuchObjectException alreadyGone) {
            // Nothing to withdraw.
        }
    }

    /**
     * Finds a port nobody is using.
     *
     * <p>{@code createRegistry(0)} does export on a free port, but on one the client has no
     * way to look up — so a registry on port zero is a registry nobody can find. Asking the
     * operating system for a port and then using it is the workaround, and it is what lets a
     * test run without picking a number and hoping.
     */
    private static int freePort() {
        try (ServerSocket probe = new ServerSocket(0)) {
            return probe.getLocalPort();
        } catch (IOException problem) {
            throw new TransportException("could not find a free port", problem);
        }
    }

    /**
     * The one object in the registry: it swaps endpoints and nothing else.
     */
    private static final class Gateway implements RemoteGateway {

        private final Liveness liveness;

        /** The door's own welcome, set once the door exists. */
        private Doorman welcome;

        Gateway(Liveness liveness) {
            this.liveness = liveness;
        }

        @Override
        public RemoteEndpoint connect(RemoteEndpoint client) {
            if (client == null) {
                throw new NullPointerException("a client has to say where to send its events");
            }
            RmiChannel<Event, Command> channel =
                    RmiChannel.exported(Command.class, liveness, welcome::answer);
            channel.attachTo(client);
            return channel;
        }
    }
}
