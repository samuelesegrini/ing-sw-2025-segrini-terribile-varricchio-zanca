/**
 * Dialling a server, and forgetting how.
 *
 * <p>One {@code switch} over two cases, in a constructor, and after it nothing in the client
 * can tell a socket from a registry. Requirement C4 asks for the transport to be chosen at
 * startup; this is where that choice is made, and it is the last place that knows about it.
 */
package it.polimi.ingsw.client.network;
