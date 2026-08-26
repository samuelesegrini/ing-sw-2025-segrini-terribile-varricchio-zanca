/**
 * The RMI transport: the same messages, over a registry.
 *
 * <p>RMI is used to <em>move envelopes</em>, not to expose the model. There is no
 * {@code drawTile()} and no {@code declareFirepower()}: a remote interface shaped like the
 * game would be a second protocol agreeing with the first only by hand, and every card added
 * would have to be added to both.
 *
 * <p>RMI gives one direction for free — a client holds a stub and calls it. The other has to
 * be arranged, which is what {@link it.polimi.ingsw.common.transport.rmi.RemoteGateway} is
 * for: the client offers its own endpoint and is handed the server's, after which the two are
 * symmetric and neither is more of a client than the other.
 *
 * <p>Two behaviours are matched to the socket transport on purpose, because requirement S5
 * says the two have to be interchangeable and a difference in timing is still a difference.
 * Sends do not block until the far side has finished with the message, and envelopes on one
 * connection are sent from a single thread — which is also what makes delivery ordered, since
 * RMI promises nothing about the order of concurrent calls.
 */
package it.polimi.ingsw.common.transport.rmi;
