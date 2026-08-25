package it.polimi.ingsw.server.model.board;

/**
 * Which flight is being played.
 *
 * <p>The project implements two of the game's configurations: the standard level II
 * flight, which the requirements call the complete rules, and the test flight, offered
 * as an advanced feature. Levels I and III as standalone flights, and the Trasvolata
 * Intergalattica campaign, are explicitly out of scope.
 */
public enum GameLevel {

    /** The learning flight of manual pages 1 to 15. Smaller ship, shorter route, eight cards. */
    TEST_FLIGHT,

    /** The standard flight. Every rule in the manual short of the campaign. */
    LEVEL_II
}
