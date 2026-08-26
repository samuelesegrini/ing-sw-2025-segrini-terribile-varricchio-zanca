package it.polimi.ingsw.common.transport.rmi;

import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.common.transport.Liveness;
import it.polimi.ingsw.common.transport.TransportException;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A registry with one thing in it, and a channel for everybody who looks it up.
 *
 * <p>The same shape as {@code SocketServer} on purpose: bind, hand each connection to a
 * function, keep them so that closing the server closes them too. Whoever calls it should not
 * have to hold the two differently, since the point of requirement S5 is that they can be
 * used together without either being a special case.
 */
public final class RmiServer implements AutoCloseable {

    private final Registry registry;
    private final Gateway gateway;
    private final int port;
    private final Set<Channel<Event, Command>> connected = ConcurrentHashMap.newKeySet();

    private RmiServer(Registry registry, Gateway gateway, int port) {
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
     * @param onConnect what to do with each new connection, given the channel to answer on
     * @param liveness  how hard each connection should try to notice a client going away
     * @return the running server
     * @throws TransportException if the registry cannot be created or the gateway bound
     */
    public static RmiServer listening(int port,
                                      Function<Channel<Event, Command>, ChannelListener<Command>> onConnect,
                                      Liveness liveness) {
        if (onConnect == null || liveness == null) {
            throw new NullPointerException("a server needs to know what to do with a connection");
        }
        int chosen = port == 0 ? freePort() : port;
        Registry registry;
        Gateway gateway;
        try {
            registry = LocateRegistry.createRegistry(chosen);
            gateway = new Gateway(onConnect, liveness);
            UnicastRemoteObject.exportObject(gateway, 0);
            registry.rebind(RemoteGateway.NAME, gateway);
        } catch (RemoteException problem) {
            throw new TransportException("could not start an RMI registry on port " + chosen, problem);
        }
        RmiServer server = new RmiServer(registry, gateway, chosen);
        gateway.connected = server.connected;
        return server;
    }

    /**
     * Returns the port the registry is on.
     *
     * @return the bound port
     */
    public int port() {
        return port;
    }

    /**
     * Returns how many clients are connected.
     *
     * @return the number of open channels
     */
    public int connectionCount() {
        connected.removeIf(channel -> !channel.isOpen());
        return connected.size();
    }

    @Override
    public void close() {
        connected.forEach(Channel::close);
        connected.clear();
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

        private final Function<Channel<Event, Command>, ChannelListener<Command>> onConnect;
        private final Liveness liveness;

        private Set<Channel<Event, Command>> connected;

        Gateway(Function<Channel<Event, Command>, ChannelListener<Command>> onConnect,
                Liveness liveness) {
            this.onConnect = onConnect;
            this.liveness = liveness;
        }

        @Override
        public RemoteEndpoint connect(RemoteEndpoint client) {
            if (client == null) {
                throw new NullPointerException("a client has to say where to send its events");
            }
            RmiChannel<Event, Command> channel = RmiChannel.exported(
                    Command.class, liveness, accepted -> {
                        // Registered before the handler sees it, for the same reason the
                        // socket server does it: a handler may start using the channel at
                        // once, and a server that counted the connection afterwards would
                        // spend that window claiming to have none.
                        connected.add(accepted);
                        return onConnect.apply(accepted);
                    });
            channel.attachTo(client);
            return channel;
        }
    }
}
