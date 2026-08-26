/**
 * The protocol: a closed set of commands going one way and a closed set of events coming
 * back.
 *
 * <p>Both sides are sealed, in two layers. {@link it.polimi.ingsw.common.protocol.Command}
 * permits four families that match the phases of a game, and each family permits its own
 * records, so a phase can switch over the commands it owns and be told at compile time when
 * a new one appears.
 *
 * <p>Two rules run through everything here, and both exist to remove a class of bug rather
 * than to be tidy.
 *
 * <p><b>Nobody says who they are.</b> No command carries a nickname or a colour; the server
 * knows which session a message arrived on. The one payload that names a player —
 * {@link it.polimi.ingsw.common.game.PlayerChoice}, which predates the network — is checked
 * against the sender and refused on a mismatch.
 *
 * <p><b>Facts, then truth.</b> Every batch of events ends with the whole picture, so nothing
 * is ever expressed only as a delta and a client cannot drift out of step by missing a
 * message. Reconnecting is then the same operation as joining.
 *
 * <p>The wire carries no {@link java.util.Optional}. RMI marshals with Java serialization,
 * which cannot carry one, so a value that may be absent is {@code null} and is read through
 * an {@code …IfAny()} accessor. That rule is enforced by a test rather than remembered.
 *
 * <p>The messages are documented for a reader, rather than only for a compiler, in
 * {@code docs/protocol/README.md}.
 */
package it.polimi.ingsw.common.protocol;
