/**
 * Opening the doors: one lobby, listening on a socket and through a registry at once.
 *
 * <p>This is the whole of requirement S5, and there is almost nothing to it — both transports
 * are handed the same {@code lobby::welcome} and nothing downstream can tell which door a
 * player came through. That is not because the wiring is clever; it is because
 * {@link it.polimi.ingsw.common.transport.Channel} was written so that there would be nothing
 * left to branch on.
 */
package it.polimi.ingsw.server.network;
