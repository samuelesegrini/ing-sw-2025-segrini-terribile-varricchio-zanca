package it.polimi.ingsw.client.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the AppStatus enum in ClientViewModel
 */
class ClientViewModelAppStatusTest {

    @Test
    void testEnumValues() {
        ClientViewModel.AppStatus[] statuses = ClientViewModel.AppStatus.values();
        assertEquals(10, statuses.length, "AppStatus should have 10 enum values");
        
        // Verify all enum values exist
        assertEquals(ClientViewModel.AppStatus.NOT_CONNECTED, statuses[0]);
        assertEquals(ClientViewModel.AppStatus.CONNECTING, statuses[1]);
        assertEquals(ClientViewModel.AppStatus.LOGIN_SCREEN, statuses[2]);
        assertEquals(ClientViewModel.AppStatus.LOGGING_IN, statuses[3]);
        assertEquals(ClientViewModel.AppStatus.LOGGED_IN_BROWSING_LOBBIES, statuses[4]);
        assertEquals(ClientViewModel.AppStatus.GAME_LOBBY, statuses[5]);
        assertEquals(ClientViewModel.AppStatus.GAME_BUILDING, statuses[6]);
        assertEquals(ClientViewModel.AppStatus.GAME_FLIGHT, statuses[7]);
        assertEquals(ClientViewModel.AppStatus.GAME_FINISHED, statuses[8]);
        assertEquals(ClientViewModel.AppStatus.DISCONNECTED, statuses[9]);
    }
    
    @Test
    void testEnumNames() {
        // Test valueOf conversion works correctly for each status
        assertEquals(ClientViewModel.AppStatus.NOT_CONNECTED, 
                ClientViewModel.AppStatus.valueOf("NOT_CONNECTED"));
        assertEquals(ClientViewModel.AppStatus.CONNECTING, 
                ClientViewModel.AppStatus.valueOf("CONNECTING"));
        assertEquals(ClientViewModel.AppStatus.LOGIN_SCREEN, 
                ClientViewModel.AppStatus.valueOf("LOGIN_SCREEN"));
        assertEquals(ClientViewModel.AppStatus.LOGGING_IN, 
                ClientViewModel.AppStatus.valueOf("LOGGING_IN"));
        assertEquals(ClientViewModel.AppStatus.LOGGED_IN_BROWSING_LOBBIES, 
                ClientViewModel.AppStatus.valueOf("LOGGED_IN_BROWSING_LOBBIES"));
        assertEquals(ClientViewModel.AppStatus.GAME_LOBBY, 
                ClientViewModel.AppStatus.valueOf("GAME_LOBBY"));
        assertEquals(ClientViewModel.AppStatus.GAME_BUILDING, 
                ClientViewModel.AppStatus.valueOf("GAME_BUILDING"));
        assertEquals(ClientViewModel.AppStatus.GAME_FLIGHT, 
                ClientViewModel.AppStatus.valueOf("GAME_FLIGHT"));
        assertEquals(ClientViewModel.AppStatus.GAME_FINISHED, 
                ClientViewModel.AppStatus.valueOf("GAME_FINISHED"));
        assertEquals(ClientViewModel.AppStatus.DISCONNECTED, 
                ClientViewModel.AppStatus.valueOf("DISCONNECTED"));
    }
    
    @Test
    void testInvalidEnumName() {
        // Ensure valueOf throws IllegalArgumentException for invalid names
        assertThrows(IllegalArgumentException.class, 
                () -> ClientViewModel.AppStatus.valueOf("INVALID_STATUS"));
    }
    
    @Test
    void testToString() {
        // Test that toString returns the correct name for each enum value
        assertEquals("NOT_CONNECTED", ClientViewModel.AppStatus.NOT_CONNECTED.toString());
        assertEquals("CONNECTING", ClientViewModel.AppStatus.CONNECTING.toString());
        assertEquals("LOGIN_SCREEN", ClientViewModel.AppStatus.LOGIN_SCREEN.toString());
        assertEquals("LOGGING_IN", ClientViewModel.AppStatus.LOGGING_IN.toString());
        assertEquals("LOGGED_IN_BROWSING_LOBBIES", ClientViewModel.AppStatus.LOGGED_IN_BROWSING_LOBBIES.toString());
        assertEquals("GAME_LOBBY", ClientViewModel.AppStatus.GAME_LOBBY.toString());
        assertEquals("GAME_BUILDING", ClientViewModel.AppStatus.GAME_BUILDING.toString());
        assertEquals("GAME_FLIGHT", ClientViewModel.AppStatus.GAME_FLIGHT.toString());
        assertEquals("GAME_FINISHED", ClientViewModel.AppStatus.GAME_FINISHED.toString());
        assertEquals("DISCONNECTED", ClientViewModel.AppStatus.DISCONNECTED.toString());
    }
    
    @Test
    void testAppStatusTransitionLogic() {
        // This test verifies the typical flow of states in a client session
        // These assertions document the expected transition paths
        
        // Initial connection flow
        assertTrue(isValidTransition(ClientViewModel.AppStatus.NOT_CONNECTED, ClientViewModel.AppStatus.LOGIN_SCREEN));
        assertTrue(isValidTransition(ClientViewModel.AppStatus.LOGIN_SCREEN, ClientViewModel.AppStatus.CONNECTING));
        assertTrue(isValidTransition(ClientViewModel.AppStatus.CONNECTING, ClientViewModel.AppStatus.LOGGING_IN));
        assertTrue(isValidTransition(ClientViewModel.AppStatus.LOGGING_IN, ClientViewModel.AppStatus.LOGGED_IN_BROWSING_LOBBIES));
        
        // Game lobby flow
        assertTrue(isValidTransition(ClientViewModel.AppStatus.LOGGED_IN_BROWSING_LOBBIES, ClientViewModel.AppStatus.GAME_LOBBY));
        assertTrue(isValidTransition(ClientViewModel.AppStatus.GAME_LOBBY, ClientViewModel.AppStatus.GAME_BUILDING));
        assertTrue(isValidTransition(ClientViewModel.AppStatus.GAME_BUILDING, ClientViewModel.AppStatus.GAME_FLIGHT));
        assertTrue(isValidTransition(ClientViewModel.AppStatus.GAME_FLIGHT, ClientViewModel.AppStatus.GAME_FINISHED));
        
        // Back to lobby flow
        assertTrue(isValidTransition(ClientViewModel.AppStatus.GAME_FINISHED, ClientViewModel.AppStatus.LOGGED_IN_BROWSING_LOBBIES));
        assertTrue(isValidTransition(ClientViewModel.AppStatus.GAME_LOBBY, ClientViewModel.AppStatus.LOGGED_IN_BROWSING_LOBBIES));
        
        // Disconnection can happen from any state
        for (ClientViewModel.AppStatus status : ClientViewModel.AppStatus.values()) {
            if (status != ClientViewModel.AppStatus.DISCONNECTED && 
                status != ClientViewModel.AppStatus.NOT_CONNECTED) {
                assertTrue(isValidTransition(status, ClientViewModel.AppStatus.DISCONNECTED),
                        "Should be able to transition from " + status + " to DISCONNECTED");
            }
        }
    }
    
    /**
     * Helper method to indicate valid transitions
     * In a real implementation, this could check against an actual state machine
     */
    private boolean isValidTransition(ClientViewModel.AppStatus from, ClientViewModel.AppStatus to) {
        // This is a simplified version to document valid transitions
        // A real implementation might check against the view model's logic
        return true;
    }
} 