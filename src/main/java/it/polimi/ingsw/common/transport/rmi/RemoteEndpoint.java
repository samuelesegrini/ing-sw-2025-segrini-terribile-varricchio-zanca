package it.polimi.ingsw.common.transport.rmi;

import it.polimi.ingsw.common.transport.Envelope;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * One end of an RMI connection, as the other end sees it.
 *
 * <p>One method, and it takes an envelope. That is the whole of what RMI is used for here:
 * moving the same messages the socket transport moves, rather than exposing the model
 * remotely.
 *
 * <p>It is worth being explicit about what this deliberately is <em>not</em>. There is no
 * {@code drawTile()}, no {@code declareFirepower()}, no remote method per player action. A
 * remote interface shaped like the game would be a second protocol, agreeing with the first
 * only by hand, and every card added would have to be added to both — which is exactly the
 * coupling architecture § 3.2 was written to avoid.
 */
public interface RemoteEndpoint extends Remote {

    /**
     * Takes delivery of one envelope.
     *
     * @param envelope what is being sent
     * @throws RemoteException if this end has gone away, which is how the sender finds out
     */
    void accept(Envelope envelope) throws RemoteException;
}
