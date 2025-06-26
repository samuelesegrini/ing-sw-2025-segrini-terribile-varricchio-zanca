package it.polimi.ingsw.server.core;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.network.ServerNetworkManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionManagerTest {

    private GameSessionManager gameSessionManager;
    private ServerNetworkManager networkManager;
    private PlayerSessionRegistry playerRegistry;
    private PlayerId testPlayerId1;
    private PlayerId testPlayerId2;
    private PlayerId testPlayerId3;

    @BeforeEach
    void setUp() {
        // Create test dependencies
        networkManager = new ServerNetworkManager(); // TODO: Implement if needed
        playerRegistry = new PlayerSessionRegistry();

        // Create test player IDs using fromString to ensure consistency
        // This ensures that toString() and fromString() work correctly together
        testPlayerId1 = PlayerId.fromString("player1");
        testPlayerId2 = PlayerId.fromString("player2");
        testPlayerId3 = PlayerId.fromString("player3");

        // Initialize GameSessionManager
        gameSessionManager = new GameSessionManager(networkManager, playerRegistry);
    }

    @AfterEach
    void tearDown() {
        if (gameSessionManager != null) {
            gameSessionManager.shutdown();
        }
    }

    @Test
    void testPlayerId_StringConversion() {
        // Test that PlayerId string conversion works correctly
        PlayerId original = PlayerId.fromString("testPlayer");
        String nickname = original.toString();
        PlayerId reconstructed = PlayerId.fromString(nickname);

        assertEquals(original, reconstructed, "PlayerId should be equal after string conversion");
        assertEquals(original.toString(), reconstructed.toString(), "String representations should match");
        assertEquals(original.hashCode(), reconstructed.hashCode(), "Hash codes should match");

        // Test with our test players
        String testPlayer1Nickname = testPlayerId1.toString();
        PlayerId reconstructedTestPlayer1 = PlayerId.fromString(testPlayer1Nickname);
        assertEquals(testPlayerId1, reconstructedTestPlayer1, "Test player 1 should be reconstructible");
    }

    @Test
    void testCreateGame_Success() {
        // Given
        int maxPlayers = 4;
        GameLevel gameLevel = GameLevel.TEST_FLIGHT;
        String gameName = "Test Game";

        // When
        String gameId = gameSessionManager.createGame(testPlayerId1, maxPlayers, gameLevel, gameName);

        // Then
        assertNotNull(gameId, "Game ID should not be null");
        assertFalse(gameId.isEmpty(), "Game ID should not be empty");

        // Verify game session was created
        GameSession session = gameSessionManager.getGameSession(gameId);
        assertNotNull(session, "Game session should exist");

        // Verify player mapping
        assertEquals(gameId, gameSessionManager.getPlayerGameId(testPlayerId1),
                "Player should be mapped to the created game");

        // Verify player is in the game
        GameSession playerSession = gameSessionManager.getGameSessionForPlayer(testPlayerId1);
        assertNotNull(playerSession, "Player should have a game session");
        assertEquals(session, playerSession, "Player should be in the created game session");
    }

    @Test
    void testCreateGame_WithStringPlayerId() {
        // Given
        String playerIdString = testPlayerId1.toString();
        int maxPlayers = 2;
        GameLevel gameLevel = GameLevel.LEVEL_II;
        String gameName = "String Test Game";

        // When
        String gameId = gameSessionManager.createGame(playerIdString, maxPlayers, gameLevel, gameName);

        // Then
        assertNotNull(gameId, "Game ID should not be null");
        GameSession session = gameSessionManager.getGameSession(gameId);
        assertNotNull(session, "Game session should exist");
    }

    @Test
    void testCreateGame_PlayerAlreadyInGame() {
        // Given - Create first game
        String gameId1 = gameSessionManager.createGame(testPlayerId1, 4, GameLevel.TEST_FLIGHT, "Game 1");
        assertNotNull(gameId1, "First game should be created successfully");

        // When - Try to create second game with same player
        String gameId2 = gameSessionManager.createGame(testPlayerId1, 4, GameLevel.LEVEL_II, "Game 2");

        // Then
        assertNull(gameId2, "Second game creation should fail when player is already in a game");

        // Verify player is still in first game
        assertEquals(gameId1, gameSessionManager.getPlayerGameId(testPlayerId1),
                "Player should still be in the first game");
    }

    @Test
    void testJoinGame_Success() {
        // Given - Create a game
        String gameId = gameSessionManager.createGame(testPlayerId1, 4, GameLevel.TEST_FLIGHT, "Test Game");
        assertNotNull(gameId, "Game should be created");

        // When - Join with another player
        boolean joinResult = gameSessionManager.joinGame(gameId, testPlayerId2);

        // Then
        assertTrue(joinResult, "Player should be able to join the game");
        assertEquals(gameId, gameSessionManager.getPlayerGameId(testPlayerId2),
                "Player should be mapped to the game");

        GameSession session = gameSessionManager.getGameSessionForPlayer(testPlayerId2);
        assertNotNull(session, "Player should have a game session");
    }

    @Test
    void testJoinGame_WithStringPlayerId() {
        // Given
        String gameId = gameSessionManager.createGame(testPlayerId1, 4, GameLevel.TEST_FLIGHT, "Test Game");
        String playerIdString = testPlayerId2.toString();

        // When
        boolean joinResult = gameSessionManager.joinGame(gameId, playerIdString);

        // Then
        assertTrue(joinResult, "Player should be able to join the game using string ID");
    }

    @Test
    void testJoinGame_NonExistentGame() {
        // Given
        String nonExistentGameId = UUID.randomUUID().toString();

        // When
        boolean joinResult = gameSessionManager.joinGame(nonExistentGameId, testPlayerId1);

        // Then
        assertFalse(joinResult, "Should not be able to join non-existent game");
        assertNull(gameSessionManager.getPlayerGameId(testPlayerId1),
                "Player should not be mapped to any game");
    }

    @Test
    void testJoinGame_PlayerAlreadyInGame() {
        // Given - Create two games
        String gameId1 = gameSessionManager.createGame(testPlayerId1, 4, GameLevel.TEST_FLIGHT, "Game 1");
        String gameId2 = gameSessionManager.createGame(testPlayerId2, 4, GameLevel.LEVEL_II, "Game 2");

        // When - Try to join second game with player already in first game
        boolean joinResult = gameSessionManager.joinGame(gameId2, testPlayerId1);

        // Then
        assertFalse(joinResult, "Player should not be able to join second game");
        assertEquals(gameId1, gameSessionManager.getPlayerGameId(testPlayerId1),
                "Player should still be in first game");
    }

    @Test
    void testRemovePlayerFromGame_Success() {
        // Given - Create game and add player
        String gameId = gameSessionManager.createGame(testPlayerId1, 4, GameLevel.TEST_FLIGHT, "Test Game");
        gameSessionManager.joinGame(gameId, testPlayerId2);

        // When
        boolean removeResult = gameSessionManager.removePlayerFromGame(gameId, testPlayerId2);

        // Then
        assertTrue(removeResult, "Player should be removed successfully");
        assertNull(gameSessionManager.getPlayerGameId(testPlayerId2),
                "Player should no longer be mapped to any game");
        assertNull(gameSessionManager.getGameSessionForPlayer(testPlayerId2),
                "Player should not have a game session");
    }

    @Test
    void testRemovePlayerFromGame_WithStringPlayerId() {
        // Given
        String gameId = gameSessionManager.createGame(testPlayerId1, 4, GameLevel.TEST_FLIGHT, "Test Game");
        boolean joinResult = gameSessionManager.joinGame(gameId, testPlayerId2);
        assertTrue(joinResult, "Player should join successfully first");

        String playerIdString = testPlayerId2.toString();

        // Debug: Verify player is in game before removal
        assertEquals(gameId, gameSessionManager.getPlayerGameId(testPlayerId2),
                "Player should be in game before removal");
        assertNotNull(gameSessionManager.getGameSessionForPlayer(testPlayerId2),
                "Player should have game session before removal");

        // When
        boolean removeResult = gameSessionManager.removePlayerFromGame(gameId, playerIdString);

        // Then
        assertTrue(removeResult, "Player should be removed successfully using string ID");
    }

    @Test
    void testRemovePlayerFromGame_NonExistentGame() {
        // Given
        String nonExistentGameId = UUID.randomUUID().toString();

        // When
        boolean removeResult = gameSessionManager.removePlayerFromGame(nonExistentGameId, testPlayerId1);

        // Then
        assertFalse(removeResult, "Should not be able to remove player from non-existent game");
    }

    @Test
    void testRemovePlayerFromGame_GameRemovedWhenEmpty() {
        // Given - Create game with only creator
        String gameId = gameSessionManager.createGame(testPlayerId1, 4, GameLevel.TEST_FLIGHT, "Test Game");

        // When - Remove the creator (only player)
        boolean removeResult = gameSessionManager.removePlayerFromGame(gameId, testPlayerId1);

        // Then
        assertTrue(removeResult, "Creator should be removed successfully");
        assertNull(gameSessionManager.getGameSession(gameId),
                "Empty game should be removed from sessions");
    }

    @Test
    void testGetGameSession_ExistingGame() {
        // Given
        String gameId = gameSessionManager.createGame(testPlayerId1, 4, GameLevel.TEST_FLIGHT, "Test Game");

        // When
        GameSession session = gameSessionManager.getGameSession(gameId);

        // Then
        assertNotNull(session, "Should return existing game session");
    }

    @Test
    void testGetGameSession_NonExistentGame() {
        // Given
        String nonExistentGameId = UUID.randomUUID().toString();

        // When
        GameSession session = gameSessionManager.getGameSession(nonExistentGameId);

        // Then
        assertNull(session, "Should return null for non-existent game");
    }

    @Test
    void testGetGameSessionForPlayer_PlayerInGame() {
        // Given
        String gameId = gameSessionManager.createGame(testPlayerId1, 4, GameLevel.TEST_FLIGHT, "Test Game");

        // When
        GameSession session = gameSessionManager.getGameSessionForPlayer(testPlayerId1);

        // Then
        assertNotNull(session, "Should return game session for player in game");
    }

    @Test
    void testGetGameSessionForPlayer_PlayerNotInGame() {
        // When
        GameSession session = gameSessionManager.getGameSessionForPlayer(testPlayerId1);

        // Then
        assertNull(session, "Should return null for player not in any game");
    }

    @Test
    void testGetGameSessionForPlayer_WithStringPlayerId() {
        // Given
        gameSessionManager.createGame(testPlayerId1, 4, GameLevel.TEST_FLIGHT, "Test Game");
        String playerIdString = testPlayerId1.toString();

        // When
        GameSession session = gameSessionManager.getGameSessionForPlayer(playerIdString);

        // Then
        assertNotNull(session, "Should return game session using string player ID");
    }

    @Test
    void testGetPlayerGameId_PlayerInGame() {
        // Given
        String gameId = gameSessionManager.createGame(testPlayerId1, 4, GameLevel.TEST_FLIGHT, "Test Game");

        // When
        String retrievedGameId = gameSessionManager.getPlayerGameId(testPlayerId1);

        // Then
        assertEquals(gameId, retrievedGameId, "Should return correct game ID for player");
    }

    @Test
    void testGetPlayerGameId_PlayerNotInGame() {
        // When
        String gameId = gameSessionManager.getPlayerGameId(testPlayerId1);

        // Then
        assertNull(gameId, "Should return null for player not in any game");
    }

    @Test
    void testGetPlayerGameId_WithStringPlayerId() {
        // Given
        String gameId = gameSessionManager.createGame(testPlayerId1, 4, GameLevel.TEST_FLIGHT, "Test Game");
        String playerIdString = testPlayerId1.toString();

        // When
        String retrievedGameId = gameSessionManager.getPlayerGameId(playerIdString);

        // Then
        assertEquals(gameId, retrievedGameId, "Should return correct game ID using string player ID");
    }

    @Test
    void testGetAvailableGames_NoGames() {
        // When
        List<GameModel> availableGames = gameSessionManager.getAvailableGames();

        // Then
        assertNotNull(availableGames, "Available games list should not be null");
        assertTrue(availableGames.isEmpty(), "Available games list should be empty");
    }

    @Test
    void testGetAvailableGames_WithGames() {
        // Given - Create multiple games
        String gameId1 = gameSessionManager.createGame(testPlayerId1, 4, GameLevel.TEST_FLIGHT, "Game 1");
        String gameId2 = gameSessionManager.createGame(testPlayerId2, 2, GameLevel.LEVEL_II, "Game 2");

        // When
        List<GameModel> availableGames = gameSessionManager.getAvailableGames();

        // Then
        assertNotNull(availableGames, "Available games list should not be null");
        assertEquals(2, availableGames.size(), "Should return all created games");

        // Verify both games are present (order doesn't matter)
        boolean foundGame1 = false;
        boolean foundGame2 = false;
        for (GameModel game : availableGames) {
            if (game.getGameId().equals(gameId1)) {
                foundGame1 = true;
            } else if (game.getGameId().equals(gameId2)) {
                foundGame2 = true;
            }
        }
        assertTrue(foundGame1, "First game should be in available games");
        assertTrue(foundGame2, "Second game should be in available games");
    }

    @Test
    void testMultiplePlayersInSameGame() {
        // Given
        String gameId = gameSessionManager.createGame(testPlayerId1, 4, GameLevel.TEST_FLIGHT, "Multi Player Game");

        // When - Add multiple players
        boolean join2 = gameSessionManager.joinGame(gameId, testPlayerId2);
        boolean join3 = gameSessionManager.joinGame(gameId, testPlayerId3);

        // Then
        assertTrue(join2, "Second player should join successfully");
        assertTrue(join3, "Third player should join successfully");

        // Verify all players are in the same game
        assertEquals(gameId, gameSessionManager.getPlayerGameId(testPlayerId1));
        assertEquals(gameId, gameSessionManager.getPlayerGameId(testPlayerId2));
        assertEquals(gameId, gameSessionManager.getPlayerGameId(testPlayerId3));

        // Verify all players have the same game session
        GameSession session1 = gameSessionManager.getGameSessionForPlayer(testPlayerId1);
        GameSession session2 = gameSessionManager.getGameSessionForPlayer(testPlayerId2);
        GameSession session3 = gameSessionManager.getGameSessionForPlayer(testPlayerId3);

        assertSame(session1, session2, "Players should have same game session");
        assertSame(session2, session3, "Players should have same game session");
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        // This test verifies that the concurrent operations work correctly
        // Given
        final int numThreads = 10;
        final Thread[] threads = new Thread[numThreads];
        final boolean[] results = new boolean[numThreads];
        final String[] gameIds = new String[numThreads];

        // When - Create games concurrently
        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                PlayerId playerId = PlayerId.fromString("concurrentPlayer" + index);
                String gameId = gameSessionManager.createGame(playerId, 4, GameLevel.TEST_FLIGHT, "Game " + index);
                gameIds[index] = gameId;
                results[index] = (gameId != null);
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - All games should be created successfully
        for (int i = 0; i < numThreads; i++) {
            assertTrue(results[i], "Game " + i + " should be created successfully");
            assertNotNull(gameIds[i], "Game ID " + i + " should not be null");
        }

        // Verify all games are tracked
        List<GameModel> availableGames = gameSessionManager.getAvailableGames();
        assertEquals(numThreads, availableGames.size(), "All games should be available");
    }

    @Test
    void testShutdown() {
        // Given - Create some games
        gameSessionManager.createGame(testPlayerId1, 4, GameLevel.TEST_FLIGHT, "Test Game");

        // When
        assertDoesNotThrow(() -> gameSessionManager.shutdown(), "Shutdown should not throw exception");

        // Note: We can't easily test that the executor is actually shut down
        // without access to internal state, but we can verify no exceptions are thrown
    }
}