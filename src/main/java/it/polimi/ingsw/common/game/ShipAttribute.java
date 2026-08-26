package it.polimi.ingsw.common.game;

/**
 * An attribute a card can ask a ship to declare.
 *
 * <p>All three are compared by Combat Zone; two of them are also declared on their own,
 * by Open Space and by every enemy card (manual p.11, p.13).
 */
public enum ShipAttribute {

    /** How far the ship can travel, and what an enemy cannot take away. */
    ENGINE_POWER,

    /** What the ship brings to a fight, counted in halves so that 5½ stays 5½. */
    FIREPOWER,

    /** Humans and aliens aboard. Needs no declaration: nothing about it is optional. */
    CREW
}
