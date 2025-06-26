package it.polimi.ingsw.common.model;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameInfoTest {

    @Test
    @DisplayName("Should create GameInfo with all valid parameters")
    void shouldCreateGameInfoWithValidParameters() {
        // Given
        String gameId = "game-123";
        String gameName = "Test Game";
        GameLevel gameLevel = GameLevel.TEST_FLIGHT;
        int currentPlayerCount = 2;
        int maxPlayers = 4;
        GamePhase currentPhase = GamePhase.SETUP;
        boolean isStarted = false;
        boolean isJoinable = true;

        // When
        GameInfo gameInfo = new GameInfo(gameId, gameName, gameLevel, currentPlayerCount,
                maxPlayers, currentPhase, isStarted, isJoinable);

        // Then
        assertEquals(gameId, gameInfo.getGameId());
        assertEquals(gameName, gameInfo.getGameName());
        assertEquals(gameLevel, gameInfo.getGameLevel());
        assertEquals(currentPlayerCount, gameInfo.getCurrentPlayerCount());
        assertEquals(maxPlayers, gameInfo.getMaxPlayers());
        assertEquals(currentPhase, gameInfo.getCurrentPhase());
        assertEquals(isStarted, gameInfo.isStarted());
        assertEquals(isJoinable, gameInfo.isJoinable());
    }

    @Test
    @DisplayName("Should create GameInfo with null values")
    void shouldCreateGameInfoWithNullValues() {
        // When
        GameInfo gameInfo = new GameInfo(null, null, null, 0, 0, null, false, false);

        // Then
        assertNull(gameInfo.getGameId());
        assertNull(gameInfo.getGameName());
        assertNull(gameInfo.getGameLevel());
        assertEquals(0, gameInfo.getCurrentPlayerCount());
        assertEquals(0, gameInfo.getMaxPlayers());
        assertNull(gameInfo.getCurrentPhase());
        assertFalse(gameInfo.isStarted());
        assertFalse(gameInfo.isJoinable());
    }

    @Test
    @DisplayName("Should create GameInfo with negative player counts")
    void shouldCreateGameInfoWithNegativePlayerCounts() {
        // When
        GameInfo gameInfo = new GameInfo("test", "test", GameLevel.LEVEL_II, -1, -5,
                GamePhase.SETUP, true, false);

        // Then
        assertEquals(-1, gameInfo.getCurrentPlayerCount());
        assertEquals(-5, gameInfo.getMaxPlayers());
    }

    @Test
    @DisplayName("Should return correct game ID")
    void shouldReturnCorrectGameId() {
        GameInfo gameInfo = new GameInfo("game-456", "Advanced Game", GameLevel.LEVEL_II,
                3, 6, GamePhase.BUILDING, true, false);
        assertEquals("game-456", gameInfo.getGameId());
    }

    @Test
    @DisplayName("Should return correct game name")
    void shouldReturnCorrectGameName() {
        GameInfo gameInfo = new GameInfo("game-456", "Advanced Game", GameLevel.LEVEL_II,
                3, 6, GamePhase.BUILDING, true, false);
        assertEquals("Advanced Game", gameInfo.getGameName());
    }

    @Test
    @DisplayName("Should return correct game level")
    void shouldReturnCorrectGameLevel() {
        GameInfo gameInfo = new GameInfo("game-456", "Advanced Game", GameLevel.LEVEL_II,
                3, 6, GamePhase.BUILDING, true, false);
        assertEquals(GameLevel.LEVEL_II, gameInfo.getGameLevel());
    }

    @Test
    @DisplayName("Should return correct current player count")
    void shouldReturnCorrectCurrentPlayerCount() {
        GameInfo gameInfo = new GameInfo("game-456", "Advanced Game", GameLevel.LEVEL_II,
                3, 6, GamePhase.BUILDING, true, false);
        assertEquals(3, gameInfo.getCurrentPlayerCount());
    }

    @Test
    @DisplayName("Should return correct max players")
    void shouldReturnCorrectMaxPlayers() {
        GameInfo gameInfo = new GameInfo("game-456", "Advanced Game", GameLevel.LEVEL_II,
                3, 6, GamePhase.BUILDING, true, false);
        assertEquals(6, gameInfo.getMaxPlayers());
    }

    @Test
    @DisplayName("Should return correct current phase")
    void shouldReturnCorrectCurrentPhase() {
        GameInfo gameInfo = new GameInfo("game-456", "Advanced Game", GameLevel.LEVEL_II,
                3, 6, GamePhase.BUILDING, true, false);
        assertEquals(GamePhase.BUILDING, gameInfo.getCurrentPhase());
    }

    @Test
    @DisplayName("Should return correct started status")
    void shouldReturnCorrectStartedStatus() {
        GameInfo gameInfo = new GameInfo("game-456", "Advanced Game", GameLevel.LEVEL_II,
                3, 6, GamePhase.BUILDING, true, false);
        assertTrue(gameInfo.isStarted());
    }

    @Test
    @DisplayName("Should return correct joinable status")
    void shouldReturnCorrectJoinableStatus() {
        GameInfo gameInfo = new GameInfo("game-456", "Advanced Game", GameLevel.LEVEL_II,
                3, 6, GamePhase.BUILDING, true, false);
        assertFalse(gameInfo.isJoinable());
    }

    @Test
    @DisplayName("Should return null when GameModel is null")
    void shouldReturnNullWhenGameModelIsNull() {
        // When
        GameInfo result = GameInfo.fromGameModel(null);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should return formatted string with all fields")
    void shouldReturnFormattedStringWithAllFields() {
        // Given
        GameInfo gameInfo = new GameInfo("test-123", "My Game", GameLevel.TEST_FLIGHT,
                2, 4, GamePhase.SETUP, false, true);

        // When
        String result = gameInfo.toString();

        // Then
        String expected = "GameInfo{id='test-123', name='My Game', level=TEST_FLIGHT, " +
                "players=2/4, phase=SETUP, started=false, joinable=true}";
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should handle null values in toString")
    void shouldHandleNullValuesInToString() {
        // Given
        GameInfo gameInfo = new GameInfo(null, null, null, 0, 0, null, false, false);

        // When
        String result = gameInfo.toString();

        // Then
        String expected = "GameInfo{id='null', name='null', level=null, " +
                "players=0/0, phase=null, started=false, joinable=false}";
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should format string correctly with TEST_FLIGHT level")
    void shouldFormatStringCorrectlyWithTestFlightLevel() {
        // Given
        GameInfo gameInfo = new GameInfo("test", "test", GameLevel.TEST_FLIGHT, 1, 2,
                GamePhase.FLIGHT, true, false);

        // When
        String result = gameInfo.toString();
        System.out.println(result);

        // Then
        assertTrue(result.contains("level=TEST_FLIGHT"));
        assertTrue(result.contains("phase=FLIGHT"));
    }

    @Test
    @DisplayName("Should be serializable and deserializable")
    void shouldBeSerializableAndDeserializable() throws IOException, ClassNotFoundException {
        // Given
        GameInfo original = new GameInfo("serial-test", "Serializable Game", GameLevel.LEVEL_II,
                3, 5, GamePhase.BUILDING, true, false);

        // When
        byte[] serialized = serialize(original);
        GameInfo deserialized = deserialize(serialized);

        // Then
        assertNotNull(deserialized);
        assertEquals(original.getGameId(), deserialized.getGameId());
        assertEquals(original.getGameName(), deserialized.getGameName());
        assertEquals(original.getGameLevel(), deserialized.getGameLevel());
        assertEquals(original.getCurrentPlayerCount(), deserialized.getCurrentPlayerCount());
        assertEquals(original.getMaxPlayers(), deserialized.getMaxPlayers());
        assertEquals(original.getCurrentPhase(), deserialized.getCurrentPhase());
        assertEquals(original.isStarted(), deserialized.isStarted());
        assertEquals(original.isJoinable(), deserialized.isJoinable());
    }

    @Test
    @DisplayName("Should serialize and deserialize null values correctly")
    void shouldSerializeAndDeserializeNullValuesCorrectly() throws IOException, ClassNotFoundException {
        // Given
        GameInfo original = new GameInfo(null, null, null, 0, 0, null, false, false);

        // When
        byte[] serialized = serialize(original);
        GameInfo deserialized = deserialize(serialized);

        // Then
        assertNotNull(deserialized);
        assertNull(deserialized.getGameId());
        assertNull(deserialized.getGameName());
        assertNull(deserialized.getGameLevel());
        assertEquals(0, deserialized.getCurrentPlayerCount());
        assertEquals(0, deserialized.getMaxPlayers());
        assertNull(deserialized.getCurrentPhase());
        assertFalse(deserialized.isStarted());
        assertFalse(deserialized.isJoinable());
    }

    @Test
    @DisplayName("Should handle extreme player count values")
    void shouldHandleExtremePlayerCountValues() {
        // Given
        GameInfo gameInfo = new GameInfo("extreme", "Extreme Game", GameLevel.TEST_FLIGHT,
                Integer.MAX_VALUE, Integer.MIN_VALUE,
                GamePhase.SETUP, false, true);

        // Then
        assertEquals(Integer.MAX_VALUE, gameInfo.getCurrentPlayerCount());
        assertEquals(Integer.MIN_VALUE, gameInfo.getMaxPlayers());
    }

    @Test
    @DisplayName("Should handle empty strings")
    void shouldHandleEmptyStrings() {
        // Given
        GameInfo gameInfo = new GameInfo("", "", GameLevel.LEVEL_II, 1, 2,
                GamePhase.FLIGHT, true, false);

        // Then
        assertEquals("", gameInfo.getGameId());
        assertEquals("", gameInfo.getGameName());
    }

    @Test
    @DisplayName("Should handle special characters in strings")
    void shouldHandleSpecialCharactersInStrings() {
        // Given
        String specialId = "game-123!@#$%^&*()_+-=[]{}|;':\",./<>?";
        String specialName = "Game with émojis 🎮 and ñovelties";
        GameInfo gameInfo = new GameInfo(specialId, specialName, GameLevel.LEVEL_II,
                1, 1, GamePhase.END, true, false);

        // Then
        assertEquals(specialId, gameInfo.getGameId());
        assertEquals(specialName, gameInfo.getGameName());
        assertTrue(gameInfo.toString().contains(specialId));
        assertTrue(gameInfo.toString().contains(specialName));
    }

    @Test
    @DisplayName("Should serialize GameInfo with all GamePhases")
    void shouldSerializeGameInfoWithAllGamePhases() throws IOException, ClassNotFoundException {
        GamePhase[] phases = {GamePhase.SETUP, GamePhase.BUILDING, GamePhase.FLIGHT, GamePhase.END};

        for (GamePhase phase : phases) {
            // Given
            GameInfo original = new GameInfo("test-" + phase, "Test Game", GameLevel.TEST_FLIGHT,
                    2, 4, phase, phase != GamePhase.SETUP, phase == GamePhase.SETUP);

            // When
            byte[] serialized = serialize(original);
            GameInfo deserialized = deserialize(serialized);

            // Then
            assertEquals(phase, deserialized.getCurrentPhase());
        }
    }

    private byte[] serialize(GameInfo gameInfo) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(gameInfo);
        }
        return baos.toByteArray();
    }

    private GameInfo deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (GameInfo) ois.readObject();
        }
    }
}