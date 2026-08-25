package it.polimi.ingsw.server.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that broken game data stops the server rather than reaching a table.
 *
 * <p>Bad data is a packaging error. A loader that fills in a default for a missing
 * field turns it into a rule that quietly does the wrong thing, which is far more
 * expensive to find, so every failure here has to name the entry at fault.
 *
 * <p>Components involved: {@link GameDataLoader}, {@link GameDataSource},
 * {@link GameDataException}.
 */
class GameDataLoaderTest {

    /**
     * A data source serving the bundled files, with one of them replaced.
     *
     * @param name    the file to override
     * @param content what to serve instead
     * @return the patched source
     */
    private static GameDataSource replacing(String name, String content) {
        GameDataSource bundled = GameDataSource.bundled();
        return requested -> requested.equals(name)
                ? new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
                : bundled.open(requested);
    }

    @Test
    @DisplayName("the bundled catalogue loads without complaint")
    void bundledCatalogue_loads() {
        assertDoesNotThrow(GameDataLoader::loadBundled);
    }

    @Test
    @DisplayName("a missing data file fails by name instead of yielding an empty catalogue")
    void missingFile_failsNamingTheFile() {
        GameDataSource bundled = GameDataSource.bundled();
        GameDataSource withoutBoards = name -> {
            if (name.equals("boards.json")) {
                throw new java.io.IOException("not here");
            }
            return bundled.open(name);
        };

        GameDataException failure = assertThrows(GameDataException.class,
                () -> new GameDataLoader(withoutBoards).load());
        assertTrue(failure.getMessage().contains("boards.json"), failure.getMessage());
    }

    @Test
    @DisplayName("a tile with an unknown kind fails by naming the tile and listing the valid kinds")
    void unknownComponentKind_failsNamingTheTile() {
        String content = """
                { "tiles": [ { "id": "made-up-tile", "kind": "TELEPORTER",
                  "connectors": { "NORTH": "PLAIN", "EAST": "PLAIN", "SOUTH": "PLAIN", "WEST": "SINGLE" } } ] }
                """;

        GameDataException failure = assertThrows(GameDataException.class,
                () -> new GameDataLoader(replacing("components.json", content)).load());
        assertTrue(failure.getMessage().contains("made-up-tile"), failure.getMessage());
        assertTrue(failure.getMessage().contains("TELEPORTER"), failure.getMessage());
    }

    @Test
    @DisplayName("a tile missing one of its four sides fails rather than defaulting that side to smooth")
    void tileMissingASide_failsNamingTheTile() {
        String content = """
                { "tiles": [ { "id": "three-sided-tile", "kind": "STRUCTURAL_MODULE",
                  "connectors": { "NORTH": "SINGLE", "EAST": "PLAIN", "SOUTH": "PLAIN" } } ] }
                """;

        GameDataException failure = assertThrows(GameDataException.class,
                () -> new GameDataLoader(replacing("components.json", content)).load());
        assertTrue(failure.getMessage().contains("three-sided-tile"), failure.getMessage());
    }

    @Test
    @DisplayName("a battery without a capacity fails, because the kind requires one")
    void batteryWithoutCapacity_fails() {
        String content = """
                { "tiles": [ { "id": "empty-battery", "kind": "BATTERY",
                  "connectors": { "NORTH": "PLAIN", "EAST": "PLAIN", "SOUTH": "PLAIN", "WEST": "SINGLE" } } ] }
                """;

        GameDataException failure = assertThrows(GameDataException.class,
                () -> new GameDataLoader(replacing("components.json", content)).load());
        assertTrue(failure.getMessage().contains("empty-battery"), failure.getMessage());
    }

    @Test
    @DisplayName("a level II card marked for the test flight fails, since the mark belongs to level I cards")
    void levelTwoCardMarkedForTestFlight_fails() {
        String content = """
                { "cards": [ { "id": "impossible-card", "type": "EPIDEMIC",
                  "level": "LEVEL_II", "testFlight": true } ] }
                """;

        GameDataException failure = assertThrows(GameDataException.class,
                () -> new GameDataLoader(replacing("adventure-cards.json", content)).load());
        assertTrue(failure.getMessage().contains("impossible-card"), failure.getMessage());
    }

    @Test
    @DisplayName("two tiles sharing an identifier fail, because the pool would draw the same tile twice")
    void duplicateTileIdentifier_fails() {
        String tile = """
                { "id": "twin", "kind": "STRUCTURAL_MODULE",
                  "connectors": { "NORTH": "SINGLE", "EAST": "PLAIN", "SOUTH": "PLAIN", "WEST": "PLAIN" } }
                """;
        String content = "{ \"tiles\": [" + tile + "," + tile + "] }";

        GameDataException failure = assertThrows(GameDataException.class,
                () -> new GameDataLoader(replacing("components.json", content)).load());
        assertTrue(failure.getMessage().contains("twin"), failure.getMessage());
    }

    @Test
    @DisplayName("a board whose timer rule contradicts its printed hourglass spaces fails")
    void contradictoryLevelRules_fail() {
        String content = """
                { "levels": {
                    "TEST_FLIGHT": {
                      "flightBoard": { "routeLength": 18, "startingPositions": [4,2,1,0], "hourglassSpaces": 0,
                        "rewards": { "finishOrder": [4,3,2,1], "prettiestShip": 2, "lostComponentPenalty": 1,
                          "goodsPrices": { "RED": 4, "YELLOW": 3, "GREEN": 2, "BLUE": 1 } },
                        "deck": { "piles": 1, "cardsPerPile": { "LEVEL_I": 8 }, "testFlightCardsOnly": true } },
                      "shipBoard": { "rows": 5, "columns": 7, "firstPrintedRow": 5, "firstPrintedColumn": 4,
                        "startingCabin": { "row": 2, "column": 3 }, "reservationSlots": 0, "forbidden": [] },
                      "rules": { "hourglass": true, "componentReservation": false, "cardPilePeeking": false,
                        "aliens": false, "illegalShipCreditPenalty": false } } } }
                """;

        GameDataException failure = assertThrows(GameDataException.class,
                () -> new GameDataLoader(replacing("boards.json", content)).load());
        assertTrue(failure.getMessage().contains("TEST_FLIGHT"), failure.getMessage());
    }
}
