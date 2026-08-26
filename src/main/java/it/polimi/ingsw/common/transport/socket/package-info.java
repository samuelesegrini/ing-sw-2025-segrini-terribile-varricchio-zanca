/**
 * The socket transport: a server that listens, a connector that dials, and a channel made of
 * object streams.
 *
 * <p>It lives under {@code common} rather than being written once on each side because it is
 * the same code both ways round. A channel reads envelopes and writes envelopes; which of
 * commands and events it sends is a type parameter, not a different implementation.
 *
 * <p>What is <em>not</em> here is anything that knows about the game. Turning an accepted
 * connection into a player's session belongs on the server, and dispatching events into a
 * local view belongs on the client; both take a channel from here and neither is visible to
 * the other.
 */
package it.polimi.ingsw.common.transport.socket;
