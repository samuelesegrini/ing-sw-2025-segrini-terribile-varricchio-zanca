package it.polimi.ingsw.server.core;

import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {

    private GameSession gameSession;
    private PlayerId creatorId;
    private PlayerId player2Id;
    private PlayerId player3Id;
    private GameConfigurationManager configManager;
    private PlayerSessionRegistry playerRegistry;

    @BeforeEach
    void setUp() {
        creatorId = new PlayerId(UUID.randomUUID(), "creator123");
        player2Id = new PlayerId(UUID.randomUUID(), "player2");
        player3Id = new PlayerId(UUID.randomUUID(), "player3");

        // TODO: Replace with actual implementations
        configManager = createTestConfigManager();
        playerRegistry = createTestPlayerRegistry();

        gameSession = new GameSession(
                "test-game-1",
                "Test Game",
                creatorId,
                4, // maxPlayers
                GameLevel.TEST_FLIGHT,
                configManager,
                playerRegistry
        );
    }

    private GameConfigurationManager createTestConfigManager() {
        GameConfigurationManager configManager = new GameConfigurationManager();
        try {
            // Carica le configurazioni dai file JSON di test
            configManager.loadAllConfigurations(
                    "/json/components.json",
                   "/json/adventure_cards.json",
                    "/json/game_configurations.json"
            );
        } catch (IOException e) {
            // In caso di errore nel caricamento, lancia un'eccezione di runtime
            throw new RuntimeException("Errore nel caricamento delle configurazioni di test", e);
        }
        return configManager;
    }

    // TODO: Implement based on actual PlayerSessionRegistry
    private PlayerSessionRegistry createTestPlayerRegistry() {
        // Crea una nuova istanza del registro per i test
        return new PlayerSessionRegistry();
    }

    @Test
    @DisplayName("Constructor should create GameSession with creator as first player")
    void testConstructor() {
        assertEquals("test-game-1", gameSession.getGameId());
        assertEquals("Test Game", gameSession.getGameName());
        assertEquals(creatorId, gameSession.getCreatorId());
        assertEquals(4, gameSession.getMaxPlayers());
        assertEquals(1, gameSession.getPlayerCount());
        assertEquals(GamePhase.SETUP, gameSession.getCurrentPhase());
        assertFalse(gameSession.isStarted());
        assertFalse(gameSession.isEnded());
        assertTrue(gameSession.canJoin());

        // Creator should be added and ready by default
        assertTrue(gameSession.getPlayerIds().contains(creatorId));
        assertTrue(gameSession.isCreator(creatorId));

        // Check creator ready status
        GameSession.PlayerState creatorState = gameSession.getPlayerState(creatorId);
        assertNotNull(creatorState);
        assertTrue(creatorState.isReady());
    }

    @Test
    @DisplayName("Should add players successfully")
    void testAddPlayer() {
        // Add second player
        assertTrue(gameSession.addPlayer(player2Id));
        assertEquals(2, gameSession.getPlayerCount());
        assertTrue(gameSession.getPlayerIds().contains(player2Id));
        assertFalse(gameSession.isCreator(player2Id));

        // Second player should not be ready by default
        GameSession.PlayerState player2State = gameSession.getPlayerState(player2Id);
        assertNotNull(player2State);
        assertFalse(player2State.isReady());

        // Add third player
        assertTrue(gameSession.addPlayer(player3Id));
        assertEquals(3, gameSession.getPlayerCount());
    }

    @Test
    @DisplayName("Should not add player if game is full")
    void testAddPlayerWhenFull() {
        // Fill the game (already has creator)
        gameSession.addPlayer(player2Id);
        gameSession.addPlayer(player3Id);
        PlayerId player4Id = PlayerId.fromString("player4");
        gameSession.addPlayer(player4Id);

        assertEquals(4, gameSession.getPlayerCount());

        // Try to add fifth player
        PlayerId player5Id = PlayerId.fromString("player5");
        assertFalse(gameSession.addPlayer(player5Id));
        assertEquals(4, gameSession.getPlayerCount());
    }

    @Test
    @DisplayName("Should not add player if game is started")
    void testAddPlayerWhenStarted() {
        // Add player and set ready
        gameSession.addPlayer(player2Id);
        gameSession.setPlayerReady(player2Id, true);

        // Start game
        assertTrue(gameSession.canStart());
        assertTrue(gameSession.startGame());

        // Try to add player after start
        assertFalse(gameSession.addPlayer(player3Id));
        assertEquals(2, gameSession.getPlayerCount());
    }

    @Test
    @DisplayName("Should remove player successfully")
    void testRemovePlayer() {
        gameSession.addPlayer(player2Id);
        gameSession.addPlayer(player3Id);
        assertEquals(3, gameSession.getPlayerCount());

        assertTrue(gameSession.removePlayer(player2Id));
        assertEquals(2, gameSession.getPlayerCount());
        assertFalse(gameSession.getPlayerIds().contains(player2Id));
    }

    @Test
    @DisplayName("Should not remove non-existent player")
    void testRemoveNonExistentPlayer() {
        assertFalse(gameSession.removePlayer(player2Id));
        assertEquals(1, gameSession.getPlayerCount());
    }

    @Test
    @DisplayName("Should set player ready status")
    void testSetPlayerReady() {
        gameSession.addPlayer(player2Id);

        // Initially not ready
        assertFalse(gameSession.getPlayerState(player2Id).isReady());

        // Set ready
        gameSession.setPlayerReady(player2Id, true);
        assertTrue(gameSession.getPlayerState(player2Id).isReady());

        // Set not ready
        gameSession.setPlayerReady(player2Id, false);
        assertFalse(gameSession.getPlayerState(player2Id).isReady());
    }

    @Test
    @DisplayName("Creator can change ready status")
    void testCreatorCanChangeReadyStatus() {
        // Creator is ready by default
        assertTrue(gameSession.getPlayerState(creatorId).isReady());

        // Creator can become not ready
        gameSession.setPlayerReady(creatorId, false);
        assertFalse(gameSession.getPlayerState(creatorId).isReady());

        // Creator can become ready again
        gameSession.setPlayerReady(creatorId, true);
        assertTrue(gameSession.getPlayerState(creatorId).isReady());
    }

    @Test
    @DisplayName("Should check if all players are ready")
    void testAreAllPlayersReady() {
        // Only creator, who is ready
        assertTrue(gameSession.areAllPlayersReady());

        // Add player who is not ready
        gameSession.addPlayer(player2Id);
        assertFalse(gameSession.areAllPlayersReady());

        // Make all players ready
        gameSession.setPlayerReady(player2Id, true);
        assertTrue(gameSession.areAllPlayersReady());

        // Make creator not ready
        gameSession.setPlayerReady(creatorId, false);
        assertFalse(gameSession.areAllPlayersReady());
    }

    @Test
    @DisplayName("Should check if game can start")
    void testCanStart() {
        // Cannot start with only one player
        assertFalse(gameSession.canStart());

        // Add second player
        gameSession.addPlayer(player2Id);
        // Still cannot start (player2 not ready)
        assertFalse(gameSession.canStart());

        // Make all players ready
        gameSession.setPlayerReady(player2Id, true);
        assertTrue(gameSession.canStart());

        // Make creator not ready
        gameSession.setPlayerReady(creatorId, false);
        assertFalse(gameSession.canStart());
    }

    @Test
    @DisplayName("Should start game successfully")
    void testStartGame() {
        // Add second player and make ready
        gameSession.addPlayer(player2Id);
        gameSession.setPlayerReady(player2Id, true);

        assertFalse(gameSession.isStarted());
        assertTrue(gameSession.startGame());
        assertTrue(gameSession.isStarted());
        assertEquals(GamePhase.BUILDING, gameSession.getCurrentPhase());
        assertFalse(gameSession.canJoin());
    }

    @Test
    @DisplayName("Should not start game if conditions not met")
    void testStartGameFails() {
        // Cannot start with only creator
        assertFalse(gameSession.startGame());
        assertFalse(gameSession.isStarted());

        // Add player but not ready
        gameSession.addPlayer(player2Id);
        assertFalse(gameSession.startGame());
        assertFalse(gameSession.isStarted());
    }

    @Test
    @DisplayName("Should handle game state correctly")
    void testGameState() {
        gameSession.addPlayer(player2Id);

        var gameState = gameSession.getGameState();
        assertEquals("test-game-1", gameState.get("gameId"));
        assertEquals(GamePhase.SETUP, gameState.get("phase"));
        assertEquals(false, gameState.get("started"));
        assertEquals(false, gameState.get("ended"));

        @SuppressWarnings("unchecked")
        var players = (java.util.List<PlayerId>) gameState.get("players");
        assertEquals(2, players.size());
        assertTrue(players.contains(creatorId));
        assertTrue(players.contains(player2Id));
    }

    @Test
    @DisplayName("Should handle component operations")
    void testComponentOperations() {
        // This test will depend on Component class structure
        // TODO: Implement based on actual Component class

        // Test getting available components
        var availableComponents = gameSession.getAvailableComponents();
        assertNotNull(availableComponents);

        // Test component usage
        // gameSession.useComponent("component1", creatorId);

        // Test getting component by ID
        // Component component = gameSession.getComponentById("component1");
    }

    @Test
    @DisplayName("Should handle ship building sync state")
    void testShipBuildingSyncState() {
        var syncState = gameSession.getShipBuildingSyncState(creatorId);
        assertNotNull(syncState);
        assertNotNull(syncState.shipGrid);
        assertNotNull(syncState.availableTiles);
        assertNotNull(syncState.heldTiles);
        assertNotNull(syncState.forbiddenPositions);
        assertTrue(syncState.buildingTimeRemaining >= 0);
    }

    @Test
    @DisplayName("Should handle legacy string methods")
    void testLegacyStringMethods() {
        String player2IdString = "player2";

        // Test string overloads
        assertTrue(gameSession.addPlayer(player2IdString));
        assertNotNull(gameSession.getPlayer(player2IdString));
        assertNotNull(gameSession.getPlayerState(player2IdString));

        gameSession.setPlayerReady(player2IdString, true);
        assertTrue(gameSession.getPlayerState(player2IdString).isReady());

        assertFalse(gameSession.isCreator(player2IdString));
        assertTrue(gameSession.removePlayer(player2IdString));
    }
}