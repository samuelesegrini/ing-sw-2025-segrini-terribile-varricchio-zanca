package it.polimi.ingsw.server.game;

import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.flight.FlightStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class MultiGameCoordinatorTest {

    private ExecutorService gameLogicExecutor;
    private ExecutorService networkExecutor;
    private TestSessionManager sessionManager;
    private TestServerClientHandler clientHandler;
    private MultiGameCoordinator coordinator;

    @BeforeEach
    void setUp() {
        gameLogicExecutor = Executors.newSingleThreadExecutor();
        networkExecutor = Executors.newSingleThreadExecutor();
        sessionManager = new TestSessionManager(playerId -> {});
        clientHandler = new TestServerClientHandler();
        coordinator = new MultiGameCoordinator(gameLogicExecutor, networkExecutor, sessionManager);
    }

    @Test
    void testCreateGame() {
        // Test creating a game with a specific ID
        String gameId = "test-game-1";
        GameInstanceController controller = coordinator.createGame(gameId, 2);
        assertNotNull(controller);
        assertEquals(gameId, controller.getGameId());

        // Test creating a game with generated ID
        GameInstanceController autoIdController = coordinator.createGame(null, 2);
        assertNotNull(autoIdController);
        assertNotNull(autoIdController.getGameId());

        // Test creating a game with duplicate ID
        GameInstanceController duplicateController = coordinator.createGame(gameId, 2);
        assertNull(duplicateController);
    }

    @Test
    void testGetGameInstance() {
        // Create a game first
        String gameId = "test-game-2";
        GameInstanceController created = coordinator.createGame(gameId, 2);
        assertNotNull(created);

        // Test getting existing game
        GameInstanceController retrieved = coordinator.getGameInstance(gameId);
        assertNotNull(retrieved);
        assertEquals(created, retrieved);

        // Test getting non-existent game
        GameInstanceController nonExistent = coordinator.getGameInstance("non-existent");
        assertNull(nonExistent);
    }

    @Test
    void testJoinOrReconnectGame() {
        // Create a game first
        String gameId = "test-game-3";
        coordinator.createGame(gameId, 2);

        // Test joining a game
        String playerName = "player1";
        sessionManager.nextSessionInfo = new TestSessionInfo(PlayerId.fromString(playerName), gameId);
        
        CompletableFuture<Boolean> joinFuture = coordinator.joinOrReconnectGame(gameId, playerName, null, clientHandler);
        assertTrue(joinFuture.join());

        // Test joining non-existent game
        CompletableFuture<Boolean> nonExistentFuture = coordinator.joinOrReconnectGame("non-existent", "player2", null, clientHandler);
        assertThrows(Exception.class, () -> nonExistentFuture.join());

        // Test reconnecting to a game
        String sessionToken = UUID.randomUUID().toString();
        sessionManager.nextReconnectionValid = true;
        
        CompletableFuture<Boolean> reconnectFuture = coordinator.joinOrReconnectGame(gameId, playerName, sessionToken, clientHandler);
        assertTrue(reconnectFuture.join());
    }

    @Test
    void testRemoveGame() {
        // Create a game first
        String gameId = "test-game-4";
        GameInstanceController game = coordinator.createGame(gameId, 2);
        assertNotNull(game);

        // Test removing existing game
        coordinator.removeGame(gameId);
        assertNull(coordinator.getGameInstance(gameId));

        // Test removing non-existent game - should not throw
        coordinator.removeGame("non-existent");
    }

    @Test
    void testHandleClientDisconnect() {
        // Setup
        String gameId = "test-game";
        PlayerId playerId = PlayerId.fromString("test-player");
        clientHandler.setPlayerId(playerId.toString());
        sessionManager.nextGameId = gameId;
        coordinator.createGame(gameId, 2);

        // Test
        coordinator.handleClientDisconnect(clientHandler);

        // Verify
        assertTrue(sessionManager.disconnectionRegistered);
        assertNotNull(sessionManager.lastDisconnectedPlayer);
        assertEquals("test-player", sessionManager.lastDisconnectedPlayer.getNickname());
    }

    @Test
    void testRouteHeartbeat() {
        // Setup
        PlayerId playerId = PlayerId.fromString("test-player");
        String sessionToken = "test-token";

        // Test
        coordinator.routeHeartbeat(playerId.toString(), sessionToken);

        // Verify
        assertTrue(sessionManager.activityUpdated);
        assertNotNull(sessionManager.lastActivePlayer);
        assertEquals("test-player", sessionManager.lastActivePlayer.getNickname());
        assertEquals(sessionToken, sessionManager.lastSessionToken);
    }

    @Test
    void testShutdown() {
        // Setup
        String gameId = "test-game";
        coordinator.createGame(gameId, 2);

        // Test
        coordinator.shutdown();

        // Verify game was removed
        assertNull(coordinator.getGameInstance(gameId));
    }

    // Test double for SessionManager
    private static class TestSessionManager extends SessionManager {
        boolean disconnectionRegistered = false;
        boolean activityUpdated = false;
        PlayerId lastDisconnectedPlayer = null;
        PlayerId lastActivePlayer = null;
        String lastSessionToken = null;
        String nextGameId = null;
        SessionInfo nextSessionInfo = null;
        boolean nextReconnectionValid = false;

        public TestSessionManager(Consumer<PlayerId> playerRemovalCallback) {
            super(playerRemovalCallback);
        }

        @Override
        public void registerDisconnection(PlayerId playerId) {
            disconnectionRegistered = true;
            lastDisconnectedPlayer = playerId;
        }

        @Override
        public boolean updateLastActivity(PlayerId playerId, String sessionToken) {
            activityUpdated = true;
            lastActivePlayer = playerId;
            lastSessionToken = sessionToken;
            return true;
        }

        @Override
        public String getGameIdForPlayer(PlayerId playerId) {
            return nextGameId;
        }

        @Override
        public SessionInfo registerNewSession(PlayerId playerId, String gameId) {
            return nextSessionInfo;
        }

        @Override
        public boolean validateReconnectionAttempt(PlayerId playerId, String sessionToken) {
            return nextReconnectionValid;
        }
    }

    // Test double for ServerClientHandler
    private static class TestServerClientHandler implements ServerClientHandler {
        private String playerId;
        private String lastErrorMessage;
        private boolean lastErrorFatal;

        @Override
        public String getPlayerId() {
            return playerId;
        }

        @Override
        public void setPlayerId(String id) {
            this.playerId = id;
        }

        @Override
        public String getConnectionId() {
            return "test-connection";
        }

        @Override
        public CompletableFuture<Void> sendError(String message, boolean fatal) {
            this.lastErrorMessage = message;
            this.lastErrorFatal = fatal;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> sendGameState(GameStateDTO state) {
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
        public CompletableFuture<Void> sendActionResult(ActionResultDTO result) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void closeConnection() {
            // No-op for test double
        }
    }

    // Test double for SessionInfo
    private static class TestSessionInfo extends SessionInfo {
        public TestSessionInfo(PlayerId playerId, String gameId) {
            super(playerId, gameId);
        }
    }
} 