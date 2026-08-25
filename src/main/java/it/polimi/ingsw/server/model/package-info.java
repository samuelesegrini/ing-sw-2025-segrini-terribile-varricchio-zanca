/**
 * The game model: the state of a game and the rules that change it.
 *
 * <p>The model is the only authority on what is legal. It answers questions and
 * applies moves; it does not know about players' connections, about turns arriving
 * over a socket, or about anything being drawn on a screen.
 *
 * <p>Behaviour here is specified in {@code docs/specs/game-rules.md}, and each rule is
 * traceable to a page of the manual.
 */
package it.polimi.ingsw.server.model;
