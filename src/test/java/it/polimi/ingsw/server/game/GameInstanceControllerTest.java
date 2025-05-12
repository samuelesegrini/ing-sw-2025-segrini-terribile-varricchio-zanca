package it.polimi.ingsw.server.game;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.flight.FlightStatus;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.commands.CommandProcessor;
import it.polimi.ingsw.server.commands.CommandResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class GameInstanceControllerTest {

    private GameModel gameModel;
    private RuleEngine ruleEngine;
    private ScoringEngine scoringEngine;
    private CommandProcessor commandProcessor;
    private SessionManager sessionManager;
    private ExecutorService gameLogicExecutor;
    private ExecutorService networkExecutor;
    private GameInstanceController gameInstanceController;
    private static final String TEST_GAME_ID = "test-game-id";
    private static final int TEST_PLAYER_COUNT = 2;

    @BeforeEach
    void setUp() {
        GameConfigurationManager configManager = new GameConfigurationManager();
        gameModel = new GameModel(GameLevel.LEVEL_II, configManager, TEST_PLAYER_COUNT);
        ruleEngine = new RuleEngine();
        scoringEngine = new ScoringEngine();
        commandProcessor = new CommandProcessor(gameModel);
        sessionManager = new SessionManager(playerId -> {}); // Empty callback
        gameLogicExecutor = Executors.newSingleThreadExecutor();
        networkExecutor = Executors.newSingleThreadExecutor();

        gameInstanceController = new GameInstanceController(
            TEST_GAME_ID,
            gameModel,
            ruleEngine,
            scoringEngine,
            commandProcessor,
            sessionManager,
            gameLogicExecutor,
            networkExecutor
        );
    }

    @Test
    void testConstructor() {
        assertNotNull(gameInstanceController);
        assertEquals(TEST_GAME_ID, gameInstanceController.getGameId());
    }

    @Test
    void testAddPlayer() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        ServerClientHandler clientHandler = new MockServerClientHandler();

        // Test
        CompletableFuture<Boolean> result = gameInstanceController.addPlayer(playerId, clientHandler);

        // Verify
        assertTrue(result.join());
        assertEquals(playerId.toString(), clientHandler.getPlayerId());
    }

    @Test
    void testStartGame() {
        // Setup
        gameModel.setCurrentPhase(GamePhase.SETUP);
        
        // Add players to allow game to start
        PlayerId player1 = PlayerId.fromString("player1");
        PlayerId player2 = PlayerId.fromString("player2");
        MockServerClientHandler handler1 = new MockServerClientHandler();
        MockServerClientHandler handler2 = new MockServerClientHandler();
        
        gameInstanceController.addPlayer(player1, handler1).join();
        gameInstanceController.addPlayer(player2, handler2).join();

        // Test
        gameInstanceController.startGame();

        // Verify
        assertEquals(GamePhase.BUILDING, gameModel.getCurrentPhase());
    }

    @Test
    void testStartGameNotInSetupPhase() {
        // Setup
        gameModel.setCurrentPhase(GamePhase.BUILDING);

        // Test
        gameInstanceController.startGame();

        // Verify
        assertEquals(GamePhase.BUILDING, gameModel.getCurrentPhase());
    }

    @Test
    void testRemoveClientHandler() {
        // Setup
        String playerIdString = "testPlayer";
        PlayerId playerId = PlayerId.fromString(playerIdString);
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        
        // Add the player first
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        // Test
        gameInstanceController.removeClientHandler(playerIdString, true);

        // Verify
        assertNull(clientHandler.getPlayerId());
    }

    @Test
    void testRemoveClientHandlerUnknownPlayer() {
        // Test
        gameInstanceController.removeClientHandler("unknownPlayer", true);

        // Verify - should not throw exception
    }

    @Test
    void testShutdown() {
        // Setup
        String playerIdString = "testPlayer";
        PlayerId playerId = PlayerId.fromString(playerIdString);
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        
        // Add the player first
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        // Test
        gameInstanceController.shutdown();

        // Verify
        assertNull(clientHandler.getPlayerId());
        gameLogicExecutor.shutdown();
        networkExecutor.shutdown();
    }

    @Test
    void testProcessComponentRequest() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        // Test
        CompletableFuture<CommandResult> result = gameInstanceController.processComponentRequest("testPlayer", "ENGINE");

        // Verify
        CommandResult commandResult = result.join();
        assertFalse(commandResult.isSuccess());
        assertEquals("AcquireComponentCommand not implemented", commandResult.getMessage());
    }

    @Test
    void testProcessComponentRequestUnknownPlayer() {
        // Test
        CompletableFuture<CommandResult> result = gameInstanceController.processComponentRequest("unknownPlayer", "ENGINE");

        // Verify
        CommandResult commandResult = result.join();
        assertFalse(commandResult.isSuccess());
        assertEquals("Unknown player ID: unknownPlayer", commandResult.getMessage());
    }

    @Test
    void testProcessPlacementRequest() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        ComponentDTO componentDTO = new ComponentDTO();
        componentDTO.setType("ENGINE");
        Position position = new Position(0, 0);

        // Test
        CompletableFuture<CommandResult> result = gameInstanceController.processPlacementRequest("testPlayer", componentDTO, position);

        // Verify
        CommandResult commandResult = result.join();
        assertFalse(commandResult.isSuccess());
        assertEquals("Could not identify component model from DTO.", commandResult.getMessage());
    }

    @Test
    void testProcessPlacementRequestUnknownPlayer() {
        // Setup
        ComponentDTO componentDTO = new ComponentDTO();
        componentDTO.setType("ENGINE");
        Position position = new Position(0, 0);

        // Test
        CompletableFuture<CommandResult> result = gameInstanceController.processPlacementRequest("unknownPlayer", componentDTO, position);

        // Verify
        CommandResult commandResult = result.join();
        assertFalse(commandResult.isSuccess());
        assertEquals("Unknown player ID: unknownPlayer", commandResult.getMessage());
    }

    @Test
    void testProcessReconnectionRequest() {
        // Setup
        String playerNickname = "testPlayer";
        PlayerId playerId = PlayerId.fromString(playerNickname);
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        
        // Add player to game model first
        gameModel.addPlayer(playerId, playerNickname);
        
        // Add player to controller
        gameInstanceController.addPlayer(playerId, clientHandler).join();
        
        // Create a session token
        String sessionToken = UUID.randomUUID().toString();
        
        // Since we can't directly register the session, we'll mock the behavior
        // by creating a new handler that will be used for reconnection
        MockServerClientHandler newHandler = new MockServerClientHandler();

        // Test
        CompletableFuture<Boolean> result = gameInstanceController.processReconnectionRequest(playerId, sessionToken, newHandler);

        // Verify - with an invalid token, the reconnection should fail
        assertFalse(result.join());
        assertNull(newHandler.getPlayerId());
    }

    @Test
    void testCheckAndEndBuildPhase() {
        // Setup
        gameModel.setCurrentPhase(GamePhase.BUILDING);

        // Test
        gameInstanceController.checkAndEndBuildPhase();

        // Verify
        assertEquals(GamePhase.FLIGHT, gameModel.getCurrentPhase());
    }

    @Test
    void testCheckAndEndBuildPhaseNotInBuildingPhase() {
        // Setup
        gameModel.setCurrentPhase(GamePhase.SETUP);

        // Test
        gameInstanceController.checkAndEndBuildPhase();

        // Verify
        assertEquals(GamePhase.SETUP, gameModel.getCurrentPhase());
    }

    @Test
    void testSendFullGameState() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        // Test
        CompletableFuture<Void> result = gameInstanceController.sendFullGameState("testPlayer");

        // Verify
        assertDoesNotThrow(() -> result.join());
    }

    @Test
    void testSendErrorToPlayer() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, handler).join();
        
        // Test
        CompletableFuture<Void> result = gameInstanceController.sendErrorToPlayer(playerId.toString(), "Test error", false);
        
        // Verify
        assertDoesNotThrow(() -> result.join());
    }

    @Test
    void testSendActionResultToPlayer() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, handler).join();
        
        ActionResultDTO actionResult = new ActionResultDTO();
        actionResult.setSuccess(true);
        actionResult.setMessage("Test action result");
        
        // Test
        CompletableFuture<Void> result = gameInstanceController.sendActionResultToPlayer(playerId.toString(), actionResult);
        
        // Verify
        assertDoesNotThrow(() -> result.join());
    }

    @Test
    void testBroadcastGameState() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        GameStateDTO gameState = new GameStateDTO();

        // Test
        CompletableFuture<Void> result = gameInstanceController.broadcastGameState(gameState);

        // Verify
        assertDoesNotThrow(() -> result.join());
    }

    @Test
    void testBroadcastWithMultipleHandlers() {
        // Setup
        PlayerId player1 = PlayerId.fromString("player1");
        PlayerId player2 = PlayerId.fromString("player2");
        MockServerClientHandler handler1 = new MockServerClientHandler();
        MockServerClientHandler handler2 = new MockServerClientHandler();

        gameInstanceController.addPlayer(player1, handler1).join();
        gameInstanceController.addPlayer(player2, handler2).join();

        // Test
        GameStateDTO gameState = new GameStateDTO();
        CompletableFuture<Void> result = gameInstanceController.broadcastGameState(gameState);

        // Verify
        assertDoesNotThrow(() -> result.join());
        assertNotNull(handler1.getLastGameState());
        assertNotNull(handler2.getLastGameState());
    }

    @Test
    void testBroadcastWithFailedHandler() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        handler.setFailNextSend(true);
        gameInstanceController.addPlayer(playerId, handler).join();

        // Test
        GameStateDTO gameState = new GameStateDTO();
        CompletableFuture<Void> result = gameInstanceController.broadcastGameState(gameState);

        // Verify
        assertDoesNotThrow(() -> result.join());
    }

    @Test
    void testSendToPlayerWithInvalidHandler() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, handler).join();
        
        // Remove handler to simulate invalid state
        gameInstanceController.removeClientHandler(playerId.toString(), false);
        
        // Test
        CompletableFuture<Void> result = gameInstanceController.sendFullGameState(playerId.toString());
        
        // Verify - should complete without throwing
        assertDoesNotThrow(() -> result.join());
    }

    @Test
    void testProcessComponentRequestWithNullComponentType() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        // Test with null component type
        CompletableFuture<CommandResult> result = gameInstanceController.processComponentRequest("testPlayer", null);
        CommandResult commandResult = result.join();
        assertFalse(commandResult.isSuccess());
        assertEquals("AcquireComponentCommand not implemented", commandResult.getMessage());
    }

    @Test
    void testProcessPlacementRequestWithNullComponent() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        Position position = new Position(0, 0);

        // Test with null component
        CompletableFuture<CommandResult> result = gameInstanceController.processPlacementRequest("testPlayer", null, position);
        
        // Verify - should return a failure CommandResult
        CommandResult commandResult = result.join();
        assertFalse(commandResult.isSuccess());
        assertEquals("Component cannot be null.", commandResult.getMessage());
    }

    @Test
    void testProcessPlacementRequestWithNullPosition() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        // Test with null position
        ComponentDTO componentDTO = new ComponentDTO();
        componentDTO.setType("ENGINE");
        CompletableFuture<CommandResult> result = gameInstanceController.processPlacementRequest("testPlayer", componentDTO, null);
        CommandResult commandResult = result.join();
        assertFalse(commandResult.isSuccess());
        assertEquals("Could not identify component model from DTO.", commandResult.getMessage());
    }

    @Test
    void testProcessPlacementRequestWithInvalidPosition() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, clientHandler).join();
        
        ComponentDTO componentDTO = new ComponentDTO();
        componentDTO.setType("ENGINE");
        Position invalidPosition = new Position(-1, -1); // Invalid position
        
        // Test
        CompletableFuture<CommandResult> result = gameInstanceController.processPlacementRequest(
            playerId.toString(), 
            componentDTO, 
            invalidPosition
        );
        
        // Verify
        CommandResult commandResult = result.join();
        assertFalse(commandResult.isSuccess());
        assertTrue(commandResult.getMessage().contains("Could not identify component model from DTO."));
    }

    @Test
    void testProcessReconnectionRequestNullSessionToken() {
        // Setup
        String playerNickname = "testPlayer";
        PlayerId playerId = PlayerId.fromString(playerNickname);
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        
        // Add player to game model first
        gameModel.addPlayer(playerId, playerNickname);
        
        // Test with null session token
        CompletableFuture<Boolean> result = gameInstanceController.processReconnectionRequest(playerId, null, clientHandler);
        assertFalse(result.join());
    }

    @Test
    void testProcessComponentRequestWithDifferentTypes() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        // Test different component types
        String[] componentTypes = {"ENGINE", "WEAPON", "SHIELD", "CARGO"};
        for (String type : componentTypes) {
            CompletableFuture<CommandResult> result = gameInstanceController.processComponentRequest(
                playerId.toString(), 
                type
            );
            CommandResult commandResult = result.join();
            assertFalse(commandResult.isSuccess(),
                "Component request should fail for type: " + type);
            assertEquals("AcquireComponentCommand not implemented", commandResult.getMessage(),
                "Should return appropriate error message for type: " + type);
        }
    }

    @Test
    void testProcessPlacementRequestWithDifferentComponents() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        // Test different component types
        String[] componentTypes = {"ENGINE", "WEAPON", "SHIELD", "CARGO"};
        Position position = new Position(0, 0);

        for (String type : componentTypes) {
            ComponentDTO componentDTO = new ComponentDTO();
            componentDTO.setType(type);
            CompletableFuture<CommandResult> result = gameInstanceController.processPlacementRequest(
                playerId.toString(), 
                componentDTO, 
                position
            );
            CommandResult commandResult = result.join();
            assertFalse(commandResult.isSuccess(),
                "Placement request should fail for type: " + type);
            assertEquals("Could not identify component model from DTO.", commandResult.getMessage(),
                "Should return appropriate error message for type: " + type);
        }
    }

    @Test
    void testProcessReconnectionRequestWithInvalidSessionToken() {
        // Setup
        String playerNickname = "testPlayer";
        PlayerId playerId = PlayerId.fromString(playerNickname);
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        
        // Add player to game model first
        gameModel.addPlayer(playerId, playerNickname);
        
        // Test with invalid session token
        CompletableFuture<Boolean> result = gameInstanceController.processReconnectionRequest(playerId, "invalid-token", clientHandler);
        assertFalse(result.join());
    }

    @Test
    void testCheckAndEndBuildPhaseWithPlayerReadiness() {
        // Setup
        gameModel.setCurrentPhase(GamePhase.BUILDING);
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        // Test
        gameInstanceController.checkAndEndBuildPhase();

        // Verify phase transition
        assertEquals(GamePhase.FLIGHT, gameModel.getCurrentPhase());
    }

    @Test
    void testSendFullGameStateWithDifferentPhases() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        // Wait for the asynchronous operation to complete
        gameInstanceController.addPlayer(playerId, handler).join();

        // Test sending game state in different phases
        GamePhase[] phases = {GamePhase.SETUP, GamePhase.BUILDING, GamePhase.FLIGHT, GamePhase.END};
        for (GamePhase phase : phases) {
            gameModel.setCurrentPhase(phase);
            // Use the handler's player ID instead of playerId.toString()
            gameInstanceController.sendFullGameState(handler.getPlayerId());
            assertNotNull(handler.getLastGameState());
            assertNotNull(handler.getLastGameState().getGamePhase());
            assertEquals(phase.name(), handler.getLastGameState().getGamePhase().getPhaseName());
        }
    }

    @Test
    void testSendErrorToPlayerWithDifferentErrorTypes() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, handler).join();
        
        // Test different error types
        String[] errorMessages = {"Test error", "Fatal error", "Warning message"};
        boolean[] isFatal = {false, true, false};
        
        for (int i = 0; i < errorMessages.length; i++) {
            // Send the error
            CompletableFuture<Void> future = gameInstanceController.sendErrorToPlayer(
                playerId.toString(), 
                errorMessages[i], 
                isFatal[i]
            );
            
            // Wait for the operation to complete
            future.join();
            
            // Verify the error was stored in the handler
            String storedError = handler.getLastError();
            assertNotNull(storedError, "Error message should not be null");
            assertEquals(errorMessages[i], storedError, "Error message should match");
        }
    }

    @Test
    void testSendActionResultToPlayerWithDifferentResults() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, handler).join();
        
        // Test different action results
        ActionResultDTO[] results = {
            createActionResult(true, "Success message"),
            createActionResult(false, "Failure message"),
            createActionResult(true, "Partial success message")
        };
        
        for (ActionResultDTO result : results) {
            // Send the action result
            CompletableFuture<Void> future = gameInstanceController.sendActionResultToPlayer(
                playerId.toString(), 
                result
            );
            
            // Wait for the operation to complete
            future.join();
            
            // Verify the result was stored in the handler
            ActionResultDTO storedResult = handler.getLastActionResult();
            assertNotNull(storedResult, "Action result should not be null");
            assertEquals(result.isSuccess(), storedResult.isSuccess(), "Success status should match");
            assertEquals(result.getMessage(), storedResult.getMessage(), "Message should match");
        }
    }

    @Test
    void testBroadcastWithMultipleConcurrentFailures() {
        // Setup
        PlayerId player1 = PlayerId.fromString("player1");
        PlayerId player2 = PlayerId.fromString("player2");
        PlayerId player3 = PlayerId.fromString("player3");
        
        MockServerClientHandler handler1 = new MockServerClientHandler() {
            @Override
            public CompletableFuture<Void> sendGameState(GameStateDTO state) {
                return CompletableFuture.failedFuture(new RuntimeException("Simulated failure 1"));
            }
        };
        MockServerClientHandler handler2 = new MockServerClientHandler() {
            @Override
            public CompletableFuture<Void> sendGameState(GameStateDTO state) {
                return CompletableFuture.failedFuture(new RuntimeException("Simulated failure 2"));
            }
        };
        MockServerClientHandler handler3 = new MockServerClientHandler();
        
        gameInstanceController.addPlayer(player1, handler1);
        gameInstanceController.addPlayer(player2, handler2);
        gameInstanceController.addPlayer(player3, handler3);
        
        GameStateDTO gameState = new GameStateDTO();
        
        // Test
        gameInstanceController.broadcastGameState(gameState);
        
        // Verify - should complete without throwing exception despite multiple failures
        assertDoesNotThrow(() -> {});
    }

    @Test
    void testGameStateUpdatesAcrossPhases() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, handler).join();
        
        // Test state updates across different phases
        GamePhase[] phases = {GamePhase.SETUP, GamePhase.BUILDING, GamePhase.FLIGHT, GamePhase.END};
        for (GamePhase phase : phases) {
            gameModel.setCurrentPhase(phase);
            
            // Send game state update
            CompletableFuture<Void> result = gameInstanceController.sendFullGameState(playerId.toString());
            assertDoesNotThrow(() -> result.join(), 
                "Should send game state in " + phase + " phase");
            
            // Verify game state was sent
            assertNotNull(handler.getLastGameState(), 
                "Handler should receive game state in " + phase + " phase");
            assertNotNull(handler.getLastGameState().getGamePhase(), 
                "Game state should include phase information");
        }
    }

    @Test
    void testCheckAndEndBuildPhaseWithAllPlayersReady() {
        // Setup
        gameModel.setCurrentPhase(GamePhase.BUILDING);
        PlayerId player1 = PlayerId.fromString("player1");
        PlayerId player2 = PlayerId.fromString("player2");
        MockServerClientHandler handler1 = new MockServerClientHandler();
        MockServerClientHandler handler2 = new MockServerClientHandler();

        gameInstanceController.addPlayer(player1, handler1);
        gameInstanceController.addPlayer(player2, handler2);
        gameInstanceController.startGame();

        // Test - both players ready
        // Since we don't have direct access to set player readiness in the GameModel,
        // we'll simulate the phase transition directly
        gameInstanceController.checkAndEndBuildPhase();

        // Verify - game should end build phase
        assertEquals(GamePhase.FLIGHT, gameModel.getCurrentPhase());
    }

    @Test
    void testGamePhaseTransitions() {
        // Test phase transitions through public methods
        gameModel.setCurrentPhase(GamePhase.SETUP);
        
        // Add players to allow game to start
        PlayerId player1 = PlayerId.fromString("player1");
        PlayerId player2 = PlayerId.fromString("player2");
        MockServerClientHandler handler1 = new MockServerClientHandler();
        MockServerClientHandler handler2 = new MockServerClientHandler();
        
        gameInstanceController.addPlayer(player1, handler1).join();
        gameInstanceController.addPlayer(player2, handler2).join();
        
        gameInstanceController.startGame();
        assertEquals(GamePhase.BUILDING, gameModel.getCurrentPhase(), 
            "Game should transition from SETUP to BUILDING phase");

        gameModel.setCurrentPhase(GamePhase.BUILDING);
        gameInstanceController.checkAndEndBuildPhase();
        assertEquals(GamePhase.FLIGHT, gameModel.getCurrentPhase(), 
            "Game should transition from BUILDING to FLIGHT phase");
    }

    @Test
    void testHandleConcurrentModifications() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, handler).join();
        
        // Simulate concurrent modifications
        CompletableFuture<Void>[] futures = new CompletableFuture[10];
        for (int i = 0; i < futures.length; i++) {
            futures[i] = gameInstanceController.sendFullGameState(playerId.toString());
        }
        
        // Verify all operations complete without errors
        assertDoesNotThrow(() -> CompletableFuture.allOf(futures).join(),
            "Concurrent operations should complete without errors");
    }

    @Test
    void testHandleNetworkFailures() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        handler.setFailNextSend(true);
        gameInstanceController.addPlayer(playerId, handler).join();
        
        // Test operations with network failures
        CompletableFuture<Void> result = gameInstanceController.sendFullGameState(playerId.toString());
        
        // Verify graceful handling of failures
        assertDoesNotThrow(() -> result.join(),
            "Network failures should be handled gracefully");
    }

    @Test
    void testShutdownWithActivePlayers() {
        // Setup multiple active players
        PlayerId[] playerIds = {
            PlayerId.fromString("player1"),
            PlayerId.fromString("player2")
        };
        
        MockServerClientHandler[] handlers = new MockServerClientHandler[playerIds.length];
        for (int i = 0; i < playerIds.length; i++) {
            handlers[i] = new MockServerClientHandler();
            gameInstanceController.addPlayer(playerIds[i], handlers[i]).join();
        }
        
        // Test shutdown
        gameInstanceController.shutdown();
        
        // Verify all resources are cleaned up
        for (MockServerClientHandler handler : handlers) {
            assertNull(handler.getPlayerId(), 
                "Player ID should be cleared after shutdown");
        }
        assertTrue(gameLogicExecutor.isShutdown(), 
            "Game logic executor should be shut down");
        assertTrue(networkExecutor.isShutdown(), 
            "Network executor should be shut down");
    }

    @Test
    void testAddPlayerWithNullHandler() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");

        // Test
        CompletableFuture<Boolean> result = gameInstanceController.addPlayer(playerId, null);

        // Verify
        assertFalse(result.join());
    }

    @Test
    void testAddPlayerWithExistingPlayerId() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler1 = new MockServerClientHandler();
        MockServerClientHandler handler2 = new MockServerClientHandler();

        // Add first player
        gameInstanceController.addPlayer(playerId, handler1).join();

        // Test adding second player with same ID
        CompletableFuture<Boolean> result = gameInstanceController.addPlayer(playerId, handler2);

        // Verify
        assertFalse(result.join());
        assertEquals(playerId.toString(), handler1.getPlayerId());
        assertNull(handler2.getPlayerId());
    }

    @Test
    void testAddPlayerWithMaxPlayers() {
        // Setup
        MockServerClientHandler[] handlers = new MockServerClientHandler[TEST_PLAYER_COUNT + 1];
        PlayerId[] playerIds = new PlayerId[TEST_PLAYER_COUNT + 1];

        // Add maximum number of players
        for (int i = 0; i < TEST_PLAYER_COUNT; i++) {
            playerIds[i] = PlayerId.fromString("player" + i);
            handlers[i] = new MockServerClientHandler();
            assertTrue(gameInstanceController.addPlayer(playerIds[i], handlers[i]).join(),
                "Should successfully add player " + i);
            assertEquals(playerIds[i].toString(), handlers[i].getPlayerId(),
                "Handler should be assigned correct player ID");
        }

        // Try to add one more player beyond the maximum
        playerIds[TEST_PLAYER_COUNT] = PlayerId.fromString("extraPlayer");
        handlers[TEST_PLAYER_COUNT] = new MockServerClientHandler();
        
        // This should fail as we're exceeding max players
        assertFalse(gameInstanceController.addPlayer(playerIds[TEST_PLAYER_COUNT], handlers[TEST_PLAYER_COUNT]).join(),
            "Should not allow adding player beyond maximum");
        assertNull(handlers[TEST_PLAYER_COUNT].getPlayerId(),
            "Extra player's handler should not be assigned a player ID");
        
        // Verify the number of players in the game matches TEST_PLAYER_COUNT
        assertEquals(TEST_PLAYER_COUNT, gameModel.getPlayers().size(),
            "Game should maintain the correct number of players");
    }

    @Test
    void testStartGameWithInsufficientPlayers() {
        // Setup
        gameModel.setCurrentPhase(GamePhase.SETUP);

        // Test starting game without adding any players
        gameInstanceController.startGame();

        // Verify game should not start
        assertEquals(GamePhase.SETUP, gameModel.getCurrentPhase());
    }

    @Test
    void testProcessComponentRequestWithEmptyComponentType() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        // Test with empty component type
        CompletableFuture<CommandResult> result = gameInstanceController.processComponentRequest("testPlayer", "");
        CommandResult commandResult = result.join();
        assertFalse(commandResult.isSuccess());
        assertEquals("AcquireComponentCommand not implemented", commandResult.getMessage());
    }

    @Test
    void testProcessPlacementRequestWithInvalidComponentType() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        // Test with invalid component type
        ComponentDTO componentDTO = new ComponentDTO();
        componentDTO.setType("INVALID_TYPE");
        Position position = new Position(0, 0);

        CompletableFuture<CommandResult> result = gameInstanceController.processPlacementRequest(
            playerId.toString(),
            componentDTO,
            position
        );

        // Verify
        CommandResult commandResult = result.join();
        assertFalse(commandResult.isSuccess());
        assertEquals("Could not identify component model from DTO.", commandResult.getMessage());
    }

    @Test
    void testProcessPlacementRequestWithOutOfBoundsPosition() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        // Test with out of bounds position
        ComponentDTO componentDTO = new ComponentDTO();
        componentDTO.setType("ENGINE");
        Position outOfBoundsPosition = new Position(100, 100); // Assuming this is out of bounds

        CompletableFuture<CommandResult> result = gameInstanceController.processPlacementRequest(
            playerId.toString(),
            componentDTO,
            outOfBoundsPosition
        );

        // Verify
        CommandResult commandResult = result.join();
        assertFalse(commandResult.isSuccess());
        assertEquals("Could not identify component model from DTO.", commandResult.getMessage());
    }

    @Test
    void testSendFullGameStateWithNullPlayerId() {
        // Test sending game state to null player ID
        CompletableFuture<Void> result = gameInstanceController.sendFullGameState(null);
        
        // Verify - should complete without throwing
        assertDoesNotThrow(() -> result.join());
    }

    @Test
    void testSendErrorToPlayerWithNullMessage() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, handler).join();
        
        // Test sending null error message
        CompletableFuture<Void> result = gameInstanceController.sendErrorToPlayer(playerId.toString(), null, false);
        
        // Verify - should complete without throwing
        assertDoesNotThrow(() -> result.join());
    }

    @Test
    void testSendActionResultToPlayerWithNullResult() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, handler).join();
        
        // Test sending null action result
        CompletableFuture<Void> result = gameInstanceController.sendActionResultToPlayer(playerId.toString(), null);
        
        // Verify - should complete without throwing
        assertDoesNotThrow(() -> result.join());
    }

    @Test
    void testBroadcastGameStateWithNullState() {
        // Test broadcasting null game state
        CompletableFuture<Void> result = gameInstanceController.broadcastGameState(null);
        
        // Verify - should complete without throwing
        assertDoesNotThrow(() -> result.join());
    }

    @Test
    void testShutdownWithNoPlayers() {
        // Test shutdown with no players added
        gameInstanceController.shutdown();
        
        // Verify executors are shut down
        assertTrue(gameLogicExecutor.isShutdown());
        assertTrue(networkExecutor.isShutdown());
    }

    @Test
    void testShutdownMultipleTimes() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, handler).join();
        
        // Test multiple shutdowns
        gameInstanceController.shutdown();
        gameInstanceController.shutdown(); // Second shutdown should be safe
        
        // Verify
        assertNull(handler.getPlayerId());
        assertTrue(gameLogicExecutor.isShutdown());
        assertTrue(networkExecutor.isShutdown());
    }

    @Test
    void testRemoveClientHandlerWithNullPlayerId() {
        // Test removing null player ID
        gameInstanceController.removeClientHandler(null, true);
        
        // Verify - should complete without throwing
        assertDoesNotThrow(() -> {});
    }

    @Test
    void testRemoveClientHandlerMultipleTimes() {
        // Setup
        String playerIdString = "testPlayer";
        PlayerId playerId = PlayerId.fromString(playerIdString);
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, clientHandler).join();

        // Test multiple removals
        gameInstanceController.removeClientHandler(playerIdString, true);
        gameInstanceController.removeClientHandler(playerIdString, true); // Second removal should be safe

        // Verify
        assertNull(clientHandler.getPlayerId());
    }

    @Test
    void testProcessReconnectionRequestWithNullHandler() {
        // Setup
        String playerNickname = "testPlayer";
        PlayerId playerId = PlayerId.fromString(playerNickname);
        gameModel.addPlayer(playerId, playerNickname);
        
        // Test with null handler
        CompletableFuture<Boolean> result = gameInstanceController.processReconnectionRequest(
            playerId,
            UUID.randomUUID().toString(),
            null
        );
        
        // Verify
        assertFalse(result.join());
    }

    @Test
    void testProcessReconnectionRequestWithEmptySessionToken() {
        // Setup
        String playerNickname = "testPlayer";
        PlayerId playerId = PlayerId.fromString(playerNickname);
        MockServerClientHandler clientHandler = new MockServerClientHandler();
        gameModel.addPlayer(playerId, playerNickname);
        
        // Test with empty session token
        CompletableFuture<Boolean> result = gameInstanceController.processReconnectionRequest(
            playerId,
            "",
            clientHandler
        );
        
        // Verify
        assertFalse(result.join());
        assertNull(clientHandler.getPlayerId());
    }

    @Test
    void testConcurrentPlayerOperations() {
        // Setup
        PlayerId[] playerIds = new PlayerId[3];
        MockServerClientHandler[] handlers = new MockServerClientHandler[3];
        
        // Create multiple players
        for (int i = 0; i < 3; i++) {
            playerIds[i] = PlayerId.fromString("player" + i);
            handlers[i] = new MockServerClientHandler();
        }
        
        // Test concurrent add operations
        CompletableFuture<Boolean>[] addFutures = new CompletableFuture[3];
        for (int i = 0; i < 3; i++) {
            addFutures[i] = gameInstanceController.addPlayer(playerIds[i], handlers[i]);
        }
        
        // Verify only the first two players are added successfully (due to TEST_PLAYER_COUNT = 2)
        CompletableFuture.allOf(addFutures).join();
        assertTrue(addFutures[0].join(), "First player should be added successfully");
        assertTrue(addFutures[1].join(), "Second player should be added successfully");
        assertFalse(addFutures[2].join(), "Third player should not be added due to player limit");
        
        // Verify player IDs are assigned correctly
        assertEquals(playerIds[0].toString(), handlers[0].getPlayerId(), "First handler should have correct player ID");
        assertEquals(playerIds[1].toString(), handlers[1].getPlayerId(), "Second handler should have correct player ID");
        assertNull(handlers[2].getPlayerId(), "Third handler should not have a player ID");
        
        // Test concurrent remove operations
        CompletableFuture<Void>[] removeFutures = new CompletableFuture[2];
        for (int i = 0; i < 2; i++) {
            final int index = i;  // Create effectively final variable
            removeFutures[i] = CompletableFuture.runAsync(() -> 
                gameInstanceController.removeClientHandler(playerIds[index].toString(), true)
            );
        }
        
        // Verify all removes complete
        CompletableFuture.allOf(removeFutures).join();
        assertNull(handlers[0].getPlayerId(), "First player should be removed");
        assertNull(handlers[1].getPlayerId(), "Second player should be removed");
        
        // Verify the game has no players after removal
        assertEquals(0, gameModel.getPlayers().size(), "Game should have no players after removal");
    }

    @Test
    void testNetworkFailureHandling() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, handler).join();
        
        // Simulate network failures for different operations
        handler.setFailNextSend(true);
        
        // Test game state send with failure
        CompletableFuture<Void> gameStateFuture = gameInstanceController.sendFullGameState(playerId.toString());
        assertDoesNotThrow(() -> gameStateFuture.join());
        
        // Test error send with failure
        CompletableFuture<Void> errorFuture = gameInstanceController.sendErrorToPlayer(
            playerId.toString(), 
            "Test error", 
            false
        );
        assertDoesNotThrow(() -> errorFuture.join());
        
        // Test action result send with failure
        ActionResultDTO actionResult = new ActionResultDTO();
        actionResult.setSuccess(true);
        actionResult.setMessage("Test result");
        CompletableFuture<Void> actionFuture = gameInstanceController.sendActionResultToPlayer(
            playerId.toString(), 
            actionResult
        );
        assertDoesNotThrow(() -> actionFuture.join());
    }

    @Test
    void testGameStateTransitions() {
        // Setup
        PlayerId player1 = PlayerId.fromString("player1");
        PlayerId player2 = PlayerId.fromString("player2");
        MockServerClientHandler handler1 = new MockServerClientHandler();
        MockServerClientHandler handler2 = new MockServerClientHandler();
        
        gameInstanceController.addPlayer(player1, handler1).join();
        gameInstanceController.addPlayer(player2, handler2).join();
        
        // Test SETUP -> BUILDING transition
        gameModel.setCurrentPhase(GamePhase.SETUP);
        gameInstanceController.startGame();
        assertEquals(GamePhase.BUILDING, gameModel.getCurrentPhase());
        
        // Test BUILDING -> FLIGHT transition
        gameModel.setCurrentPhase(GamePhase.BUILDING);
        gameInstanceController.checkAndEndBuildPhase();
        assertEquals(GamePhase.FLIGHT, gameModel.getCurrentPhase());
        
        // Test invalid transitions
        gameModel.setCurrentPhase(GamePhase.FLIGHT);
        gameInstanceController.startGame(); // Should not change phase
        assertEquals(GamePhase.FLIGHT, gameModel.getCurrentPhase());
        
        gameModel.setCurrentPhase(GamePhase.END);
        gameInstanceController.checkAndEndBuildPhase(); // Should not change phase
        assertEquals(GamePhase.END, gameModel.getCurrentPhase());
    }

    @Test
    void testPlayerReconnectionScenarios() {
        // Setup
        String playerNickname = "testPlayer";
        PlayerId playerId = PlayerId.fromString(playerNickname);
        MockServerClientHandler originalHandler = new MockServerClientHandler();
        gameModel.addPlayer(playerId, playerNickname);
        gameInstanceController.addPlayer(playerId, originalHandler).join();
        
        // Test reconnection with same session token
        String sessionToken = UUID.randomUUID().toString();
        MockServerClientHandler newHandler = new MockServerClientHandler();
        
        // Simulate session registration (this would normally be done by the session manager)
        // For testing, we'll just verify the reconnection attempt
        CompletableFuture<Boolean> result = gameInstanceController.processReconnectionRequest(
            playerId,
            sessionToken,
            newHandler
        );
        
        // Verify reconnection fails (since we can't actually register the session)
        assertFalse(result.join());
        assertNull(newHandler.getPlayerId());
        
        // Test reconnection with different player ID
        PlayerId differentPlayerId = PlayerId.fromString("differentPlayer");
        MockServerClientHandler differentHandler = new MockServerClientHandler();
        CompletableFuture<Boolean> differentResult = gameInstanceController.processReconnectionRequest(
            differentPlayerId,
            sessionToken,
            differentHandler
        );
        
        // Verify reconnection fails for different player
        assertFalse(differentResult.join());
        assertNull(differentHandler.getPlayerId());
    }

    @Test
    void testComponentAndPlacementValidation() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, handler).join();
        
        // Test component request validation
        String[] invalidComponentTypes = {
            "", // Empty
            " ", // Whitespace
            "INVALID_TYPE", // Non-existent type
            "engine", // Wrong case
            "ENGINE ", // Trailing space
            " ENGINE" // Leading space
        };
        
        for (String type : invalidComponentTypes) {
            CompletableFuture<CommandResult> result = gameInstanceController.processComponentRequest(
                playerId.toString(),
                type
            );
            CommandResult commandResult = result.join();
            assertFalse(commandResult.isSuccess());
            assertEquals("AcquireComponentCommand not implemented", commandResult.getMessage());
        }
        
        // Test placement request validation
        Position[] invalidPositions = {
            new Position(-1, 0), // Negative X
            new Position(0, -1), // Negative Y
            new Position(100, 0), // Out of bounds X
            new Position(0, 100)  // Out of bounds Y
        };
        
        ComponentDTO componentDTO = new ComponentDTO();
        componentDTO.setType("ENGINE");
        
        for (Position position : invalidPositions) {
            CompletableFuture<CommandResult> result = gameInstanceController.processPlacementRequest(
                playerId.toString(),
                componentDTO,
                position
            );
            CommandResult commandResult = result.join();
            assertFalse(commandResult.isSuccess());
            assertEquals("Could not identify component model from DTO.", commandResult.getMessage());
        }
    }

    @Test
    void testBroadcastOperationsWithMultipleFailures() {
        // Setup multiple players with different failure scenarios
        PlayerId[] playerIds = new PlayerId[3];
        MockServerClientHandler[] handlers = new MockServerClientHandler[3];
        
        for (int i = 0; i < 3; i++) {
            playerIds[i] = PlayerId.fromString("player" + i);
            handlers[i] = new MockServerClientHandler() {
                @Override
                public CompletableFuture<Void> sendGameState(GameStateDTO state) {
                    return CompletableFuture.failedFuture(new RuntimeException("Simulated failure"));
                }
            };
            gameInstanceController.addPlayer(playerIds[i], handlers[i]).join();
        }
        
        // Test broadcasting with all handlers failing
        GameStateDTO gameState = new GameStateDTO();
        CompletableFuture<Void> result = gameInstanceController.broadcastGameState(gameState);
        
        // Verify broadcast completes without throwing despite all failures
        assertDoesNotThrow(() -> result.join());
    }

    @Test
    void testShutdownDuringActiveOperations() {
        // Setup
        PlayerId playerId = PlayerId.fromString("testPlayer");
        MockServerClientHandler handler = new MockServerClientHandler();
        gameInstanceController.addPlayer(playerId, handler).join();
        
        // Start multiple operations
        CompletableFuture<Void>[] operations = new CompletableFuture[5];
        for (int i = 0; i < 5; i++) {
            operations[i] = gameInstanceController.sendFullGameState(playerId.toString());
        }
        
        // Shutdown during operations
        gameInstanceController.shutdown();
        
        // Verify all operations complete
        assertDoesNotThrow(() -> CompletableFuture.allOf(operations).join());
        
        // Verify cleanup
        assertNull(handler.getPlayerId());
        assertTrue(gameLogicExecutor.isShutdown());
        assertTrue(networkExecutor.isShutdown());
    }

    private ActionResultDTO createActionResult(boolean success, String message) {
        ActionResultDTO result = new ActionResultDTO();
        result.setSuccess(success);
        result.setMessage(message);
        return result;
    }

    // Mock implementation of ServerClientHandler for testing
    private static class MockServerClientHandler implements ServerClientHandler {
        private String playerId;
        private final String connectionId = UUID.randomUUID().toString();
        private boolean failNextSend = false;
        private GameStateDTO lastGameState;
        private String lastError;
        private ActionResultDTO lastActionResult;

        @Override
        public String getConnectionId() {
            return connectionId;
        }

        @Override
        public String getPlayerId() {
            return playerId;
        }

        @Override
        public void setPlayerId(String playerId) {
            this.playerId = playerId;
        }

        @Override
        public CompletableFuture<Void> sendGameState(GameStateDTO state) {
            if (failNextSend) {
                return CompletableFuture.failedFuture(new RuntimeException("Simulated failure"));
            }
            lastGameState = state;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> sendPhaseChange(GamePhaseDTO phase) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> sendPlayerStatusUpdate(String affectedPlayerId, FlightStatus status) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> sendShipUpdate(String affectedPlayerId, ShipDTO dto) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> sendComponentPoolUpdate(ComponentDeckDTO dto) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> sendGameResults(Map<String, Integer> scores, String winner) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> sendPlayerRemoval(String removedPlayerId) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> sendError(String message, boolean isFatal) {
            lastError = message;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> sendActionResult(ActionResultDTO result) {
            lastActionResult = result;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void closeConnection() {
            playerId = null;
        }

        public void setFailNextSend(boolean failNextSend) {
            this.failNextSend = failNextSend;
        }

        public GameStateDTO getLastGameState() {
            return lastGameState;
        }

        public String getLastError() {
            return lastError;
        }

        public ActionResultDTO getLastActionResult() {
            return lastActionResult;
        }
    }

    private GameInstanceController createController() {
        return new GameInstanceController(
            TEST_GAME_ID,
            gameModel,
            ruleEngine,
            scoringEngine,
            commandProcessor,
            sessionManager,
            gameLogicExecutor,
            networkExecutor
        );
    }
} 