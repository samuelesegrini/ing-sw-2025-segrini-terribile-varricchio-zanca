package it.polimi.ingsw;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Guards the location of the game data files.
 *
 * <p>Cards, components and boards are loaded from the classpath at runtime, so a
 * rename or a move that the compiler cannot see would only surface when a game is
 * started. These tests fail at build time instead.
 */
class GameDataResourcesTest {

    @DisplayName("every game data file is reachable on the classpath at its documented path")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "/data/adventure-cards.json",
            "/data/components.json",
            "/data/boards.json"
    })
    void gameDataFile_isOnTheClasspath(String resourcePath) {
        try (InputStream stream = GameDataResourcesTest.class.getResourceAsStream(resourcePath)) {
            assertNotNull(stream, "missing game data resource: " + resourcePath);
        } catch (Exception e) {
            throw new AssertionError("could not open game data resource: " + resourcePath, e);
        }
    }
}
