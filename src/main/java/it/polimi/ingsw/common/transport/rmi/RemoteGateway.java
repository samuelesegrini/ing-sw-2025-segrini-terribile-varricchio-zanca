package it.polimi.ingsw.common.transport.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * What a client looks up in the registry: a way to swap endpoints with the server.
 *
 * <p>RMI gives one direction for free — a client holds a stub and calls it. The other
 * direction has to be arranged, and this is the arrangement: the client offers its own
 * endpoint and is handed the server's, after which the two are symmetric and neither is
 * more of a client than the other.
 */
public interface RemoteGateway extends Remote {

    /** What the gateway is bound as. Both sides agree on this and nothing else. */
    String NAME = "galaxy-trucker";

    /**
     * Introduces a client to the server.
     *
     * @param client where the server should send events
     * @return where the client should send commands
     * @throws RemoteException if the server cannot be reached, or refuses
     */
    RemoteEndpoint connect(RemoteEndpoint client) throws RemoteException;
}
