package it.polimi.ingsw.integration;

import it.polimi.ingsw.common.ComponentData;
import it.polimi.ingsw.common.message.request.RequestContextImpl;
import it.polimi.ingsw.common.message.request.LoginRequest;
import it.polimi.ingsw.common.message.request.CreateGameRequest;
import it.polimi.ingsw.common.message.request.JoinGameRequest;
import it.polimi.ingsw.common.message.request.SetPlayerReadyRequest;
import it.polimi.ingsw.common.message.request.StartGameRequest;
import it.polimi.ingsw.common.message.request.TakeTileRequest;
import it.polimi.ingsw.common.message.request.PlaceTileRequest;
import it.polimi.ingsw.common.message.request.ReserveTileRequest;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.LoginResponse;
import it.polimi.ingsw.common.message.response.CreateGameResponse;
import it.polimi.ingsw.common.message.response.TakeTileResponse;
import it.polimi.ingsw.common.message.response.PlaceTileResponse;
import it.polimi.ingsw.common.message.response.ReserveTileResponse;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.EventPublisher;
import it.polimi.ingsw.common.message.event.Event;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the building phase flow.
 * Tests the complete building mechanics including component taking, placement, and ship validation.
 */
class BuildingPhaseFlowTest {

    private PlayerSessionRegistry playerRegistry;
    private GameSessionManager gameSessionManager;
    private RequestContextImpl context1;
    private RequestContextImpl context2;
    private List<Event> capturedEvents;
    
    private static final String CLIENT_ID_1 = "test-client-1";
    private static final String CLIENT_ID_2 = "test-client-2";
    private static final String PLAYER_NICKNAME_1 = "Player1";
    private static final String PLAYER_NICKNAME_2 = "Player2";

    @BeforeEach
    void setUp() {
        playerRegistry = new PlayerSessionRegistry();
        gameSessionManager = new GameSessionManager(null, playerRegistry);
        capturedEvents = new ArrayList<>();
        
        // Create mock event publisher that captures events
        EventPublisher mockEventPublisher = new EventPublisher() {
            @Override
            public void publishEvent(Event event) {
                capturedEvents.add(event);
            }
            
            @Override
            public void publishEventToGame(Event event, String gameId) {
                capturedEvents.add(event);
            }
            
            @Override
            public void publishEventToClient(Event event, String clientId) {
                capturedEvents.add(event);
            }
        };
        
        // Create request contexts for two test clients
        context1 = new RequestContextImpl(CLIENT_ID_1, gameSessionManager, playerRegistry, mockEventPublisher, null, new HashMap<>());
        context2 = new RequestContextImpl(CLIENT_ID_2, gameSessionManager, playerRegistry, mockEventPublisher, null, new HashMap<>());
    }

    /**
     * Helper method to set up a game in building phase with two players
     */
    private String setupGameInBuildingPhase() {
        // Players log in
        LoginRequest loginRequest1 = new LoginRequest(PLAYER_NICKNAME_1);
        loginRequest1.execute(context1);
        
        LoginRequest loginRequest2 = new LoginRequest(PLAYER_NICKNAME_2);
        loginRequest2.execute(context2);
        
        // Create and join game
        CreateGameRequest createGameRequest = new CreateGameRequest(2, GameLevel.TEST_FLIGHT, "Test Game");
        CreateGameResponse createResponse = (CreateGameResponse) createGameRequest.execute(context1);
        String gameId = createResponse.getGameId();
        
        JoinGameRequest joinGameRequest = new JoinGameRequest(gameId);
        joinGameRequest.execute(context2);
        
        // Set ready and start game
        SetPlayerReadyRequest setReady1 = new SetPlayerReadyRequest(true);
        setReady1.execute(context1);
        
        SetPlayerReadyRequest setReady2 = new SetPlayerReadyRequest(true);
        setReady2.execute(context2);
        
        StartGameRequest startGameRequest = new StartGameRequest(gameId);
        startGameRequest.execute(context1);
        
        return gameId;
    }

    @Test
    @DisplayName("Should successfully transition to building phase after game start")
    void testGameStartTransitionsToBuildingPhase() {
        String gameId = setupGameInBuildingPhase();
        
        GameSession session = gameSessionManager.getGameSession(gameId);
        assertNotNull(session, "Game session should exist");
        assertEquals(GamePhase.BUILDING, session.getCurrentPhase(), "Game should be in building phase");
    }

    @Test
    @DisplayName("Should successfully take a face-down component tile")
    void testTakeFaceDownTile() {
        String gameId = setupGameInBuildingPhase();
        
        // Player 1 takes a tile
        TakeTileRequest takeTileRequest = new TakeTileRequest();
        Response response = takeTileRequest.execute(context1);
        
        assertTrue(response.isSuccess(), "Taking tile should succeed");
        assertInstanceOf(TakeTileResponse.class, response);
        
        TakeTileResponse takeTileResponse = (TakeTileResponse) response;
        ComponentData componentData = takeTileResponse.getComponentData();
        assertNotNull(componentData, "Response should contain component data");
        assertNotNull(componentData.getId(), "Component should have an ID");
        assertNotNull(componentData.getType(), "Component should have a type");
        assertNotNull(componentData.getConnectors(), "Component should have connectors");
    }

    @Test
    @DisplayName("Should fail to take tile when not in building phase")
    void testCannotTakeTileOutsideBuildingPhase() {
        // Set up game but don't start it (should be in SETUP phase)
        LoginRequest loginRequest = new LoginRequest(PLAYER_NICKNAME_1);
        loginRequest.execute(context1);
        
        CreateGameRequest createGameRequest = new CreateGameRequest(2, GameLevel.TEST_FLIGHT, "Test Game");
        createGameRequest.execute(context1);
        
        // Try to take tile while in SETUP phase
        TakeTileRequest takeTileRequest = new TakeTileRequest();
        Response response = takeTileRequest.execute(context1);
        
        assertFalse(response.isSuccess(), "Taking tile should fail when not in building phase");
        assertInstanceOf(ErrorResponse.class, response);
        
        ErrorResponse errorResponse = (ErrorResponse) response;
        assertTrue(errorResponse.getErrorMessage().contains("Not in building phase"), 
                "Error should indicate wrong phase");
    }

    @Test
    @DisplayName("Should successfully place a component on ship grid")
    void testPlaceComponentOnShip() {
        String gameId = setupGameInBuildingPhase();
        
        // Player 1 takes a tile
        TakeTileRequest takeTileRequest = new TakeTileRequest();
        TakeTileResponse takeTileResponse = (TakeTileResponse) takeTileRequest.execute(context1);
        ComponentData componentData = takeTileResponse.getComponentData();
        
        // Place the component at position (2, 3) with no rotation
        PlaceTileRequest placeTileRequest = new PlaceTileRequest(
            componentData.getId(), 
            2, 3, 0  // row, col, rotation
        );
        Response response = placeTileRequest.execute(context1);
        
        assertTrue(response.isSuccess(), "Placing tile should succeed");
        assertInstanceOf(PlaceTileResponse.class, response);
        
        // Verify component is placed on the ship
        GameSession session = gameSessionManager.getGameSession(gameId);
        String playerId = playerRegistry.getPlayerIdForClient(CLIENT_ID_1);
        Player player = session.getPlayer(playerId);
        Ship ship = player.getShip();
        
        assertNotNull(ship.getBoard()[2][3], "Component should be placed at specified position");
    }

    @Test
    @DisplayName("Should fail to place component at invalid position")
    void testCannotPlaceComponentAtInvalidPosition() {
        String gameId = setupGameInBuildingPhase();
        
        // Player 1 takes a tile
        TakeTileRequest takeTileRequest = new TakeTileRequest();
        TakeTileResponse takeTileResponse = (TakeTileResponse) takeTileRequest.execute(context1);
        ComponentData componentData = takeTileResponse.getComponentData();
        
        // Try to place component outside grid bounds
        PlaceTileRequest placeTileRequest = new PlaceTileRequest(
            componentData.getId(), 
            5, 7, 0  // Invalid row and col (should be max 4, 6)
        );
        Response response = placeTileRequest.execute(context1);
        
        assertFalse(response.isSuccess(), "Placing tile at invalid position should fail");
        assertInstanceOf(ErrorResponse.class, response);
        
        ErrorResponse errorResponse = (ErrorResponse) response;
        assertTrue(errorResponse.getErrorMessage().contains("must be between"), 
                "Error should indicate position validation failure");
    }

    @Test
    @DisplayName("Should fail to place component at occupied position")
    void testCannotPlaceComponentAtOccupiedPosition() {
        String gameId = setupGameInBuildingPhase();
        
        // Player 1 takes first tile
        TakeTileRequest takeTileRequest1 = new TakeTileRequest();
        TakeTileResponse takeTileResponse1 = (TakeTileResponse) takeTileRequest1.execute(context1);
        ComponentData componentData1 = takeTileResponse1.getComponentData();
        
        // Place first component
        PlaceTileRequest placeTileRequest1 = new PlaceTileRequest(
            componentData1.getId(), 
            2, 3, 0
        );
        placeTileRequest1.execute(context1);
        
        // Player 1 takes second tile
        TakeTileRequest takeTileRequest2 = new TakeTileRequest();
        TakeTileResponse takeTileResponse2 = (TakeTileResponse) takeTileRequest2.execute(context1);
        ComponentData componentData2 = takeTileResponse2.getComponentData();
        
        // Try to place second component at same position
        PlaceTileRequest placeTileRequest2 = new PlaceTileRequest(
            componentData2.getId(), 
            2, 3, 0  // Same position as first component
        );
        Response response = placeTileRequest2.execute(context1);
        
        assertFalse(response.isSuccess(), "Placing tile at occupied position should fail");
        assertInstanceOf(ErrorResponse.class, response);
        
        ErrorResponse errorResponse = (ErrorResponse) response;
        assertTrue(errorResponse.getErrorMessage().contains("already occupied"), 
                "Error should indicate position is occupied");
    }

    @Test
    @DisplayName("Should successfully reserve a component")
    void testReserveComponent() {
        String gameId = setupGameInBuildingPhase();
        
        // Player 1 takes a tile
        TakeTileRequest takeTileRequest = new TakeTileRequest();
        TakeTileResponse takeTileResponse = (TakeTileResponse) takeTileRequest.execute(context1);
        ComponentData componentData = takeTileResponse.getComponentData();
        
        // Reserve the component
        ReserveTileRequest reserveTileRequest = new ReserveTileRequest(componentData.getId());
        Response response = reserveTileRequest.execute(context1);
        
        assertTrue(response.isSuccess(), "Reserving tile should succeed");
        assertInstanceOf(ReserveTileResponse.class, response);
    }

    @Test
    @DisplayName("Should fail to reserve non-existent component")
    void testCannotReserveNonExistentComponent() {
        setupGameInBuildingPhase();
        
        // Try to reserve a non-existent component
        ReserveTileRequest reserveTileRequest = new ReserveTileRequest("non-existent-id");
        Response response = reserveTileRequest.execute(context1);
        
        assertFalse(response.isSuccess(), "Reserving non-existent component should fail");
        assertInstanceOf(ErrorResponse.class, response);
        
        ErrorResponse errorResponse = (ErrorResponse) response;
        assertTrue(errorResponse.getErrorMessage().contains("not found"), 
                "Error should indicate component not found");
    }

    @Test
    @DisplayName("Should handle component rotation during placement")
    void testComponentRotationDuringPlacement() {
        String gameId = setupGameInBuildingPhase();
        
        // Player 1 takes a tile
        TakeTileRequest takeTileRequest = new TakeTileRequest();
        TakeTileResponse takeTileResponse = (TakeTileResponse) takeTileRequest.execute(context1);
        ComponentData componentData = takeTileResponse.getComponentData();
        
        // Place component with 90-degree rotation
        PlaceTileRequest placeTileRequest = new PlaceTileRequest(
            componentData.getId(), 
            2, 3, 1  // rotation = 1 means 90 degrees
        );
        Response response = placeTileRequest.execute(context1);
        
        assertTrue(response.isSuccess(), "Placing rotated tile should succeed");
        assertInstanceOf(PlaceTileResponse.class, response);
    }

    @Test
    @DisplayName("Should validate rotation parameter bounds")
    void testRotationParameterValidation() {
        String gameId = setupGameInBuildingPhase();
        
        // Player 1 takes a tile
        TakeTileRequest takeTileRequest = new TakeTileRequest();
        TakeTileResponse takeTileResponse = (TakeTileResponse) takeTileRequest.execute(context1);
        ComponentData componentData = takeTileResponse.getComponentData();
        
        // Try to place component with invalid rotation
        PlaceTileRequest placeTileRequest = new PlaceTileRequest(
            componentData.getId(), 
            2, 3, 4  // rotation = 4 is invalid (should be 0-3)
        );
        Response response = placeTileRequest.execute(context1);
        
        assertFalse(response.isSuccess(), "Placing tile with invalid rotation should fail");
        assertInstanceOf(ErrorResponse.class, response);
        
        ErrorResponse errorResponse = (ErrorResponse) response;
        assertTrue(errorResponse.getErrorMessage().contains("Rotation must be"), 
                "Error should indicate rotation validation failure");
    }

    @Test
    @DisplayName("Should handle multiple players building simultaneously")
    void testMultiplayerBuildingFlow() {
        String gameId = setupGameInBuildingPhase();
        
        // Player 1 takes and places a tile
        TakeTileRequest takeTileRequest1 = new TakeTileRequest();
        TakeTileResponse takeTileResponse1 = (TakeTileResponse) takeTileRequest1.execute(context1);
        ComponentData componentData1 = takeTileResponse1.getComponentData();
        
        PlaceTileRequest placeTileRequest1 = new PlaceTileRequest(
            componentData1.getId(), 2, 3, 0
        );
        Response placeResponse1 = placeTileRequest1.execute(context1);
        
        // Player 2 takes and places a tile
        TakeTileRequest takeTileRequest2 = new TakeTileRequest();
        TakeTileResponse takeTileResponse2 = (TakeTileResponse) takeTileRequest2.execute(context2);
        ComponentData componentData2 = takeTileResponse2.getComponentData();
        
        PlaceTileRequest placeTileRequest2 = new PlaceTileRequest(
            componentData2.getId(), 1, 2, 0  // Position (1,2) is valid, (1,1) is forbidden
        );
        Response placeResponse2 = placeTileRequest2.execute(context2);
        
        assertTrue(placeResponse1.isSuccess(), "Player 1 should successfully place tile");
        assertTrue(placeResponse2.isSuccess(), "Player 2 should successfully place tile");
        
        // Verify both players have components on their ships
        GameSession session = gameSessionManager.getGameSession(gameId);
        String playerId1 = playerRegistry.getPlayerIdForClient(CLIENT_ID_1);
        String playerId2 = playerRegistry.getPlayerIdForClient(CLIENT_ID_2);
        
        Player player1 = session.getPlayer(playerId1);
        Player player2 = session.getPlayer(playerId2);
        
        assertNotNull(player1.getShip().getBoard()[2][3], "Player 1 should have component placed");
        assertNotNull(player2.getShip().getBoard()[1][2], "Player 2 should have component placed");
    }

    @Test
    @DisplayName("Should broadcast events when components are taken and placed")
    void testEventBroadcasting() {
        String gameId = setupGameInBuildingPhase();
        capturedEvents.clear(); // Clear setup events
        
        // Player 1 takes a tile
        TakeTileRequest takeTileRequest = new TakeTileRequest();
        TakeTileResponse takeTileResponse = (TakeTileResponse) takeTileRequest.execute(context1);
        ComponentData componentData = takeTileResponse.getComponentData();
        
        // Player 1 places the tile
        PlaceTileRequest placeTileRequest = new PlaceTileRequest(
            componentData.getId(), 2, 3, 0
        );
        placeTileRequest.execute(context1);
        
        // Verify events were broadcast
        assertTrue(capturedEvents.size() >= 2, "Should have captured at least 2 events (take + place)");
        
        // Verify event types
        boolean foundTakeEvent = capturedEvents.stream()
            .anyMatch(event -> event.getClass().getSimpleName().contains("ComponentTaken"));
        boolean foundPlaceEvent = capturedEvents.stream()
            .anyMatch(event -> event.getClass().getSimpleName().contains("TilePlaced"));
        
        assertTrue(foundTakeEvent || foundPlaceEvent, "Should have captured building-related events");
    }

    @Test
    @DisplayName("Should handle empty component deck gracefully")
    void testEmptyComponentDeckHandling() {
        String gameId = setupGameInBuildingPhase();
        GameSession session = gameSessionManager.getGameSession(gameId);
        ComponentDeck deck = session.getGameModel().getComponentDeck();
        
        // Exhaust the component deck
        while (deck.draw().isPresent()) {
            // Keep drawing until deck is empty
        }
        
        // Try to take a tile from empty deck
        TakeTileRequest takeTileRequest = new TakeTileRequest();
        Response response = takeTileRequest.execute(context1);
        
        assertFalse(response.isSuccess(), "Taking tile from empty deck should fail");
        assertInstanceOf(ErrorResponse.class, response);
        
        ErrorResponse errorResponse = (ErrorResponse) response;
        assertTrue(errorResponse.getErrorMessage().contains("No tiles left"), 
                "Error should indicate deck is empty");
    }

    @Test
    @DisplayName("Should validate ship grid bounds for all operations")
    void testShipGridBoundsValidation() {
        String gameId = setupGameInBuildingPhase();
        
        // Player 1 takes a tile
        TakeTileRequest takeTileRequest = new TakeTileRequest();
        TakeTileResponse takeTileResponse = (TakeTileResponse) takeTileRequest.execute(context1);
        ComponentData componentData = takeTileResponse.getComponentData();
        
        // Test various out-of-bounds positions
        int[][] invalidPositions = {
            {-1, 3},   // Negative row
            {5, 3},    // Row too high (max is 4)
            {2, -1},   // Negative column
            {2, 7},    // Column too high (max is 6)
            {-1, -1},  // Both negative
            {10, 10}   // Both too high
        };
        
        for (int[] pos : invalidPositions) {
            PlaceTileRequest placeTileRequest = new PlaceTileRequest(
                componentData.getId(), pos[0], pos[1], 0
            );
            Response response = placeTileRequest.execute(context1);
            
            assertFalse(response.isSuccess(), 
                String.format("Placing tile at (%d, %d) should fail", pos[0], pos[1]));
            assertInstanceOf(ErrorResponse.class, response);
        }
    }
}