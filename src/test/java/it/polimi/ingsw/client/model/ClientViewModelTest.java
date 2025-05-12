package it.polimi.ingsw.client.model;

import it.polimi.ingsw.client.network.ClientNetworkInterface;
import it.polimi.ingsw.client.network.ClientNetworkManager;
import it.polimi.ingsw.common.dto.GameLobbyInfoDTO;
import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.common.message.setup.GameSessionStateChangedEvent;
import it.polimi.ingsw.common.message.system.ServerLoginResponse;
import it.polimi.ingsw.common.model.GameSessionState;
import it.polimi.ingsw.server.model.enums.GameLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ClientViewModel class that maintains client application state
 * and serves as the central model for UI components.
 */
class ClientViewModelTest {

    private EventBus eventBus;
    private ClientViewModel viewModel;
    private ExecutorService executor;
    private TestNetworkManager networkManager;

    // Inner class to mock the ClientNetworkManager
    static class TestNetworkManager extends ClientNetworkManager {
        private boolean connected = false;
        private boolean messageSent = false;
        private Message lastMessage = null;
        private EventBus eventBus;

        public TestNetworkManager(EventBus eventBus) {
            // Use a no-op constructor without calling super
            super(new MockClientNetworkAdapter(), eventBus);
            this.eventBus = eventBus;
        }

        // Mock implementation of ClientNetworkInterface for testing
        private static class MockClientNetworkAdapter implements ClientNetworkInterface {
            @Override
            public void connect(String host, int port) throws IOException {
                // No-op implementation
            }

            @Override
            public void disconnect() {
                // No-op implementation
            }

            @Override
            public boolean isConnected() {
                return false;
            }

            @Override
            public boolean sendMessage(Message message) {
                return true;
            }

            @Override
            public void setOnMessageReceived(Consumer<Message> onMessageReceivedHandler) {
                // No-op implementation
            }

            @Override
            public void setOnDisconnected(Runnable onDisconnectedHandler) {
                // No-op implementation
            }
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public boolean sendMessage(Message message) {
            lastMessage = message;
            messageSent = true;
            return true; // Always return true for tests to succeed
        }

        public void simulateConnection() {
            connected = true;
        }

        public void simulateDisconnection() {
            connected = false;
        }

        public boolean wasMessageSent() {
            return messageSent;
        }

        public Message getLastMessage() {
            Message msg = lastMessage;
            return msg;
        }
        
        // Reset internal state for tests
        public void reset() {
            lastMessage = null;
            messageSent = false;
        }
    }

    @BeforeEach
    void setUp() {
        // Set GUI mode to false for testing (avoid JavaFX dependencies)
        ClientViewModel.setGUIMode(false);
        
        eventBus = new EventBus(2, "test-vm-event-bus");
        viewModel = new ClientViewModel(eventBus);
        
        // Create an executor that runs tasks immediately on the current thread
        executor = new ExecutorService() {
            @Override
            public void execute(Runnable command) {
                command.run(); // Execute immediately on this thread
            }
            
            @Override
            public void shutdown() {}
            
            @Override
            public List<Runnable> shutdownNow() { return List.of(); }
            
            @Override
            public boolean isShutdown() { return false; }
            
            @Override
            public boolean isTerminated() { return false; }
            
            @Override
            public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) { return true; }
            
            @Override
            public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> task) {
                try {
                    T result = task.call(); // Execute immediately
                    return new java.util.concurrent.FutureTask<>(() -> result);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            
            @Override
            public <T> java.util.concurrent.Future<T> submit(Runnable task, T result) {
                task.run(); // Execute immediately
                return new java.util.concurrent.FutureTask<>(() -> result);
            }
            
            @Override
            public java.util.concurrent.Future<?> submit(Runnable task) {
                task.run(); // Execute immediately
                return new java.util.concurrent.FutureTask<>(() -> null);
            }
            
            @Override
            public <T> List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) { 
                return List.of();
            }
            
            @Override
            public <T> List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, java.util.concurrent.TimeUnit unit) {
                return List.of();
            }
            
            @Override
            public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) {
                return null;
            }
            
            @Override
            public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, java.util.concurrent.TimeUnit unit) {
                return null;
            }
        };
        
        viewModel.setBackgroundTaskExecutor(executor);
        
        networkManager = new TestNetworkManager(eventBus);
        viewModel.setNetworkManager(networkManager);
        
        // Simulate connection for tests
        networkManager.simulateConnection();
    }

    // Helper method to set up logged-in state for tests that need it
    private void setupLoggedInState() {
        // Reset message tracking first
        networkManager.reset();
        
        viewModel.handleLoginSuccess("testId", "testUser", "Test login successful");
        
        // Reset message tracking after setting up the state
        networkManager.reset();
    }

    @Test
    void testInitialState() {
        assertEquals(ClientViewModel.AppStatus.LOGIN_SCREEN, viewModel.appStatusProperty().get());
        assertNull(viewModel.loggedInPlayerIdProperty().get());
        assertNull(viewModel.loggedInNicknameProperty().get());
        assertEquals(0, viewModel.getJoinableGames().size());
        assertEquals(0, viewModel.getRunningGames().size());
    }

    @Test
    void testHandleConnectionInitiated() {
        viewModel.handleConnectionInitiated();
        
        assertEquals(ClientViewModel.AppStatus.CONNECTING, viewModel.appStatusProperty().get());
        assertTrue(viewModel.statusMessageProperty().get().contains("Connecting"));
    }

    @Test
    void testHandleConnected() {
        viewModel.handleConnected();
        
        assertEquals(ClientViewModel.AppStatus.LOGGING_IN, viewModel.appStatusProperty().get());
        assertTrue(viewModel.statusMessageProperty().get().contains("Connected"));
    }

    @Test
    void testHandleConnectionFailed() {
        String errorMessage = "Connection timeout";
        viewModel.handleConnectionFailed(errorMessage);
        
        assertEquals(ClientViewModel.AppStatus.NOT_CONNECTED, viewModel.appStatusProperty().get());
        assertTrue(viewModel.statusMessageProperty().get().contains(errorMessage));
    }

    @Test
    void testHandleLoginSuccess() {
        // Call the method we're testing with the runOnUIThread lambda executed immediately
        viewModel.handleLoginSuccess("user123", "testuser", "Login successful");
        
        // Verify direct property state changes only - nothing about networking or background tasks
        assertEquals("user123", viewModel.loggedInPlayerIdProperty().get());
        assertEquals("testuser", viewModel.loggedInNicknameProperty().get());
        assertEquals(ClientViewModel.AppStatus.LOGGED_IN_BROWSING_LOBBIES, viewModel.appStatusProperty().get());
        assertTrue(viewModel.statusMessageProperty().get().contains("successful"));
        
        // Don't test requestGameList() which depends on the networkManager
    }

    @Test
    void testHandleLoginFailure() {
        // First set a logged in state that should be reset
        viewModel.handleLoginSuccess("tempId", "tempUser", "temp");
        
        // Now test failure
        String errorMessage = "Invalid nickname";
        viewModel.handleLoginFailure(errorMessage);
        
        assertEquals(ClientViewModel.AppStatus.LOGIN_SCREEN, viewModel.appStatusProperty().get());
        assertNull(viewModel.loggedInPlayerIdProperty().get());
        assertNull(viewModel.loggedInNicknameProperty().get());
        assertTrue(viewModel.statusMessageProperty().get().contains(errorMessage));
    }

    @Test
    void testHandleDisconnected() {
        // First set a logged in state
        setupLoggedInState();
        
        // Now test disconnection
        String reason = "Connection lost";
        viewModel.handleDisconnected(reason);
        
        assertEquals(ClientViewModel.AppStatus.DISCONNECTED, viewModel.appStatusProperty().get());
        assertNull(viewModel.loggedInPlayerIdProperty().get());
        assertNull(viewModel.loggedInNicknameProperty().get());
        assertTrue(viewModel.statusMessageProperty().get().contains(reason));
    }

    @Test
    void testHandleDisconnectedDuringLogin() {
        // Set connecting state
        viewModel.handleConnectionInitiated();
        
        // Test disconnection during login
        String reason = "Connection timeout";
        viewModel.handleDisconnected(reason);
        
        // Should return to login screen, not disconnected state
        assertEquals(ClientViewModel.AppStatus.LOGIN_SCREEN, viewModel.appStatusProperty().get());
        assertTrue(viewModel.statusMessageProperty().get().contains(reason));
    }

    @Test
    void testGameListUpdates() {
        // We don't need to be logged in for this test
        
        // Create some test data
        GameLobbyInfoDTO lobby1 = new GameLobbyInfoDTO("lobby1", "Game 1", 4, 2, GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        GameLobbyInfoDTO lobby2 = new GameLobbyInfoDTO("lobby2", "Game 2", 4, 4, GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        GameLobbyInfoDTO running1 = new GameLobbyInfoDTO("running1", "Running 1", 4, 3, GameSessionState.SHIP_BUILDING, GameLevel.TEST_FLIGHT);
        
        // Create lists of games
        List<GameLobbyInfoDTO> joinableGames = Arrays.asList(lobby1, lobby2);
        List<GameLobbyInfoDTO> runningGames = Arrays.asList(running1);
        
        // Call the handler method that would be triggered by the message
        viewModel.handleGameListResponse(joinableGames, runningGames);
        
        // Verify lists were updated
        assertEquals(2, viewModel.getJoinableGames().size());
        assertEquals(1, viewModel.getRunningGames().size());
        assertEquals("Game 1", viewModel.getJoinableGames().get(0).getGameName());
        assertEquals("Running 1", viewModel.getRunningGames().get(0).getGameName());
    }

    @Test
    void testJoinGameSessionHandling() {
        // Set up logged in state first
        setupLoggedInState();
        
        // Create test data
        String sessionId = "session123";
        GameLobbyInfoDTO lobbyInfo = new GameLobbyInfoDTO(sessionId, "Test Game", 4, 1, GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        List<PlayerInfoDTO> players = Arrays.asList(
            new PlayerInfoDTO("player1", "User1", false)
        );
        
        // Add the lobbyInfo to joinableGames first so handleJoinGameResponse can find it
        viewModel.handleGameListResponse(List.of(lobbyInfo), List.of());
        
        // Call the handler method that would be triggered by the message
        viewModel.handleJoinGameResponse(true, sessionId, null, players);
        
        // Verify state changes
        assertEquals(ClientViewModel.AppStatus.GAME_LOBBY, viewModel.appStatusProperty().get());
        assertEquals(sessionId, viewModel.currentSessionIdProperty().get());
        assertNotNull(viewModel.currentLobbyInfoProperty().get());
        assertEquals(1, viewModel.getPlayersInCurrentLobby().size());
        assertEquals("User1", viewModel.getPlayersInCurrentLobby().get(0).getNickname());
    }

    @Test
    void testPlayerJoinedNotification() {
        // Set up logged in state first
        setupLoggedInState();
        
        // First join a game session
        String sessionId = "session123";
        List<PlayerInfoDTO> initialPlayers = List.of(new PlayerInfoDTO("player1", "User1", false));
        
        // Setup the initial state
        viewModel.handleJoinGameResponse(true, sessionId, null, initialPlayers);
        
        // Create a new player that joins
        PlayerInfoDTO newPlayer = new PlayerInfoDTO("player2", "User2", false);
        
        // Updated player list with the new player
        List<PlayerInfoDTO> updatedPlayers = Arrays.asList(
            new PlayerInfoDTO("player1", "User1", true),
            newPlayer
        );
        
        // Call the handler that would be triggered by the message
        viewModel.handlePlayerJoinedNotification(sessionId, newPlayer, updatedPlayers);
        
        // Verify updates
        assertEquals(2, viewModel.getPlayersInCurrentLobby().size());
        assertTrue(viewModel.getPlayersInCurrentLobby().get(0).isReady());
        assertEquals("User2", viewModel.getPlayersInCurrentLobby().get(1).getNickname());
    }

    @Test
    void testGameSessionStateChange() {
        // Set up logged in state first
        setupLoggedInState();
        
        // First join a game session
        String sessionId = "session123";
        List<PlayerInfoDTO> players = List.of(
            new PlayerInfoDTO("player1", "User1", true),
            new PlayerInfoDTO("player2", "User2", true)
        );
        
        // Setup the initial state
        viewModel.handleJoinGameResponse(true, sessionId, null, players);
        
        // Now change to ship building phase by simulating a GameSessionStateChangedEvent
        viewModel.handleGameSessionStateChanged(
            sessionId,
            GameSessionState.LOBBY.name(),
            GameSessionState.SHIP_BUILDING.name(),
            players
        );
        
        // Verify state changes
        assertEquals(ClientViewModel.AppStatus.GAME_BUILDING, viewModel.appStatusProperty().get());
    }

    @Test
    void testLeaveGameSession() {
        // Set up logged in state first
        setupLoggedInState();
        
        // First join a game session
        String sessionId = "session123";
        List<PlayerInfoDTO> players = List.of(new PlayerInfoDTO("player1", "User1", false));
        viewModel.handleJoinGameResponse(true, sessionId, null, players);
        
        // Now leave the session - this directly calls the method that UI would trigger
        viewModel.leaveGame();
        
        // Verify state changes
        assertEquals(ClientViewModel.AppStatus.LOGGED_IN_BROWSING_LOBBIES, viewModel.appStatusProperty().get());
        assertNull(viewModel.currentSessionIdProperty().get());
        assertNull(viewModel.currentLobbyInfoProperty().get());
        assertEquals(0, viewModel.getPlayersInCurrentLobby().size());
    }

    @Test
    void testRequestGameList() {
        // Set up logged in state first
        setupLoggedInState();
        
        // Reset message tracking
        networkManager.reset();
        
        viewModel.requestGameList();
        
        assertTrue(networkManager.wasMessageSent(), "A message should have been sent to request game list");
    }

    @Test
    void testCreateNewGame() {
        // Set up logged in state first
        setupLoggedInState();
        
        // Reset message tracking
        networkManager.reset();
        
        viewModel.createGame(new it.polimi.ingsw.common.dto.GameSettingsDTO(
            "New Game", 4, GameLevel.TEST_FLIGHT
        ));
        
        assertTrue(networkManager.wasMessageSent());
    }
    
    @Test
    void testJoinGame() {
        // Set up logged in state first
        setupLoggedInState();
        
        // Reset message tracking
        networkManager.reset();
        
        viewModel.joinGame("lobby123");
        
        assertTrue(networkManager.wasMessageSent());
    }
    
    @Test
    void testLeaveGame() {
        // Set up logged in state first
        setupLoggedInState();
        
        // First join a game
        String sessionId = "session123";
        List<PlayerInfoDTO> players = List.of(new PlayerInfoDTO("player1", "User1", false));
        viewModel.handleJoinGameResponse(true, sessionId, null, players);
        
        // Reset message tracking
        networkManager.reset();
        
        viewModel.leaveGame();
        
        assertTrue(networkManager.wasMessageSent(), "A message should have been sent to request leaving the game");
    }
    
    @Test
    void testSetPlayerReady() {
        // Set up logged in state first
        setupLoggedInState();
        
        // First join a game
        String sessionId = "session123";
        List<PlayerInfoDTO> players = List.of(new PlayerInfoDTO("player1", "User1", false));
        viewModel.handleJoinGameResponse(true, sessionId, null, players);
        
        // Reset message tracking
        networkManager.reset();
        
        viewModel.setPlayerReady(true);
        
        assertTrue(networkManager.wasMessageSent());
    }
    
    @Test
    void testStartGame() {
        // Set up logged in state first
        setupLoggedInState();
        
        // First join a game
        String sessionId = "session123";
        List<PlayerInfoDTO> players = List.of(new PlayerInfoDTO("player1", "User1", false));
        viewModel.handleJoinGameResponse(true, sessionId, null, players);
        
        // Reset message tracking
        networkManager.reset();
        
        viewModel.startGame();
        
        assertTrue(networkManager.wasMessageSent(), "A message should have been sent to request starting the game");
    }
    
    @Test
    void testSetAppStatus() {
        viewModel.setAppStatus(ClientViewModel.AppStatus.GAME_FINISHED);
        
        assertEquals(ClientViewModel.AppStatus.GAME_FINISHED, viewModel.appStatusProperty().get());
    }
    
    @Test
    void testSetGUIMode() {
        // Test static GUI mode setting
        ClientViewModel.setGUIMode(true);
        assertTrue(ClientViewModel.isGUIMode());
        
        ClientViewModel.setGUIMode(false);
        assertFalse(ClientViewModel.isGUIMode());
    }
} 