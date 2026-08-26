package it.polimi.ingsw.server.model.flight;

import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.server.model.board.LevelSpec;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flight configurations for tests, taken from the data the application actually ships.
 *
 * <p>Hand-built board specifications drift from the real ones, and a test that passes
 * against a board nobody plays on is worth very little. Loading the bundled catalogue
 * once keeps the flight tests honest about route lengths, start spaces and rewards.
 *
 * <p>Public because the card tests need flights too, and every card is tested by playing
 * it against one.
 */
public final class FlightFixtures {

    private static final GameData DATA = GameDataLoader.loadBundled();

    private FlightFixtures() {
    }

    /** Returns the real specification for a level, as shipped. */
    public static LevelSpec levelSpec(GameLevel level) {
        return DATA.level(level);
    }

    /**
     * Returns a level II flight with the given ships on the start spaces, best first.
     *
     * <p>Iteration order decides who leads, so a test can say "red, then blue" and get a
     * route order it can reason about.
     *
     * @param ships the ships, in the order they take start spaces
     * @return the flight, rolling sevens
     */
    public static Flight levelTwoFlight(Map<PlayerColor, Ship> ships) {
        return levelTwoFlight(ships, Dice.scripted(7));
    }

    /**
     * Returns a level II flight rolling the given dice.
     *
     * <p>Scripted dice are what make a firing card assertable: a rules test that depends
     * on chance is a rules test nobody trusts.
     *
     * @param ships the ships, in the order they take start spaces
     * @param dice  the dice every card in this flight rolls
     * @return the flight
     */
    public static Flight levelTwoFlight(Map<PlayerColor, Ship> ships, Dice dice) {
        LevelSpec level = levelSpec(GameLevel.LEVEL_II);
        List<Integer> spaces = level.flightBoard().startingPositions();
        Map<PlayerColor, Integer> starts = new LinkedHashMap<>();
        int index = 0;
        for (PlayerColor player : ships.keySet()) {
            starts.put(player, spaces.get(index++));
        }
        return new Flight(level, ships, starts, dice);
    }
}
