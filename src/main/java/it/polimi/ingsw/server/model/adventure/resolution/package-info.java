/**
 * How an adventure card gets resolved.
 *
 * <p>Card logic lives with the card. A controller drives a resolution without knowing
 * which card it is: it asks what decision is outstanding, hands the answer back, and
 * repeats until nothing is pending. Adding a card means adding a file, never editing a
 * switch — which is the whole reason this is not a visitor.
 */
package it.polimi.ingsw.server.model.adventure.resolution;
