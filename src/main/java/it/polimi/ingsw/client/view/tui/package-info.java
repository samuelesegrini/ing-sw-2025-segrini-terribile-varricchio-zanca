/**
 * The text interface, and the renderers underneath it.
 *
 * <p>The renderers are pure functions of the protocol's projections: hand one a
 * {@link it.polimi.ingsw.common.protocol.view.ShipView} and it returns the lines to print.
 * Nothing in them reads a clock, a terminal or a connection, which is what makes them ordinary
 * things to test — a renderer that needed a running game to exercise would be a renderer nobody
 * exercised.
 *
 * <p>What is left over is the loop: read a line, work out what it means <em>in the phase the
 * game is actually in</em>, and send a command. That part is thin on purpose.
 */
package it.polimi.ingsw.client.view.tui;
