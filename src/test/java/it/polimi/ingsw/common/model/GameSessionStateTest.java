package it.polimi.ingsw.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionStateTest {

    @Test
    void testEnumValues() {
        GameSessionState[] states = GameSessionState.values();
        assertEquals(7, states.length, "GameSessionState should have 7 enum values");
        
        // Verify state order for game flow purposes (except ABORTED which can occur anytime)
        assertEquals(GameSessionState.LOBBY, states[0]);
        assertEquals(GameSessionState.SHIP_BUILDING, states[1]);
        assertEquals(GameSessionState.FLIGHT_PREPARATION, states[2]);
        assertEquals(GameSessionState.FLIGHT, states[3]);
        assertEquals(GameSessionState.SCORING, states[4]);
        assertEquals(GameSessionState.FINISHED, states[5]);
        assertEquals(GameSessionState.ABORTED, states[6]);
    }
    
    @Test
    void testEnumNames() {
        // Test valueOf conversion works correctly
        assertEquals(GameSessionState.LOBBY, GameSessionState.valueOf("LOBBY"));
        assertEquals(GameSessionState.SHIP_BUILDING, GameSessionState.valueOf("SHIP_BUILDING"));
        assertEquals(GameSessionState.FLIGHT_PREPARATION, GameSessionState.valueOf("FLIGHT_PREPARATION"));
        assertEquals(GameSessionState.FLIGHT, GameSessionState.valueOf("FLIGHT"));
        assertEquals(GameSessionState.SCORING, GameSessionState.valueOf("SCORING"));
        assertEquals(GameSessionState.FINISHED, GameSessionState.valueOf("FINISHED"));
        assertEquals(GameSessionState.ABORTED, GameSessionState.valueOf("ABORTED"));
    }

    @Test
    void testInvalidEnumName() {
        // Ensure valueOf throws IllegalArgumentException for invalid names
        assertThrows(IllegalArgumentException.class, () -> GameSessionState.valueOf("INVALID_STATE"));
    }
    
    @Test
    void testToString() {
        // Test that toString returns the name
        assertEquals("LOBBY", GameSessionState.LOBBY.toString());
        assertEquals("SHIP_BUILDING", GameSessionState.SHIP_BUILDING.toString());
        assertEquals("FLIGHT_PREPARATION", GameSessionState.FLIGHT_PREPARATION.toString());
        assertEquals("FLIGHT", GameSessionState.FLIGHT.toString());
        assertEquals("SCORING", GameSessionState.SCORING.toString());
        assertEquals("FINISHED", GameSessionState.FINISHED.toString());
        assertEquals("ABORTED", GameSessionState.ABORTED.toString());
    }
} 