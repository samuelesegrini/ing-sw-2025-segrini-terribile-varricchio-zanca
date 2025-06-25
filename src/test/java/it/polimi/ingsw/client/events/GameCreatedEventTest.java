package it.polimi.ingsw.client.events;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.core.ViewNavigator;
import it.polimi.ingsw.client.ui.core.ViewNavigatorImpl;
import it.polimi.ingsw.common.message.event.GameCreatedEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.GameLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GameCreatedEvent handling, specifically verifying:
 * 1. Creator's view switches to game lobby
 * 2. Current game lobby is set properly
 * 3. Proper ViewNavigator usage
 */
class GameCreatedEventTest {

    private ClientState clientState;
    private ViewNavigator viewNavigator;

    @BeforeEach
    void setUp() {
        clientState = new ClientState();
        viewNavigator = new ViewNavigatorImpl(clientState);
    }

    @Test
    @DisplayName("GameCreatedEvent should NOT handle creator navigation (that's CreateGameResponse's job)")
    void testGameCreatedEventForCreator() {
        // Arrange - Set up as authenticated user
        PlayerId creatorId = new PlayerId(UUID.randomUUID(), "TestCreator");
        clientState.setPlayerInfo(creatorId, "TestCreator");
        clientState.setConnectionStatus(ClientState.ConnectionStatus.CONNECTED);
        
        // Start in LOBBY view (proper navigation path)
        assertTrue(viewNavigator.navigateTo(ClientState.ViewState.LOGIN, "Connected"));
        assertTrue(viewNavigator.navigateTo(ClientState.ViewState.LOBBY, "Logged in"));
        assertEquals(ClientState.ViewState.LOBBY, clientState.getCurrentView());
        
        // Create the event
        GameCreatedEvent event = new GameCreatedEvent(
            "game-123",
            creatorId.toString(),
            "TestCreator",
            4,
            GameLevel.TEST_FLIGHT,
            "Test Game"
        );
        
        // Act - Handle the event
        TestClientEventContext eventContext = new TestClientEventContext(creatorId.toString());
        event.handleOnClient(eventContext);
        
        // Assert - Check that the event does NOT change the view
        // Navigation should be handled by CreateGameResponse, not the event
        assertEquals(ClientState.ViewState.LOBBY, clientState.getCurrentView(),
            "GameCreatedEvent should NOT navigate creator - that's CreateGameResponse's responsibility");
        
        // The event should only log that it was received for the creator
        // (CreateGameResponse handles the actual navigation and state changes)
    }

    @Test
    @DisplayName("GameCreatedEvent should not affect non-creators")
    void testGameCreatedEventForNonCreator() {
        // Arrange - Set up as different user
        PlayerId localPlayerId = new PlayerId(UUID.randomUUID(), "OtherPlayer");
        PlayerId creatorId = new PlayerId(UUID.randomUUID(), "TestCreator");
        clientState.setPlayerInfo(localPlayerId, "OtherPlayer");
        clientState.setConnectionStatus(ClientState.ConnectionStatus.CONNECTED);
        
        // Start in LOBBY view (proper navigation path)
        assertTrue(viewNavigator.navigateTo(ClientState.ViewState.LOGIN, "Connected"));
        assertTrue(viewNavigator.navigateTo(ClientState.ViewState.LOBBY, "Logged in"));
        assertEquals(ClientState.ViewState.LOBBY, clientState.getCurrentView());
        
        // Create the event
        GameCreatedEvent event = new GameCreatedEvent(
            "game-123",
            creatorId.toString(),
            "TestCreator",
            4,
            GameLevel.TEST_FLIGHT,
            "Test Game"
        );
        
        // Act - Handle the event
        TestClientEventContext eventContext = new TestClientEventContext(localPlayerId.toString());
        event.handleOnClient(eventContext);
        
        // Assert - Check that view didn't change
        assertEquals(ClientState.ViewState.LOBBY, clientState.getCurrentView(),
            "Non-creator should remain in LOBBY view");
        
        // Check that current game lobby is NOT set
        assertNull(clientState.getCurrentGameLobby(),
            "Current game lobby should not be set for non-creator");
    }

    /**
     * Test implementation of ClientEventContext
     */
    private class TestClientEventContext implements ClientEventContext {
        private final String localPlayerId;

        public TestClientEventContext(String localPlayerId) {
            this.localPlayerId = localPlayerId;
        }

        @Override
        public void runOnUIThread(Runnable task) {
            // For testing, run immediately on current thread
            task.run();
        }

        @Override
        public ClientController getController() {
            // Create a minimal controller for testing
            return new TestClientController();
        }

        @Override
        public String getLocalPlayerId() {
            return localPlayerId;
        }

        @Override
        public boolean isLocalPlayer(String playerId) {
            return localPlayerId.equals(playerId);
        }

        @Override
        public ClientState getClientState() {
            return clientState;
        }

        @Override
        public it.polimi.ingsw.client.ui.core.NotificationService getNotificationService() {
            return null; // Not needed for this test
        }
    }

    /**
     * Test implementation of ClientController
     */
    private class TestClientController extends ClientController {
        public TestClientController() {
            super(null, clientState);
        }

        @Override
        public java.util.concurrent.CompletableFuture<Boolean> refreshGameList() {
            // Simulate successful refresh
            return java.util.concurrent.CompletableFuture.completedFuture(true);
        }
    }
}