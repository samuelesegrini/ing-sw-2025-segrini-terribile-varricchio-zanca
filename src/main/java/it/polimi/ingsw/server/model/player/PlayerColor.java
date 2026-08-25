package it.polimi.ingsw.server.model.player;

/**
 * The four colours a player can take.
 *
 * <p>A colour ties together a player's starting cabin and their two rocket markers,
 * so it is fixed for the whole game and unique within one game.
 */
public enum PlayerColor {

    /** Blue. */
    BLUE,

    /** Green. */
    GREEN,

    /** Red. */
    RED,

    /** Yellow. */
    YELLOW
}
