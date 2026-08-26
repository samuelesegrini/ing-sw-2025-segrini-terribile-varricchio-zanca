package it.polimi.ingsw.common.transport.rmi;

import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.common.transport.Liveness;
import it.polimi.ingsw.common.transport.TransportException;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * The client end: looks up the gateway, swaps endpoints, and returns a channel.
 *
 * <p>The same one method {@code SocketConnector} has, taking the same arguments and giving
 * back the same kind of thing. A client that wants to offer both transports should be able to
 * choose between them with an {@code if} and no other difference.
 */
public final class RmiConnector {

    private RmiConnector() {
    }

    /**
     * Connects to a server.
     *
     * @param host     where the registry is
     * @param port     what port it is on
     * @param listener what to do with the events that come back
     * @param liveness how hard to try to notice the server going away
     * @return a channel to the server
     * @throws TransportException if there is no registry there, or nothing bound in it
     */
    public static Channel<Command, Event> connect(String host, int port,
                                                  ChannelListener<Event> listener,
                                                  Liveness liveness) {
        if (listener == null || liveness == null) {
            throw new NullPointerException("connecting needs a listener and liveness");
        }
        RmiChannel<Command, Event> channel =
                RmiChannel.exported(Event.class, liveness, ignored -> listener);
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            RemoteGateway gateway = (RemoteGateway) registry.lookup(RemoteGateway.NAME);
            channel.attachTo(gateway.connect(channel));
            return channel;
        } catch (RemoteException | NotBoundException problem) {
            // Withdraw the endpoint that nobody is going to call, or it stays exported and
            // keeps the JVM's RMI machinery alive for a connection that never happened.
            channel.close();
            throw new TransportException(
                    "could not reach a server at " + host + ":" + port, problem);
        }
    }
}
