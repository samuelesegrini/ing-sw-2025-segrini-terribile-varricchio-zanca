/**
 * Galaxy Trucker, a distributed client-server implementation of the board game by
 * Cranio Creations, written as the final project of the Software Engineering course
 * at Politecnico di Milano.
 *
 * <p>The system is split into three top-level areas. {@code server} owns the
 * authoritative game model and the rules; {@code client} owns the two user
 * interfaces and a read-only projection of the game; {@code common} carries the
 * message catalogue the two exchange and holds no logic of its own.
 *
 * <p>Behaviour is specified in {@code docs/specs/game-rules.md} and the design is
 * described in {@code docs/architecture/overview.md}.
 */
package it.polimi.ingsw;
