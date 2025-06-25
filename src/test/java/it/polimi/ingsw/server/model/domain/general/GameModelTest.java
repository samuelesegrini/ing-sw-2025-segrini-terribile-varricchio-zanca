//package it.polimi.ingsw.server.model.domain.general;
//
//import it.polimi.ingsw.server.model.domain.general.GameModel;
//import it.polimi.ingsw.server.model.domain.general.config.GameConfig;
//import it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager;
//import it.polimi.ingsw.server.model.domain.player.PlayerId;
//import it.polimi.ingsw.server.model.enums.GameLevel;
//import it.polimi.ingsw.server.model.enums.GamePhase;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class GameModelTest {
//
//    private GameModel gameModel;
//    private GameConfigurationManager configManager;
//    private GameConfig config;
//
//    @BeforeEach
//    void setUp() {
//        configManager = new GameConfigurationManager();
//        config = configManager.getConfigForLevel(GameLevel.LEVEL_II);
//        gameModel = new GameModel(GameLevel.LEVEL_II, configManager, 2);
//    }
//
//    @Test
//    void constructor_WithInvalidPlayerCount_ShouldThrowException() {
//        assertThrows(IllegalArgumentException.class, () ->
//            new GameModel(GameLevel.LEVEL_II, configManager, 1));
//        assertThrows(IllegalArgumentException.class, () ->
//            new GameModel(GameLevel.LEVEL_II, configManager, 5));
//    }
//
//    @Test
//    void constructor_WithValidPlayerCount_ShouldInitializeCorrectly() {
//        assertEquals(GamePhase.SETUP, gameModel.getCurrentPhase());
//        assertNotNull(gameModel.getGameId());
//        assertEquals(GameLevel.LEVEL_II, gameModel.getLevel());
//        assertTrue(gameModel.getPlayers().isEmpty());
//    }
//
//    @Test
//    void addPlayer_InSetupPhase_ShouldAddPlayer() {
//        PlayerId playerId = PlayerId.fromString("test-player");
//        gameModel.addPlayer(playerId, "Test Player");
//
//        assertEquals(1, gameModel.getPlayers().size());
//        assertNotNull(gameModel.getPlayerById(playerId));
//    }
//
//    @Test
//    void addPlayer_AfterGameStarted_ShouldThrowException() {
//        gameModel.changePhase(GamePhase.BUILDING);
//        PlayerId playerId = PlayerId.fromString("test-player");
//
//        assertThrows(IllegalStateException.class, () ->
//            gameModel.addPlayer(playerId, "Test Player"));
//    }
//
//    @Test
//    void addPlayer_WithDuplicateId_ShouldThrowException() {
//        PlayerId playerId = PlayerId.fromString("test-player");
//        gameModel.addPlayer(playerId, "Test Player");
//
//        assertThrows(IllegalArgumentException.class, () ->
//            gameModel.addPlayer(playerId, "Another Player"));
//    }
//
//    @Test
//    void addPlayer_ExceedingMaxPlayers_ShouldThrowException() {
//        for (int i = 0; i < 2; i++) {
//            gameModel.addPlayer(PlayerId.fromString("player-" + i), "Player " + i);
//        }
//
//        assertThrows(IllegalStateException.class, () ->
//            gameModel.addPlayer(PlayerId.fromString("extra-player"), "Extra Player"));
//    }
//
//    @Test
//    void initializeGame_WithEnoughPlayers_ShouldThrowUnsupportedOperationException() {
//        // Add required players
//        gameModel.addPlayer(PlayerId.fromString("player-1"), "Player 1");
//        gameModel.addPlayer(PlayerId.fromString("player-2"), "Player 2");
//
//        // Initialize the game should throw UnsupportedOperationException
//        assertThrows(UnsupportedOperationException.class, () ->
//            gameModel.initializeGame(),
//            "Game initialization should throw UnsupportedOperationException since createDeckForGameLevel is not implemented");
//    }
//
//    @Test
//    void initializeGame_WithoutEnoughPlayers_ShouldThrowException() {
//        gameModel.addPlayer(PlayerId.fromString("player-1"), "Player 1");
//
//        assertThrows(IllegalStateException.class, () ->
//            gameModel.initializeGame());
//    }
//
//    @Test
//    void startGame_WhenInitialized_ShouldThrowUnsupportedOperationException() {
//        // Add players and initialize
//        gameModel.addPlayer(PlayerId.fromString("player-1"), "Player 1");
//        gameModel.addPlayer(PlayerId.fromString("player-2"), "Player 2");
//
//        // Initialize the game should throw UnsupportedOperationException
//        assertThrows(UnsupportedOperationException.class, () ->
//            gameModel.initializeGame(),
//            "Game initialization should throw UnsupportedOperationException since createDeckForGameLevel is not implemented");
//    }
//
//    @Test
//    void startGame_WhenNotInitialized_ShouldThrowException() {
//        assertThrows(IllegalStateException.class, () ->
//            gameModel.startGame());
//    }
//
//    @Test
//    void startGame_WhenAlreadyStarted_ShouldThrowUnsupportedOperationException() {
//        // Add players and initialize
//        gameModel.addPlayer(PlayerId.fromString("player-1"), "Player 1");
//        gameModel.addPlayer(PlayerId.fromString("player-2"), "Player 2");
//
//        // Initialize the game should throw UnsupportedOperationException
//        assertThrows(UnsupportedOperationException.class, () ->
//            gameModel.initializeGame(),
//            "Game initialization should throw UnsupportedOperationException since createDeckForGameLevel is not implemented");
//    }
//
//    @Test
//    void nextPhase_ShouldTransitionToNextPhase() {
//        gameModel.changePhase(GamePhase.SETUP);
//        gameModel.nextPhase();
//        assertEquals(GamePhase.BUILDING, gameModel.getCurrentPhase());
//
//        gameModel.nextPhase();
//        assertEquals(GamePhase.FLIGHT, gameModel.getCurrentPhase());
//
//        gameModel.nextPhase();
//        assertEquals(GamePhase.END, gameModel.getCurrentPhase());
//    }
//
//    @Test
//    void getCurrentPlayer_ShouldReturnCurrentPlayer() {
//        PlayerId playerId = PlayerId.fromString("player-1");
//        gameModel.addPlayer(playerId, "Player 1");
//        gameModel.addPlayer(PlayerId.fromString("player-2"), "Player 2");
//
//        assertEquals(playerId, gameModel.getCurrentPlayer().getId());
//    }
//
//    @Test
//    void nextPlayer_ShouldRotateToNextPlayer() {
//        PlayerId player1Id = PlayerId.fromString("player-1");
//        PlayerId player2Id = PlayerId.fromString("player-2");
//
//        gameModel.addPlayer(player1Id, "Player 1");
//        gameModel.addPlayer(player2Id, "Player 2");
//
//        assertEquals(player1Id, gameModel.getCurrentPlayer().getId());
//        gameModel.nextPlayer();
//        assertEquals(player2Id, gameModel.getCurrentPlayer().getId());
//        gameModel.nextPlayer();
//        assertEquals(player1Id, gameModel.getCurrentPlayer().getId());
//    }
//
//    @Test
//    void removePlayer_ShouldRemovePlayerSuccessfully() {
//        PlayerId playerId = PlayerId.fromString("player-1");
//        gameModel.addPlayer(playerId, "Player 1");
//
//        assertTrue(gameModel.removePlayer(playerId));
//        assertNull(gameModel.getPlayerById(playerId));
//    }
//
//    @Test
//    void removePlayer_WithNonexistentPlayer_ShouldReturnFalse() {
//        PlayerId nonexistentId = PlayerId.fromString("nonexistent");
//        assertFalse(gameModel.removePlayer(nonexistentId));
//    }
//}