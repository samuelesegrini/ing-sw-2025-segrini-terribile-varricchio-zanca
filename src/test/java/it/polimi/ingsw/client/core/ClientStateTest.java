package it.polimi.ingsw.client.core;

import it.polimi.ingsw.client.ui.core.UIContext;
import it.polimi.ingsw.common.model.GameInfo;
import it.polimi.ingsw.server.model.domain.adventure.AdventureDeck;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.general.config.GameConfig;
import it.polimi.ingsw.server.model.domain.general.config.ShipGridConfig;
import it.polimi.ingsw.server.model.domain.general.config.FlightBoardConfig;
import it.polimi.ingsw.server.model.domain.general.config.RewardSystemConfig;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.player.PlayerColor;
import it.polimi.ingsw.client.ui.core.UIView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClientState Test Suite")
class ClientStateTest {

    private ClientState clientState;
    private TestUIView testUIComponent;
    private static boolean javaFXInitialized = false;

    @BeforeAll
    static void initializeJavaFX() {
        try {
            // First check if JavaFX is available
            Class.forName("javafx.application.Platform");

            // Try to use Platform.runLater to check if toolkit is initialized
            try {
                javafx.application.Platform.runLater(() -> {});
                javaFXInitialized = true;
                System.out.println("JavaFX toolkit already initialized");
            } catch (IllegalStateException e) {
                // Toolkit not initialized, try to initialize it
                try {
                    // Create a JFXPanel to initialize the toolkit
                    Class<?> jfxPanelClass = Class.forName("javafx.embed.swing.JFXPanel");
                    jfxPanelClass.getDeclaredConstructor().newInstance();

                    // Wait a bit for initialization
                    Thread.sleep(100);

                    // Test if it works now
                    javafx.application.Platform.runLater(() -> {});
                    javaFXInitialized = true;
                    System.out.println("JavaFX toolkit initialized via JFXPanel");
                } catch (Exception initException) {
                    System.err.println("Failed to initialize JavaFX toolkit: " + initException.getMessage());
                    javaFXInitialized = false;
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("JavaFX not available on classpath - skipping JavaFX-dependent tests");
            javaFXInitialized = false;
        } catch (Exception e) {
            System.err.println("Unexpected error during JavaFX initialization: " + e.getMessage());
            javaFXInitialized = false;
        }
    }

    static boolean isJavaFXInitialized() {
        return javaFXInitialized;
    }

    @BeforeEach
    void setUp() {
        clientState = new ClientState();
        testUIComponent = new TestUIView();
    }

    @Test
    @DisplayName("Initial state should be correct")
    void testInitialState() {
        assertEquals(ClientState.ConnectionStatus.DISCONNECTED, clientState.getConnectionStatus());
        assertEquals(ClientState.ViewState.CONNECTION, clientState.getCurrentView());
        assertNull(clientState.getPlayerId());
        assertNull(clientState.getPlayerNickname());
        assertNull(clientState.getGameModel());
        assertFalse(clientState.isConnected());
        assertFalse(clientState.isAuthenticated());
        assertFalse(clientState.isInGame());
    }

    @Test
    @DisplayName("Connection status management")
    void testConnectionStatus() {
        // Test connecting
        clientState.setConnectionStatus(ClientState.ConnectionStatus.CONNECTING);
        assertEquals(ClientState.ConnectionStatus.CONNECTING, clientState.getConnectionStatus());
        assertFalse(clientState.isConnected());

        // Test connected
        clientState.setConnectionStatus(ClientState.ConnectionStatus.CONNECTED);
        assertEquals(ClientState.ConnectionStatus.CONNECTED, clientState.getConnectionStatus());
        assertTrue(clientState.isConnected());

        // Only test JavaFX properties if initialized
        if (javaFXInitialized) {
            try {
                assertTrue(clientState.connectedProperty().get());
            } catch (Exception e) {
                System.out.println("JavaFX property test skipped: " + e.getMessage());
            }
        }

        // Test failed
        clientState.setConnectionStatus(ClientState.ConnectionStatus.FAILED);
        assertEquals(ClientState.ConnectionStatus.FAILED, clientState.getConnectionStatus());
        assertFalse(clientState.isConnected());

        // Test disconnected
        clientState.setConnectionStatus(ClientState.ConnectionStatus.DISCONNECTED);
        assertEquals(ClientState.ConnectionStatus.DISCONNECTED, clientState.getConnectionStatus());
        assertFalse(clientState.isConnected());

        if (javaFXInitialized) {
            try {
                assertFalse(clientState.connectedProperty().get());
            } catch (Exception e) {
                System.out.println("JavaFX property test skipped: " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("Player info management")
    void testPlayerInfo() {
        PlayerId playerId = new PlayerId(UUID.randomUUID(), "testPlayer");
        String nickname = "TestPlayer";

        // Test setting player info
        clientState.setPlayerInfo(playerId, nickname);

        assertEquals(playerId.toString(), clientState.getPlayerId());
        assertEquals(playerId, clientState.getPlayerIdObject());
        assertEquals(nickname, clientState.getPlayerNickname());
        assertEquals(nickname, clientState.getNickname());
        assertEquals(nickname, clientState.getCurrentNickname());
        assertTrue(clientState.isAuthenticated());
        assertTrue(clientState.isLoggedIn());

        if (javaFXInitialized) {
            try {
                assertTrue(clientState.authenticatedProperty().get());
            } catch (Exception e) {
                System.out.println("JavaFX property test skipped: " + e.getMessage());
            }
        }

        // Test clearing player info - without JavaFX dependencies
        clientState.setPlayerInfo(null, null);
        assertNull(clientState.getPlayerId());
        assertNull(clientState.getPlayerNickname());
        assertFalse(clientState.isAuthenticated());
    }

    @Test
    @DisplayName("Available games management")
    void testAvailableGames() {
        List<GameInfo> games = Arrays.asList(
                new SimpleGameInfo("game1", true, false),  // joinable
                new SimpleGameInfo("game2", false, true),  // started
                new SimpleGameInfo("game3", false, false)  // full
        );

        clientState.setAvailableGames(games);
        assertEquals(games, clientState.getAvailableGames());

        // Test filters
        List<GameInfo> joinableGames = clientState.getJoinableGames();
        assertEquals(1, joinableGames.size());
        assertEquals("game1", joinableGames.get(0).getGameId());

        List<GameInfo> gamesInProgress = clientState.getGamesInProgress();
        assertEquals(1, gamesInProgress.size());
        assertEquals("game2", gamesInProgress.get(0).getGameId());

        // Test with null
        clientState.setAvailableGames(null);
        assertTrue(clientState.getJoinableGames().isEmpty());
        assertTrue(clientState.getGamesInProgress().isEmpty());
    }

    @Test
    @DisplayName("Current game basic info management")
    void testCurrentGameBasicInfo() {
        String gameId = "test-game-123";
        String gameName = "Test Game";
        GameLevel gameLevel = GameLevel.LEVEL_II;
        int maxPlayers = 4;

        clientState.setCurrentGameId(gameId);
        clientState.setCurrentGameName(gameName);
        clientState.setCurrentGameLevel(gameLevel);
        clientState.setCurrentGameMaxPlayers(maxPlayers);

        assertEquals(gameId, clientState.getCurrentGameId());
        assertEquals(gameName, clientState.getCurrentGameName());
        assertEquals(gameLevel, clientState.getCurrentGameLevel());
        assertEquals(maxPlayers, clientState.getCurrentGameMaxPlayers());
    }

    @Test
    @DisplayName("Current game lobby management")
    void testCurrentGameLobby() {
        SimpleGameModel gameModel = new SimpleGameModel();

        clientState.setCurrentGameLobby(gameModel);
        assertEquals(gameModel, clientState.getCurrentGameLobby());
        assertEquals(gameModel, clientState.getCurrentGameInfo());

        // Test getCurrentGameId when lobby is set
        assertEquals("test-game", clientState.getCurrentGameId());
    }

    @Test
    @DisplayName("Game model management")
    void testGameModel() {
        assertFalse(clientState.isInGame());
        assertNull(clientState.getCurrentGame());

        SimpleGameModel gameModel = new SimpleGameModel();
        clientState.setGameModel(gameModel);

        assertTrue(clientState.isInGame());
        assertEquals(gameModel, clientState.getGameModel());
        assertEquals(gameModel, clientState.getCurrentGame());
        assertEquals(GamePhase.SETUP, clientState.getCurrentPhase());

        // Test clearing game model
        clientState.setGameModel(null);
        assertFalse(clientState.isInGame());
        assertNull(clientState.getGameModel());
        assertEquals(GamePhase.SETUP, clientState.getCurrentPhase()); // Default when no game
    }

    @Test
    @DisplayName("Players in lobby management")
    void testPlayersInLobby() {
        SimplePlayer player1 = new SimplePlayer("player1", false);
        SimplePlayer player2 = new SimplePlayer("player2", true);
        List<Player> players = Arrays.asList(player1, player2);

        clientState.setPlayersInLobby(players);
        assertEquals(players, clientState.getPlayersInLobby());

        // Test player ready status
        String player1Id = player1.getId().toString();
        String player2Id = player2.getId().toString();

        assertFalse(clientState.isPlayerReady(player1Id));
        assertTrue(clientState.isPlayerReady(player2Id));
    }

    @Test
    @DisplayName("Player ready status management")
    void testPlayerReadyStatus() {
        SimplePlayer player = new SimplePlayer("test-player", false);
        List<Player> players = Arrays.asList(player);

        clientState.setPlayersInLobby(players);

        String playerId = player.getId().toString();

        // Initially not ready
        assertFalse(clientState.isPlayerReady(playerId));

        // Set ready using different methods
        clientState.setPlayerReadyStatus(playerId, true);
        assertTrue(clientState.isPlayerReady(playerId));

        clientState.setPlayerReady(playerId, false);
        assertFalse(clientState.isPlayerReady(playerId));

        clientState.updatePlayerReadyStatus(playerId, true);
        assertTrue(clientState.isPlayerReady(playerId));

        // Test with non-existent player
        assertFalse(clientState.isPlayerReady("non-existent"));
    }

    @Test
    @DisplayName("Player ready status in game lobby")
    void testPlayerReadyStatusInGameLobby() {
        SimplePlayer player = new SimplePlayer("lobby-player", false);
        SimpleGameModel gameModel = new SimpleGameModel();
        gameModel.setTestPlayers(Arrays.asList(player));

        clientState.setCurrentGameLobby(gameModel);

        String playerId = player.getId().toString();
        assertFalse(clientState.isPlayerReady(playerId));

        clientState.setPlayerReadyStatus(playerId, true);
        assertTrue(clientState.isPlayerReady(playerId));
    }

    @Test
    @DisplayName("View state management")
    void testViewState() {
        assertEquals(ClientState.ViewState.CONNECTION, clientState.getCurrentView());

        // Test setting view state only
        clientState.setCurrentView(ClientState.ViewState.LOGIN);
        assertEquals(ClientState.ViewState.LOGIN, clientState.getCurrentView());

        // Test setting view state with component
        clientState.setCurrentView(ClientState.ViewState.LOBBY, testUIComponent);
        assertEquals(ClientState.ViewState.LOBBY, clientState.getCurrentView());

        // Test all view states
        for (ClientState.ViewState viewState : ClientState.ViewState.values()) {
            clientState.setCurrentView(viewState);
            assertEquals(viewState, clientState.getCurrentView());
        }
    }

    @Test
    @DisplayName("UI view registration and refresh")
    void testUIViewManagement() {
        TestUIView view1 = new TestUIView();
        TestUIView view2 = new TestUIView();

        // Register views
        clientState.registerRefreshableView(view1);
        clientState.registerRefreshableView(view2);

        int initialCount1 = view1.getRefreshCount();
        int initialCount2 = view2.getRefreshCount();

        // Trigger refresh by changing state
        clientState.setConnectionStatus(ClientState.ConnectionStatus.CONNECTED);

        assertTrue(view1.getRefreshCount() > initialCount1);
        assertTrue(view2.getRefreshCount() > initialCount2);

        // Test unregister
        clientState.unregisterRefreshableView(view1);

        int beforeCount1 = view1.getRefreshCount();
        int beforeCount2 = view2.getRefreshCount();

        clientState.setConnectionStatus(ClientState.ConnectionStatus.DISCONNECTED);

        // view1 should not refresh, view2 should
        assertEquals(beforeCount1, view1.getRefreshCount());
        assertTrue(view2.getRefreshCount() > beforeCount2);
    }

    @Test
    @DisplayName("Local player operations")
    void testLocalPlayerOperations() {
        // Without game model
        assertNull(clientState.getLocalPlayer());
        assertNull(clientState.getLocalPlayerShip());

        // Set up game with local player
        PlayerId localPlayerId = new PlayerId(UUID.randomUUID(), "localPlayer");
        clientState.setPlayerInfo(localPlayerId, "Local Player");

        SimplePlayer localPlayer = new SimplePlayer("local", true);
        SimpleGameModel gameModel = new SimpleGameModel();
        gameModel.setLocalPlayerId(localPlayerId);
        gameModel.setTestPlayers(Arrays.asList(localPlayer));

        clientState.setGameModel(gameModel);

        // Test local player ship update
        SimpleShip ship = new SimpleShip();
        clientState.updateLocalPlayerShip(ship);
        // Should not throw exception
    }

    @Test
    @DisplayName("Component deck operations")
    void testComponentDeckOperations() {
        // Without game model
        assertNull(clientState.getComponentDeck());

        // With game model
        SimpleGameModel gameModel = new SimpleGameModel();
        clientState.setGameModel(gameModel);
        assertNotNull(clientState.getComponentDeck());

        // Update component deck
        SimpleComponentDeck newDeck = new SimpleComponentDeck();
        clientState.updateComponentDeck(newDeck);
        // Should not throw exception
    }

    @Test
    @DisplayName("Player update operations")
    void testPlayerUpdateOperations() {
        SimplePlayer player1 = new SimplePlayer("player1", false);
        SimplePlayer player2 = new SimplePlayer("player2", true);
        SimpleGameModel gameModel = new SimpleGameModel();
        gameModel.setTestPlayers(Arrays.asList(player1, player2));

        clientState.setGameModel(gameModel);

        // Update a player
        SimplePlayer updatedPlayer = new SimplePlayer("player1", true);
        updatedPlayer.setPlayerId(player1.getId()); // Same ID
        clientState.updatePlayer(updatedPlayer);
        // Should not throw exception
    }

    @Test
    @DisplayName("Ship validation")
    void testShipValidation() {
        // Test valid ship
        List<String> noErrors = Collections.emptyList();
        assertDoesNotThrow(() -> clientState.setShipValidation(true, noErrors));

        // Test invalid ship
        List<String> errors = Arrays.asList("Error 1", "Error 2");
        assertDoesNotThrow(() -> clientState.setShipValidation(false, errors));
    }

    @Test
    @DisplayName("JavaFX properties")
    @EnabledIf("isJavaFXInitialized")
    void testJavaFXProperties() {
        try {
            // Test initial values
            assertFalse(clientState.connectedProperty().get());
            assertFalse(clientState.authenticatedProperty().get());

            // Test connection property
            clientState.setConnectionStatus(ClientState.ConnectionStatus.CONNECTED);

            // Use Platform.runLater and wait for execution
            CountDownLatch latch1 = new CountDownLatch(1);
            javafx.application.Platform.runLater(() -> {
                try {
                    assertTrue(clientState.connectedProperty().get());
                } finally {
                    latch1.countDown();
                }
            });
            assertTrue(latch1.await(5, TimeUnit.SECONDS), "Timeout waiting for JavaFX thread");

            clientState.setConnectionStatus(ClientState.ConnectionStatus.DISCONNECTED);

            CountDownLatch latch2 = new CountDownLatch(1);
            javafx.application.Platform.runLater(() -> {
                try {
                    assertFalse(clientState.connectedProperty().get());
                } finally {
                    latch2.countDown();
                }
            });
            assertTrue(latch2.await(5, TimeUnit.SECONDS), "Timeout waiting for JavaFX thread");

            // Test authentication property
            PlayerId playerId = new PlayerId(UUID.randomUUID(), "test");
            clientState.setPlayerInfo(playerId, "TestPlayer");

            CountDownLatch latch3 = new CountDownLatch(1);
            javafx.application.Platform.runLater(() -> {
                try {
                    assertTrue(clientState.authenticatedProperty().get());
                } finally {
                    latch3.countDown();
                }
            });
            assertTrue(latch3.await(5, TimeUnit.SECONDS), "Timeout waiting for JavaFX thread");

            // Clear authentication
            clientState.setPlayerInfo(null, null);

            CountDownLatch latch4 = new CountDownLatch(1);
            javafx.application.Platform.runLater(() -> {
                try {
                    assertFalse(clientState.authenticatedProperty().get());
                } finally {
                    latch4.countDown();
                }
            });
            assertTrue(latch4.await(5, TimeUnit.SECONDS), "Timeout waiting for JavaFX thread");

        } catch (Exception e) {
            fail("JavaFX property test failed with exception: " + e.getMessage(), e);
        }
    }
    @Test
    @DisplayName("Null handling and edge cases")
    void testNullHandling() {
        // Null handling should be safe
        assertDoesNotThrow(() -> clientState.registerRefreshableView(null));
        assertDoesNotThrow(() -> clientState.unregisterRefreshableView(null));

        // Operations with null should be safe
        assertDoesNotThrow(() -> clientState.setAvailableGames(null));
        assertDoesNotThrow(() -> clientState.setPlayersInLobby(null));
        assertDoesNotThrow(() -> clientState.setCurrentGameLobby(null));
        assertDoesNotThrow(() -> clientState.setGameModel(null));

        // Operations with null player should be safe
        assertDoesNotThrow(() -> clientState.updatePlayer(null));
        assertDoesNotThrow(() -> clientState.updateLocalPlayerShip(null));
        assertDoesNotThrow(() -> clientState.updateComponentDeck(null));

        // Default values when no data
        assertEquals(GamePhase.SETUP, clientState.getCurrentPhase());
        assertNull(clientState.getComponentDeck());
        assertNull(clientState.getLocalPlayer());
        assertNull(clientState.getLocalPlayerShip());
    }

    @Test
    @DisplayName("Game ID priority - basic info vs lobby")
    void testGameIdPriority() {
        // Initially null
        assertNull(clientState.getCurrentGameId());

        // Set basic game ID
        clientState.setCurrentGameId("basic-game-id");
        assertEquals("basic-game-id", clientState.getCurrentGameId());

        // Set game lobby - should override basic ID
        SimpleGameModel gameModel = new SimpleGameModel();
        clientState.setCurrentGameLobby(gameModel);
        assertEquals("test-game", clientState.getCurrentGameId()); // From game model

        // Clear lobby - should fall back to basic ID
        clientState.setCurrentGameLobby(null);
        assertEquals("basic-game-id", clientState.getCurrentGameId());
    }

    @Test
    @DisplayName("Authentication state consistency")
    void testAuthenticationStateConsistency() {
        assertFalse(clientState.isAuthenticated());
        assertFalse(clientState.isLoggedIn());

        // Set player info
        PlayerId playerId = new PlayerId(UUID.randomUUID(), "test");
        clientState.setPlayerInfo(playerId, "Test");
        assertTrue(clientState.isAuthenticated());
        assertTrue(clientState.isLoggedIn());

        // Clear authentication
        clientState.setPlayerInfo(null, null);
        assertFalse(clientState.isAuthenticated());
        assertFalse(clientState.isLoggedIn());
        assertNull(clientState.getPlayerId());
        assertNull(clientState.getNickname());
    }
    // === HELPER CLASSES ===

    private static class TestUIView implements UIView {
        private int refreshCount = 0;

        @Override
        public ClientState.ViewState getViewState() {
            return null;
        }

        @Override
        public String getTitle() {
            return "";
        }

        @Override
        public void initialize(UIContext context) {

        }

        @Override
        public void show() {

        }

        @Override
        public void hide() {

        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public void refresh() {
            refreshCount++;
        }

        @Override
        public void dispose() {

        }

        public int getRefreshCount() {
            return refreshCount;
        }
    }

    private static class SimpleGameInfo extends GameInfo {
        private final boolean joinable;
        private final boolean started;

        public SimpleGameInfo(String id, boolean joinable, boolean started) {
            super(id, "Game " + id, GameLevel.TEST_FLIGHT, 4, 2, GamePhase.SETUP, started, joinable);
            this.joinable = joinable;
            this.started = started;
        }

        @Override
        public boolean isJoinable() { return joinable; }

        @Override
        public boolean isStarted() { return started; }
    }

    private static class SimplePlayer extends Player {
        private boolean ready;
        private PlayerId playerId;

        public SimplePlayer(String id, boolean ready) {
            super(new PlayerId(UUID.randomUUID(), "Player" + id), PlayerColor.RED);
            this.playerId = new PlayerId(UUID.randomUUID(), "Player" + id);
            this.ready = ready;
        }

        @Override
        public boolean isReady() { return ready; }

        @Override
        public void setReady(boolean ready) { this.ready = ready; }

        @Override
        public PlayerId getId() { return playerId; }

        public void setPlayerId(PlayerId playerId) { this.playerId = playerId; }
    }

    private static class SimpleGameModel extends GameModel {
        private List<Player> testPlayers = new ArrayList<>();
        private PlayerId localPlayerId;

        public SimpleGameModel() {
            super(
                    "test-game",
                    "Test Game",
                    GameLevel.TEST_FLIGHT,
                    createDummyConfig(),
                    new SimpleComponentDeck(),
                    createDummyAdventureDeck(),
                    2
            );
        }

        @Override
        public GamePhase getCurrentPhase() { return GamePhase.SETUP; }

        @Override
        public ComponentDeck getComponentDeck() { return new SimpleComponentDeck(); }

        @Override
        public Player getPlayerById(PlayerId playerId) {
            return testPlayers.stream()
                    .filter(p -> p.getId().equals(playerId))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<Player> getPlayers() { return new ArrayList<>(testPlayers); }

        @Override
        public String getGameId() { return "test-game"; }

        public void setTestPlayers(List<Player> players) {
            this.testPlayers = new ArrayList<>(players);
        }

        public void setLocalPlayerId(PlayerId playerId) {
            this.localPlayerId = playerId;
        }

        private static GameConfig createDummyConfig() {
            try {
                return new GameConfig(
                        GameLevel.TEST_FLIGHT,
                        createDummyFlightBoardConfig(),
                        createDummyShipGridConfig()
                );
            } catch (Exception e) {
                return null;
            }
        }

        private static ShipGridConfig createDummyShipGridConfig() {
            try {
                return new ShipGridConfig(
                        "test-image",
                        5,
                        5,
                        new ArrayList<>(),
                        new ArrayList<>()
                );
            } catch (Exception e) {
                return null;
            }
        }

        private static AdventureDeck createDummyAdventureDeck() {
            try {
                return new AdventureDeck(
                        GameLevel.TEST_FLIGHT,
                        new ArrayList<>(),
                        new ArrayList<>()
                );
            } catch (Exception e) {
                return null;
            }
        }

        private static FlightBoardConfig createDummyFlightBoardConfig() {
            try {
                return new FlightBoardConfig(
                        "test-flight-image",
                        "18",
                        Arrays.asList(0, 1, 2),
                        null // RewardSystemConfig può essere null
                );
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static class SimpleShip extends Ship {
        public SimpleShip() {
            super(GameLevel.TEST_FLIGHT, createDummyShipGridConfig());
        }

        private static ShipGridConfig createDummyShipGridConfig() {
            try {
                return new ShipGridConfig(
                        "test-image",
                        5,
                        5,
                        new ArrayList<>(),
                        new ArrayList<>()
                );
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static class SimpleComponentDeck extends ComponentDeck {
        public SimpleComponentDeck() {
            super(new ArrayList<>());
        }
    }
}