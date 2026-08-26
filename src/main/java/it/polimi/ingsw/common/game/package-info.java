/**
 * The shared vocabulary of the game: its names, its geometry, the values built out of
 * those, and the questions the rules ask.
 *
 * <p>Everything here is needed by both sides. A placement command carries a
 * {@link it.polimi.ingsw.common.game.Position} and a
 * {@link it.polimi.ingsw.common.game.Rotation}; a ship view is keyed by position; a
 * defence prompt names a {@link it.polimi.ingsw.common.game.HitKind}; a whole flight is
 * nothing but {@link it.polimi.ingsw.common.game.PlayerPrompt}s and the
 * {@link it.polimi.ingsw.common.game.PlayerChoice}s that answer them. Putting these words
 * below both sides is what lets the dependency arrow run {@code server → common} and
 * {@code client → common}, and never between the two.
 *
 * <p>This package holds no game <b>state</b> and no <b>authority</b>. The predicates that
 * live here — whether two connectors join, whether a shield stops a hit, which hold takes
 * red — exist so that a view can grey out an illegal placement before sending it. They
 * decide nothing. The server revalidates every command as though the client had computed
 * nothing at all, and a client that lies about geometry gets the same answer as one that
 * does not.
 *
 * <p>Nor does a value here confer any right. A
 * {@link it.polimi.ingsw.common.game.PlayerChoice} names the player it came from, which
 * over a socket is a claim rather than a fact; the server checks it against the session
 * that sent it before the rules ever see it.
 */
package it.polimi.ingsw.common.game;
