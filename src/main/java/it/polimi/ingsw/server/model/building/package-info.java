/**
 * The building phase: the shared pool of tiles and each player's construction of a ship.
 *
 * <p>Building is the one phase where every player acts at once, grabbing from the same
 * heap as fast as they can. What keeps that fair here is that every action is a command
 * on one queue, so a race for the same tile resolves by arrival order rather than by
 * whoever's thread woke up first.
 */
package it.polimi.ingsw.server.model.building;
