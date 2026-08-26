/**
 * The ship: a grid of welded components, and everything that can be asked of it.
 *
 * <p>{@link it.polimi.ingsw.server.model.ship.Ship} is the deep module of the model. It
 * hides the grid, the connector matrix and the connectivity search behind questions worth
 * asking — what is this ship's firepower, what does that shot hit, is it still in one
 * piece — so that no card and no controller ever walks the grid itself.
 *
 * <p>The value types those answers are made of live in
 * {@link it.polimi.ingsw.common.game}, because a client needs them to draw the ship and to
 * say what it wants done to it. What stays here is the part that decides.
 */
package it.polimi.ingsw.server.model.ship;
