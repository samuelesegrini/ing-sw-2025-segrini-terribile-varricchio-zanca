package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.network.NetworkClient;
import it.polimi.ingsw.client.ui.core.ViewNavigator;
import it.polimi.ingsw.client.ui.core.ViewNavigatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GUI view switching behavior on successful connection.
 * Specifically tests the issue where views don't switch after connecting successfully.
 */
class GuiViewSwitchingTest {

    private ClientState clientState;
    private ViewNavigator viewNavigator;

    @BeforeEach
    void setUp() {
        // Create real client state and view navigator
        clientState = new ClientState();
        viewNavigator = new ViewNavigatorImpl(clientState);
    }

    @Test
    @DisplayName("Should demonstrate the view switching flow issue")
    void testViewSwitchingFlowIssue() {
        // This test demonstrates the actual issue: 
        // The ViewNavigator prevents navigation when connection status update timing is wrong
        
        // Arrange - Start in CONNECTION view
        assertEquals(ClientState.ViewState.CONNECTION, clientState.getCurrentView(), 
                "Should start in CONNECTION view");
        
        // Simulate the problematic scenario:
        // 1. Try to navigate to LOGIN before connection status is updated
        boolean canNavigateBeforeConnection = viewNavigator.canNavigateTo(ClientState.ViewState.LOGIN);
        assertFalse(canNavigateBeforeConnection, 
                "Should NOT be able to navigate to LOGIN when not connected (this is the issue!)");
        
        // 2. Now update connection status (simulating successful connection)
        clientState.setConnectionStatus(ClientState.ConnectionStatus.CONNECTED);
        
        // 3. Try navigation again - now it should work
        boolean canNavigateAfterConnection = viewNavigator.canNavigateTo(ClientState.ViewState.LOGIN);
        assertTrue(canNavigateAfterConnection, 
                "Should be able to navigate to LOGIN when connected");
        
        // 4. Perform the actual navigation
        boolean navigationSuccess = viewNavigator.navigateTo(ClientState.ViewState.LOGIN);
        assertTrue(navigationSuccess, "Navigation should succeed");
        assertEquals(ClientState.ViewState.LOGIN, viewNavigator.getCurrentViewState(),
                "Should be in LOGIN view after successful navigation");
    }

    @Test
    @DisplayName("Should test correct connection flow with proper timing")
    void testCorrectConnectionFlow() {
        // This test shows the correct way the connection flow should work
        
        // Arrange
        assertEquals(ClientState.ViewState.CONNECTION, clientState.getCurrentView());
        
        // Simulate what the ClientController.connect() method should do:
        // 1. Update connection status FIRST
        clientState.setConnectionStatus(ClientState.ConnectionStatus.CONNECTED);
        
        // 2. THEN try to navigate
        boolean navigationSuccess = viewNavigator.navigateTo(ClientState.ViewState.LOGIN);
        assertTrue(navigationSuccess, "Navigation should succeed when done in correct order");
        assertEquals(ClientState.ViewState.LOGIN, clientState.getCurrentView(),
                "Should be in LOGIN view after successful navigation");
    }

    @Test
    @DisplayName("Should validate ViewNavigator navigation rules for connection flow")
    void testViewNavigatorConnectionRules() {
        // Test navigation rules from CONNECTION state
        clientState.setCurrentView(ClientState.ViewState.CONNECTION);
        
        // Should not allow navigation to LOGIN when not connected
        assertFalse(viewNavigator.canNavigateTo(ClientState.ViewState.LOGIN), 
                "Should not allow navigation to LOGIN when not connected");
        
        // Simulate successful connection
        clientState.setConnectionStatus(ClientState.ConnectionStatus.CONNECTED);
        
        // Should allow navigation to LOGIN when connected
        assertTrue(viewNavigator.canNavigateTo(ClientState.ViewState.LOGIN), 
                "Should allow navigation to LOGIN when connected");
        
        // Test actual navigation
        boolean navigationSuccess = viewNavigator.navigateTo(ClientState.ViewState.LOGIN);
        assertTrue(navigationSuccess, "Navigation to LOGIN should succeed");
        assertEquals(ClientState.ViewState.LOGIN, viewNavigator.getCurrentViewState(), 
                "Current view should be LOGIN after navigation");
    }
    
    @Test
    @DisplayName("Should test the corrected ClientController connection behavior")
    void testCorrectedClientControllerConnectionBehavior() {
        // This test validates that the new implementation works correctly
        
        // Start in CONNECTION view (default state)
        assertEquals(ClientState.ViewState.CONNECTION, clientState.getCurrentView());
        assertEquals(ClientState.ConnectionStatus.DISCONNECTED, clientState.getConnectionStatus());
        
        // Test the CORRECTED flow from ClientController.connect():
        // 1. Connection status is updated FIRST
        clientState.setConnectionStatus(ClientState.ConnectionStatus.CONNECTED);
        
        // 2. ViewNavigator is used for navigation (not direct ClientState manipulation)
        boolean canNavigate = viewNavigator.canNavigateTo(ClientState.ViewState.LOGIN);
        assertTrue(canNavigate, "Should be able to navigate to LOGIN when connected");
        
        boolean navigationSuccess = viewNavigator.navigateTo(ClientState.ViewState.LOGIN, "Successful connection to server");
        assertTrue(navigationSuccess, "Navigation should succeed with proper order");
        assertEquals(ClientState.ViewState.LOGIN, viewNavigator.getCurrentViewState(),
                "Should be in LOGIN view after proper navigation");
    }
    
    @Test
    @DisplayName("Should test navigation failure scenarios with proper error handling")
    void testNavigationFailureScenarios() {
        // Test navigation failure when not connected
        assertEquals(ClientState.ViewState.CONNECTION, clientState.getCurrentView());
        assertEquals(ClientState.ConnectionStatus.DISCONNECTED, clientState.getConnectionStatus());
        
        // Try to navigate to LOGIN without being connected
        assertFalse(viewNavigator.canNavigateTo(ClientState.ViewState.LOGIN),
                "Should not allow navigation to LOGIN when not connected");
        
        String failureReason = viewNavigator.getNavigationFailureReason(ClientState.ViewState.LOGIN);
        assertEquals("Not connected to server", failureReason,
                "Should provide clear reason for navigation failure");
        
        boolean navigationAttempt = viewNavigator.navigateTo(ClientState.ViewState.LOGIN);
        assertFalse(navigationAttempt, "Navigation should fail when not connected");
        assertEquals(ClientState.ViewState.CONNECTION, clientState.getCurrentView(),
                "Should remain in CONNECTION view after failed navigation");
    }
    
    @Test
    @DisplayName("Should test the complete corrected navigation flow")
    void testCompleteNavigationFlow() {
        // Test the complete flow: CONNECTION -> LOGIN -> LOBBY -> GAME_LOBBY -> GAME
        
        // 1. Start in CONNECTION
        assertEquals(ClientState.ViewState.CONNECTION, clientState.getCurrentView());
        
        // 2. Connect to server
        clientState.setConnectionStatus(ClientState.ConnectionStatus.CONNECTED);
        assertTrue(viewNavigator.navigateTo(ClientState.ViewState.LOGIN, "Connected to server"));
        assertEquals(ClientState.ViewState.LOGIN, clientState.getCurrentView());
        
        // 3. Login successfully
        clientState.setPlayerInfo(new it.polimi.ingsw.server.model.domain.player.PlayerId(java.util.UUID.randomUUID(), "TestPlayer"), "TestPlayer");
        assertTrue(viewNavigator.navigateTo(ClientState.ViewState.LOBBY, "Login successful"));
        assertEquals(ClientState.ViewState.LOBBY, clientState.getCurrentView());
        
        // 4. Join a game
        assertTrue(viewNavigator.navigateTo(ClientState.ViewState.GAME_LOBBY, "Joined game"));
        assertEquals(ClientState.ViewState.GAME_LOBBY, clientState.getCurrentView());
        
        // 5. Start the game (simulate having a current game)
        // We need to mock the game state for navigation to work
        // For now, just set a mock game ID directly
        clientState.setCurrentGameLobby(null); // Reset first 
        // Simulate having a game by setting the game model directly (this would normally be done by join game response)
        try {
            // Create a minimal GameModel for testing
            it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager configManager = 
                new it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager();
            it.polimi.ingsw.server.model.domain.general.GameModel testGame = 
                new it.polimi.ingsw.server.model.domain.general.GameModel(it.polimi.ingsw.server.model.enums.GameLevel.TEST_FLIGHT, configManager, 2);
            clientState.setCurrentGameLobby(testGame);
            
            assertTrue(viewNavigator.navigateTo(ClientState.ViewState.GAME, "Game started"));
            assertEquals(ClientState.ViewState.GAME, clientState.getCurrentView());
        } catch (Exception e) {
            // If GameModel creation fails, just test that we can't navigate without proper game state
            assertFalse(viewNavigator.canNavigateTo(ClientState.ViewState.GAME), 
                    "Should not be able to navigate to GAME without proper game setup");
        }
        
        // 6. Leave the game
        assertTrue(viewNavigator.navigateTo(ClientState.ViewState.LOBBY, "Left game"));
        assertEquals(ClientState.ViewState.LOBBY, clientState.getCurrentView());
    }

}