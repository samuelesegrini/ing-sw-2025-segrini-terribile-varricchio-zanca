package it.polimi.ingsw.server.model.flight;

import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import it.polimi.ingsw.server.model.board.GameLevel;
import it.polimi.ingsw.server.model.board.LevelSpec;

/**
 * Flight configurations for tests, taken from the data the application actually ships.
 *
 * <p>Hand-built board specifications drift from the real ones, and a test that passes
 * against a board nobody plays on is worth very little. Loading the bundled catalogue
 * once keeps the flight tests honest about route lengths, start spaces and rewards.
 */
final class FlightFixtures {

    private static final GameData DATA = GameDataLoader.loadBundled();

    private FlightFixtures() {
    }

    /** Returns the real specification for a level, as shipped. */
    static LevelSpec levelSpec(GameLevel level) {
        return DATA.level(level);
    }
}
