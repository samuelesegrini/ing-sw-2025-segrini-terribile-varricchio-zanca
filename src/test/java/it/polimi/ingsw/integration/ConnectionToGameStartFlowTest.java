package it.polimi.ingsw.integration;

import it.polimi.ingsw.common.message.request.RequestContextImpl;
import it.polimi.ingsw.common.message.request.LoginRequest;
import it.polimi.ingsw.common.message.request.CreateGameRequest;
import it.polimi.ingsw.common.message.request.JoinGameRequest;
import it.polimi.ingsw.common.message.request.LeaveGameRequest;
import it.polimi.ingsw.common.message.request.StartGameRequest;
import it.polimi.ingsw.common.message.request.SetPlayerReadyRequest;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.LoginResponse;
import it.polimi.ingsw.common.message.response.CreateGameResponse;
import it.polimi.ingsw.common.message.response.JoinGameResponse;
import it.polimi.ingsw.common.message.response.GenericSuccessResponse;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.EventPublisher;
import it.polimi.ingsw.common.message.event.Event;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.model.enums.GameLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the connection to game start flow.
 * Tests the complete message flow from player login through game start.
 */
class ConnectionToGameStartFlowTest {

    private PlayerSessionRegistry playerRegistry;
    private GameSessionManager gameSessionManager;
    private RequestContextImpl context1;
    private RequestContextImpl context2;
    
    private static final String CLIENT_ID_1 = "test-client-1";
    private static final String CLIENT_ID_2 = "test-client-2";
    private static final String PLAYER_NICKNAME_1 = "Player1";
    private static final String PLAYER_NICKNAME_2 = "Player2";

    @BeforeEach
    void setUp() {
        playerRegistry = new PlayerSessionRegistry();
        gameSessionManager = new GameSessionManager(null, playerRegistry);
        
        // Create mock event publisher that does nothing
        EventPublisher mockEventPublisher = new EventPublisher() {
            @Override
            public void publishEvent(Event event) {
                // Do nothing in tests
            }
            
            @Override
            public void publishEventToGame(Event event, String gameId) {
                // Do nothing in tests
            }
            
            @Override
            public void publishEventToClient(Event event, String clientId) {
                // Do nothing in tests
            }
        };
        
        // Create request contexts for two test clients
        context1 = new RequestContextImpl(CLIENT_ID_1, gameSessionManager, playerRegistry, mockEventPublisher, null, new HashMap<>());
        context2 = new RequestContextImpl(CLIENT_ID_2, gameSessionManager, playerRegistry, mockEventPublisher, null, new HashMap<>());
    }

    @Test
    @DisplayName("Should complete full flow from login to game start")
    void testCompleteConnectionToGameStartFlow() {
        // Step 1: Player 1 logs in
        LoginRequest loginRequest1 = new LoginRequest(PLAYER_NICKNAME_1);
        Response loginResponse1 = loginRequest1.execute(context1);
        
        assertTrue(loginResponse1.isSuccess(), "Player 1 login should succeed");
        assertInstanceOf(LoginResponse.class, loginResponse1);
        
        LoginResponse login1 = (LoginResponse) loginResponse1;
        String playerId1 = login1.getPlayerId();
        assertNotNull(playerId1, "Player 1 should receive a player ID");
        
        // Step 2: Player 2 logs in
        LoginRequest loginRequest2 = new LoginRequest(PLAYER_NICKNAME_2);
        Response loginResponse2 = loginRequest2.execute(context2);
        
        assertTrue(loginResponse2.isSuccess(), "Player 2 login should succeed");
        assertInstanceOf(LoginResponse.class, loginResponse2);
        
        LoginResponse login2 = (LoginResponse) loginResponse2;
        String playerId2 = login2.getPlayerId();
        assertNotNull(playerId2, "Player 2 should receive a player ID");
        assertNotEquals(playerId1, playerId2, "Players should have different IDs");
        
        // Step 3: Player 1 creates a game
        CreateGameRequest createGameRequest = new CreateGameRequest(2, GameLevel.TEST_FLIGHT, "Test Game");
        Response createGameResponse = createGameRequest.execute(context1);
        
        assertTrue(createGameResponse.isSuccess(), "Game creation should succeed");
        assertInstanceOf(CreateGameResponse.class, createGameResponse);
        
        CreateGameResponse createResponse = (CreateGameResponse) createGameResponse;
        String gameId = createResponse.getGameId();
        assertNotNull(gameId, "Created game should have an ID");
        
        // Step 4: Player 2 joins the game
        JoinGameRequest joinGameRequest = new JoinGameRequest(gameId);
        Response joinGameResponse = joinGameRequest.execute(context2);
        
        assertTrue(joinGameResponse.isSuccess(), "Player 2 should successfully join the game");
        assertInstanceOf(JoinGameResponse.class, joinGameResponse);
        
        // Step 5: Player 1 sets ready status
        SetPlayerReadyRequest setReady1 = new SetPlayerReadyRequest(true);
        Response readyResponse1 = setReady1.execute(context1);
        
        assertTrue(readyResponse1.isSuccess(), "Player 1 should successfully set ready status");
        
        // Step 6: Player 2 sets ready status
        SetPlayerReadyRequest setReady2 = new SetPlayerReadyRequest(true);
        Response readyResponse2 = setReady2.execute(context2);
        
        assertTrue(readyResponse2.isSuccess(), "Player 2 should successfully set ready status");
        
        // Step 7: Player 1 starts the game
        StartGameRequest startGameRequest = new StartGameRequest(gameId);
        Response startGameResponse = startGameRequest.execute(context1);
        
        assertTrue(startGameResponse.isSuccess(), "Game should start successfully");
        assertInstanceOf(GenericSuccessResponse.class, startGameResponse);
        
        // Verify game session exists and is started
        var gameSession = gameSessionManager.getGameSession(gameId);
        assertNotNull(gameSession, "Game session should exist");
        assertTrue(gameSession.isStarted(), "Game should be in started state");
    }

    @Test
    @DisplayName("Should fail to start game with only one player")
    void testCannotStartGameWithOnePlayer() {
        // Step 1: Player 1 logs in
        LoginRequest loginRequest = new LoginRequest(PLAYER_NICKNAME_1);
        loginRequest.execute(context1);
        
        // Step 2: Player 1 creates a game
        CreateGameRequest createGameRequest = new CreateGameRequest(2, GameLevel.TEST_FLIGHT, "Test Game");
        CreateGameResponse createResponse = (CreateGameResponse) createGameRequest.execute(context1);
        
        // Step 3: Player 1 sets ready status
        SetPlayerReadyRequest setReady = new SetPlayerReadyRequest(true);
        setReady.execute(context1);
        
        // Step 4: Try to start game with only one player
        String gameId = createResponse.getGameId();
        StartGameRequest startGameRequest = new StartGameRequest(gameId);
        Response startGameResponse = startGameRequest.execute(context1);
        
        assertFalse(startGameResponse.isSuccess(), "Game should not start with only one player");
        assertInstanceOf(ErrorResponse.class, startGameResponse);
    }

    @Test
    @DisplayName("Should fail to start game when players are not ready")
    void testCannotStartGameWhenPlayersNotReady() {
        // Step 1: Both players log in
        LoginRequest loginRequest1 = new LoginRequest(PLAYER_NICKNAME_1);
        loginRequest1.execute(context1);
        
        LoginRequest loginRequest2 = new LoginRequest(PLAYER_NICKNAME_2);
        loginRequest2.execute(context2);
        
        // Step 2: Player 1 creates a game
        CreateGameRequest createGameRequest = new CreateGameRequest(2, GameLevel.TEST_FLIGHT, "Test Game");
        CreateGameResponse createResponse = (CreateGameResponse) createGameRequest.execute(context1);
        
        // Step 3: Player 2 joins the game
        JoinGameRequest joinGameRequest = new JoinGameRequest(createResponse.getGameId());
        joinGameRequest.execute(context2);
        
        // Step 4: Only Player 1 sets ready status (Player 2 remains not ready)
        SetPlayerReadyRequest setReady1 = new SetPlayerReadyRequest(true);
        setReady1.execute(context1);
        
        // Step 5: Try to start game when not all players are ready
        String gameId = createResponse.getGameId();
        StartGameRequest startGameRequest = new StartGameRequest(gameId);
        Response startGameResponse = startGameRequest.execute(context1);
        
        assertFalse(startGameResponse.isSuccess(), "Game should not start when not all players are ready");
        assertInstanceOf(ErrorResponse.class, startGameResponse);
    }

    @Test
    @DisplayName("Should prevent duplicate nicknames during login")
    void testDuplicateNicknamesPrevented() {
        // Step 1: Player 1 logs in
        LoginRequest loginRequest1 = new LoginRequest(PLAYER_NICKNAME_1);
        Response loginResponse1 = loginRequest1.execute(context1);
        
        assertTrue(loginResponse1.isSuccess(), "First player with nickname should succeed");
        
        // Step 2: Try to log in second player with same nickname
        LoginRequest loginRequest2 = new LoginRequest(PLAYER_NICKNAME_1);
        Response loginResponse2 = loginRequest2.execute(context2);
        
        assertFalse(loginResponse2.isSuccess(), "Second player with same nickname should fail");
        assertInstanceOf(ErrorResponse.class, loginResponse2);
    }

    @Test
    @DisplayName("Should handle player leaving during lobby phase")
    void testPlayerLeavingDuringLobby() {
        // Step 1: Both players log in and join game
        LoginRequest loginRequest1 = new LoginRequest(PLAYER_NICKNAME_1);
        loginRequest1.execute(context1);
        
        LoginRequest loginRequest2 = new LoginRequest(PLAYER_NICKNAME_2);
        loginRequest2.execute(context2);
        
        CreateGameRequest createGameRequest = new CreateGameRequest(2, GameLevel.TEST_FLIGHT, "Test Game");
        CreateGameResponse createResponse = (CreateGameResponse) createGameRequest.execute(context1);
        String gameId = createResponse.getGameId();
        
        JoinGameRequest joinGameRequest = new JoinGameRequest(gameId);
        joinGameRequest.execute(context2);
        
        // Step 2: Player 2 leaves the game
        LeaveGameRequest leaveGameRequest = new LeaveGameRequest(gameId);
        Response leaveResponse = leaveGameRequest.execute(context2);
        
        assertTrue(leaveResponse.isSuccess(), "Player should be able to leave game");
        
        // Step 3: Player 1 tries to start game (should fail due to insufficient players)
        SetPlayerReadyRequest setReady1 = new SetPlayerReadyRequest(true);
        setReady1.execute(context1);
        
        StartGameRequest startGameRequest = new StartGameRequest(gameId);
        Response startGameResponse = startGameRequest.execute(context1);
        
        assertFalse(startGameResponse.isSuccess(), "Game should not start after player leaves");
    }

    @Test
    @DisplayName("Should maintain player session registry correctly")
    void testPlayerSessionRegistryManagement() {
        // Verify initial state
        assertNull(playerRegistry.getPlayerIdForClient(CLIENT_ID_1), "Client should not be registered initially");
        assertNull(playerRegistry.getPlayerIdForClient(CLIENT_ID_2), "Client should not be registered initially");
        
        // Step 1: Player 1 logs in
        LoginRequest loginRequest1 = new LoginRequest(PLAYER_NICKNAME_1);
        LoginResponse loginResponse1 = (LoginResponse) loginRequest1.execute(context1);
        
        // Verify player 1 is registered
        String playerId1 = loginResponse1.getPlayerId();
        assertEquals(playerId1, playerRegistry.getPlayerIdForClient(CLIENT_ID_1), 
                "Client 1 should be mapped to player 1");
        assertNull(playerRegistry.getPlayerIdForClient(CLIENT_ID_2), 
                "Client 2 should not be registered yet");
        
        // Step 2: Player 2 logs in
        LoginRequest loginRequest2 = new LoginRequest(PLAYER_NICKNAME_2);
        LoginResponse loginResponse2 = (LoginResponse) loginRequest2.execute(context2);
        
        // Verify both players are registered
        String playerId2 = loginResponse2.getPlayerId();
        assertEquals(playerId1, playerRegistry.getPlayerIdForClient(CLIENT_ID_1), 
                "Client 1 should still be mapped to player 1");
        assertEquals(playerId2, playerRegistry.getPlayerIdForClient(CLIENT_ID_2), 
                "Client 2 should be mapped to player 2");
        
        // Verify bidirectional mapping
        assertEquals(CLIENT_ID_1, playerRegistry.getClientIdForPlayer(playerId1), 
                "Player 1 should be mapped back to client 1");
        assertEquals(CLIENT_ID_2, playerRegistry.getClientIdForPlayer(playerId2), 
                "Player 2 should be mapped back to client 2");
    }
}