/**
 * A two-way connection to the other side, and nothing about what it is made of.
 *
 * <p>Requirement S5 asks for one game with some players on sockets and some on RMI, and the
 * only way to get that without special cases is for the code above this package to be unable
 * to tell the difference. Nothing in {@link it.polimi.ingsw.common.transport.Channel}
 * mentions a stream, a registry or a stub.
 *
 * <p>{@link it.polimi.ingsw.common.transport.AbstractChannel} holds everything the two
 * transports share, which turns out to be most of it: wrapping and unwrapping, answering
 * keep-alives, noticing silence, closing exactly once however many threads decide to close
 * at the same moment, and doing nothing when somebody sends to a connection that has already
 * gone. A transport implements two methods and calls two.
 *
 * <p>That last behaviour is deliberate and worth stating twice. <b>Sending to a closed
 * channel is not an error.</b> A player dropping out is a normal event in this game — the
 * flight carries on without them — and a channel that threw would put a null check at every
 * call site, one of which would be missing.
 */
package it.polimi.ingsw.common.transport;
