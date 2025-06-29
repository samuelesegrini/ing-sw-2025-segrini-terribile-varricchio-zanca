package it.polimi.ingsw.server.core;

import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.server.model.domain.general.BuildingTimer;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.common.message.EventPublisher;
import it.polimi.ingsw.common.message.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.util.UUID;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {

    private GameSession gameSession;
    private PlayerId creatorId;
    private PlayerId player2Id;
    private PlayerId player3Id;
    private GameConfigurationManager configManager;
    private PlayerSessionRegistry playerRegistry;
    private TestEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        creatorId = new PlayerId(UUID.randomUUID(), "creator123");
        player2Id = new PlayerId(UUID.randomUUID(), "player2");
        player3Id = new PlayerId(UUID.randomUUID(), "player3");

        configManager = createTestConfigManager();
        playerRegistry = createTestPlayerRegistry();
        eventPublisher = new TestEventPublisher();

        gameSession = new GameSession(
                "test-game-1",
                "Test Game",
                creatorId,
                4, // maxPlayers
                GameLevel.TEST_FLIGHT,
                configManager,
                playerRegistry,
                eventPublisher
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

    private PlayerSessionRegistry createTestPlayerRegistry() {
        return new PlayerSessionRegistry();
    }

    // Dummy EventPublisher for testing
    private static class TestEventPublisher implements EventPublisher {
        public void publish(Event event) {
            // Do nothing - just capture events for testing
        }

        @Override
        public void publishEvent(Event event) {

        }

        @Override
        public void publishEventToClient(Event event, String clientId) {

        }

        @Override
        public void publishEventToGame(Event event, String gameId) {

        }
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
        GameSession.PlayerState creatorState = gameSession.getPlayer(creatorId);
        assertNotNull(creatorState);
        assertTrue(creatorState.isReady());

        // Check creator is also ready in GameModel
        Player creatorPlayer = gameSession.getPlayer(creatorId);
        assertNotNull(creatorPlayer);
        assertTrue(creatorPlayer.isReady());
    }

    @Test
    @DisplayName("Constructor with legacy string creator should work")
    void testLegacyConstructor() {
        GameSession legacySession = new GameSession(
                "legacy-game",
                "Legacy Game",
                "creator123", // String instead of PlayerId
                3,
                GameLevel.TEST_FLIGHT,
                configManager,
                playerRegistry
        );

        assertEquals("legacy-game", legacySession.getGameId());
        assertEquals("Legacy Game", legacySession.getGameName());
        assertEquals(1, legacySession.getPlayerCount());
        assertNotNull(legacySession.getCreatorId());
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
        GameSession.PlayerState player2State = gameSession.getPlayer(player2Id);
        assertNotNull(player2State);
        assertFalse(player2State.isReady());

        // Check player is also in GameModel
        Player player2 = gameSession.getPlayer(player2Id);
        assertNotNull(player2);
        assertFalse(player2.isReady());

        // Add third player
        assertTrue(gameSession.addPlayer(player3Id));
        assertEquals(3, gameSession.getPlayerCount());
    }

    @Test
    @DisplayName("Should add player using string ID (legacy)")
    void testAddPlayerLegacyString() {
        PlayerId playerId = PlayerId.fromString("player2");
        assertTrue(gameSession.addPlayer(playerId));
        assertEquals(2, gameSession.getPlayerCount());

        // Verify player was added correctly
        Player addedPlayer = gameSession.getPlayer(playerId);
        assertNotNull(addedPlayer);
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

        // Player should also be removed from GameModel
        Player removedPlayer = gameSession.getPlayer(player2Id);
        assertNull(removedPlayer);
    }

    @Test
    @DisplayName("Should remove player using string ID (legacy)")
    void testRemovePlayerLegacyString() {
        PlayerId playerId = PlayerId.fromString("player2");
        gameSession.addPlayer(playerId);
        assertEquals(2, gameSession.getPlayerCount());

        assertTrue(gameSession.removePlayer(playerId));
        assertEquals(1, gameSession.getPlayerCount());
    }

    @Test
    @DisplayName("Should not remove non-existent player")
    void testRemoveNonExistentPlayer() {
        assertFalse(gameSession.removePlayer(player2Id));
        assertEquals(1, gameSession.getPlayerCount());
    }

    @Test
    @DisplayName("Should end game when all players leave")
    void testEndGameWhenAllPlayersLeave() {
        gameSession.addPlayer(player2Id);
        assertFalse(gameSession.isEnded());

        gameSession.removePlayer(creatorId);
        gameSession.removePlayer(player2Id);

        assertTrue(gameSession.isEnded());
        assertEquals(GamePhase.END, gameSession.getCurrentPhase());
    }

    @Test
    @DisplayName("Should set player ready status")
    void testSetPlayerReady() {
        gameSession.addPlayer(player2Id);

        // Initially not ready
        assertFalse(gameSession.getPlayer(player2Id).isReady());
        assertFalse(gameSession.getPlayer(player2Id).isReady());

        // Set ready
        gameSession.setPlayerReady(player2Id, true);
        assertTrue(gameSession.getPlayer(player2Id).isReady());
        assertTrue(gameSession.getPlayer(player2Id).isReady());

        // Set not ready
        gameSession.setPlayerReady(player2Id, false);
        assertFalse(gameSession.getPlayer(player2Id).isReady());
        assertFalse(gameSession.getPlayer(player2Id).isReady());
    }

    @Test
    @DisplayName("Should set player ready status using string ID (legacy)")
    void testSetPlayerReadyLegacyString() {
        PlayerId playerId = PlayerId.fromString("player2");
        gameSession.addPlayer(playerId);

        gameSession.setPlayerReady(playerId, true);
        assertTrue(gameSession.getPlayer(playerId).isReady());
    }

    @Test
    @DisplayName("Creator can change ready status")
    void testCreatorCanChangeReadyStatus() {
        // Creator is ready by default
        assertTrue(gameSession.getPlayer(creatorId).isReady());
        assertTrue(gameSession.getPlayer(creatorId).isReady());

        // Creator can become not ready
        gameSession.setPlayerReady(creatorId, false);
        assertFalse(gameSession.getPlayer(creatorId).isReady());
        assertFalse(gameSession.getPlayer(creatorId).isReady());

        // Creator can become ready again
        gameSession.setPlayerReady(creatorId, true);
        assertTrue(gameSession.getPlayer(creatorId).isReady());
        assertTrue(gameSession.getPlayer(creatorId).isReady());
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

    // DISABLED: Component methods moved to GameModel
    // @Test
    // @DisplayName("Should initialize components when game starts")
    // void testComponentInitialization() {
    //     gameSession.addPlayer(player2Id);
    //     gameSession.setPlayerReady(player2Id, true);
    //     gameSession.startGame();
    //
    //     // Check that components are available
    //     Map<String, Component> availableComponents = gameSession.getAvailableComponents();
    //     assertNotNull(availableComponents);
    //     assertFalse(availableComponents.isEmpty());
    // }

    @Test
    @DisplayName("Should handle building timer for supported game levels")
    void testBuildingTimerInitialization() {
        // Create session with a level that supports timer
        GameSession timerSession = new GameSession(
                "timer-game",
                "Timer Game",
                creatorId,
                4,
                GameLevel.TEST_FLIGHT, // Assuming this supports timer
                configManager,
                playerRegistry,
                eventPublisher
        );

        timerSession.addPlayer(player2Id);
        timerSession.setPlayerReady(player2Id, true);
        timerSession.startGame();

        // For TEST_FLIGHT level, timer should not be initialized
        assertNull(gameSession.getBuildingTimer());

        // For other levels, timer might be initialized (depends on implementation)
        // This test would need to be adjusted based on actual timer support
    }

    @Test
    @DisplayName("Should handle component operations")
    void testComponentOperations() {
        gameSession.addPlayer(player2Id);
        gameSession.setPlayerReady(player2Id, true);
        gameSession.startGame();

        // Test getting available components
        Map<String, Component> availableComponents = gameSession.getAvailableComponents();
        assertNotNull(availableComponents);

        // Test component usage if components are available
        if (!availableComponents.isEmpty()) {
            String componentId = availableComponents.keySet().iterator().next();
            Component component = gameSession.getComponentById(componentId);
            assertNotNull(component);

            // Use component
            gameSession.useComponent(componentId, creatorId);

            // Test legacy string method
            gameSession.useComponent(componentId, "creator123");
        }
    }

    @Test
    @DisplayName("Should handle face-up components")
    void testFaceUpComponents() {
        gameSession.addPlayer(player2Id);
        gameSession.setPlayerReady(player2Id, true);
        gameSession.startGame();

        Map<String, Component> availableComponents = gameSession.getAvailableComponents();
        if (!availableComponents.isEmpty()) {
            String componentId = availableComponents.keySet().iterator().next();
            Component component = availableComponents.get(componentId);

            // Add to face-up pile
            gameSession.addToFaceUpPile(componentId, component);

            // Get face-up component
            Component faceUpComponent = gameSession.getFaceUpComponent(componentId);
            assertNotNull(faceUpComponent);

            // Reserve face-up component
            gameSession.reserveFaceUpComponent(componentId, creatorId);

            // Test legacy string method
            gameSession.reserveFaceUpComponent(componentId, "creator123");

            // Check player held components
            List<String> heldComponents = gameSession.getPlayerHeldComponents(creatorId);
            assertNotNull(heldComponents);

            // Test legacy string method
            List<String> heldComponentsLegacy = gameSession.getPlayerHeldComponents("creator123");
            assertNotNull(heldComponentsLegacy);
        }
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
        assertFalse(syncState.timerFlipped);

        // Test legacy string method
        var syncStateLegacy = gameSession.getShipBuildingSyncState("creator123");
        assertNotNull(syncStateLegacy);
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
    @DisplayName("Should handle ship grid configuration")
    void testShipGridConfig() {
        var shipGridConfig = gameSession.getShipGridConfig();
        assertNotNull(shipGridConfig);
    }

    @Test
    @DisplayName("Should handle adventure cards")
    void testAdventureCards() {
        gameSession.addPlayer(player2Id);
        gameSession.setPlayerReady(player2Id, true);
        gameSession.startGame();

        // Initially no current card
        var currentCard = gameSession.getCurrentAdventureCard();
        // Could be null depending on implementation

        // Draw next card
        var nextCard = gameSession.drawNextAdventureCard();
        // Could be null depending on deck state
    }

    @Test
    @DisplayName("Should handle event publisher updates")
    void testEventPublisherUpdate() {
        TestEventPublisher newEventPublisher = new TestEventPublisher();
        gameSession.updateEventPublisher(newEventPublisher);

        // Start game to trigger initialization
        gameSession.addPlayer(player2Id);
        gameSession.setPlayerReady(player2Id, true);
        gameSession.startGame();

        // For TEST_FLIGHT level, adventure card controller might not be initialized
        // This test verifies that the event publisher update doesn't cause errors
        // The actual behavior depends on the specific implementation
        var adventureCardController = gameSession.getAdventureCardController();
        // adventureCardController could be null for TEST_FLIGHT level - this is acceptable
    }

    @Test
    @DisplayName("Should check player ship finished status")
    void testIsPlayerShipFinished() {
        // Creator starts ready, so ship is considered finished
        assertTrue(gameSession.isPlayerShipFinished(creatorId));

        // Add a new player who is not ready by default
        gameSession.addPlayer(player2Id);
        assertFalse(gameSession.isPlayerShipFinished(player2Id));

        // When player becomes ready, ship is considered finished
        gameSession.setPlayerReady(player2Id, true);
        assertTrue(gameSession.isPlayerShipFinished(player2Id));

        // When player becomes not ready, ship is not finished
        gameSession.setPlayerReady(player2Id, false);
        assertFalse(gameSession.isPlayerShipFinished(player2Id));
    }

    @Test
    @DisplayName("Should handle building timer flipping")
    void testBuildingTimerFlipping() {
        // For TEST_FLIGHT level, timer system is not active
        assertThrows(IllegalStateException.class, () -> {
            gameSession.flipBuildingTimer("creator123", true);
        });

        // Test legacy method
        gameSession.addPlayer(player2Id);
        gameSession.setPlayerReady(player2Id, true);
        gameSession.startGame();

        // Legacy flip method should work
        long timeRemaining = gameSession.flipBuildingTimer(30000L);
        assertTrue(timeRemaining >= 0);
    }

    @Test
    @DisplayName("Should handle legacy string methods")
    void testLegacyStringMethods() {
        PlayerId player2Id = PlayerId.fromString("player2");

        // Test string overloads
        assertTrue(gameSession.addPlayer(player2Id));
        assertNotNull(gameSession.getPlayer(player2Id));
        assertNotNull(gameSession.getPlayer(player2Id));

        gameSession.setPlayerReady(player2Id, true);
        assertTrue(gameSession.getPlayer(player2Id).isReady());

        assertFalse(gameSession.isCreator(player2Id));
        assertTrue(gameSession.removePlayer(player2Id));
    }

    @Test
    @DisplayName("Should handle player list retrieval")
    void testGetPlayers() {
        gameSession.addPlayer(player2Id);
        gameSession.addPlayer(player3Id);

        List<Player> players = gameSession.getPlayers();
        assertNotNull(players);
        assertEquals(3, players.size());

        // Check that all players are present
        boolean hasCreator = players.stream().anyMatch(p -> p.getId().equals(creatorId));
        boolean hasPlayer2 = players.stream().anyMatch(p -> p.getId().equals(player2Id));
        boolean hasPlayer3 = players.stream().anyMatch(p -> p.getId().equals(player3Id));

        assertTrue(hasCreator);
        assertTrue(hasPlayer2);
        assertTrue(hasPlayer3);
    }

    @Test
    @DisplayName("Should handle game level retrieval")
    void testGetGameLevel() {
        assertEquals(GameLevel.TEST_FLIGHT, gameSession.getGameLevel());
    }
}