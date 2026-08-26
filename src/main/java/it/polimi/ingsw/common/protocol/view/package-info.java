/**
 * Immutable projections of the server's model, and the only shape game state takes on the
 * wire.
 *
 * <p>The client never receives a {@code Ship}. It receives a
 * {@link it.polimi.ingsw.common.protocol.view.ShipView}: a record carrying exactly what a
 * view needs to draw, and nothing that would let a client answer a question the server is
 * supposed to answer.
 *
 * <p>Most of what a view holds is public by the manual's own rules — ships are built in
 * the open, and the route is on the table. Two things are not, and they are why a
 * {@link it.polimi.ingsw.common.protocol.view.GameView} is built for one recipient rather
 * than once for the table: the tile in a player's hand, and the card pile they are peeking
 * at.
 */
package it.polimi.ingsw.common.protocol.view;
