package it.polimi.ingsw.server.model.util;

import it.polimi.ingsw.server.model.enums.GameLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PileIdentifierTest {

    @Test
    void testEnumValues() {
        PileIdentifier[] piles = PileIdentifier.values();
        assertEquals(4, piles.length, "PileIdentifier should have 4 enum values");
        
        // Verify the enum values
        assertEquals(PileIdentifier.BOTTOM_LEFT, piles[0]);
        assertEquals(PileIdentifier.BOTTOM_CENTER, piles[1]);
        assertEquals(PileIdentifier.BOTTOM_RIGHT, piles[2]);
        assertEquals(PileIdentifier.UNKNOWN, piles[3]);
    }
    
    @Test
    void testEnumProperties() {
        // Test predictable piles
        assertTrue(PileIdentifier.BOTTOM_LEFT.isPredictable(), "BOTTOM_LEFT should be predictable");
        assertTrue(PileIdentifier.BOTTOM_CENTER.isPredictable(), "BOTTOM_CENTER should be predictable");
        assertTrue(PileIdentifier.BOTTOM_RIGHT.isPredictable(), "BOTTOM_RIGHT should be predictable");
        
        // Test unpredictable piles
        assertFalse(PileIdentifier.UNKNOWN.isPredictable(), "UNKNOWN should not be predictable");
        
        // Test pile indices
        assertEquals(0, PileIdentifier.BOTTOM_LEFT.getIndex());
        assertEquals(1, PileIdentifier.BOTTOM_CENTER.getIndex());
        assertEquals(2, PileIdentifier.BOTTOM_RIGHT.getIndex());
        assertEquals(-1, PileIdentifier.UNKNOWN.getIndex());
    }
    
    @Test
    void testFromIndex() {
        // Test finding piles by index
        assertEquals(PileIdentifier.BOTTOM_LEFT, PileIdentifier.fromIndex(0));
        assertEquals(PileIdentifier.BOTTOM_CENTER, PileIdentifier.fromIndex(1));
        assertEquals(PileIdentifier.BOTTOM_RIGHT, PileIdentifier.fromIndex(2));
        assertEquals(PileIdentifier.UNKNOWN, PileIdentifier.fromIndex(-1));
        
        // Test unknown index
        assertEquals(PileIdentifier.UNKNOWN, PileIdentifier.fromIndex(999));
    }
    
    @Test
    void testGetPredictablePiles() {
        // This test verifies that the correct predictable piles are returned for different game levels
        PileIdentifier[] testFlightPiles = PileIdentifier.getPredictablePiles(GameLevel.TEST_FLIGHT);
        PileIdentifier[] levelIIPiles = PileIdentifier.getPredictablePiles(GameLevel.LEVEL_II);
        
        // Both levels should return the same predictable piles
        assertEquals(3, testFlightPiles.length, "TEST_FLIGHT should have 3 predictable piles");
        assertEquals(3, levelIIPiles.length, "LEVEL_II should have 3 predictable piles");
        
        // Verify the piles are the correct ones
        assertArrayEquals(
            new PileIdentifier[] {
                PileIdentifier.BOTTOM_LEFT,
                PileIdentifier.BOTTOM_CENTER,
                PileIdentifier.BOTTOM_RIGHT
            },
            testFlightPiles,
            "TEST_FLIGHT predictable piles should be BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT"
        );
        
        assertArrayEquals(
            new PileIdentifier[] {
                PileIdentifier.BOTTOM_LEFT,
                PileIdentifier.BOTTOM_CENTER,
                PileIdentifier.BOTTOM_RIGHT
            },
            levelIIPiles,
            "LEVEL_II predictable piles should be BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT"
        );
    }
    
    @Test
    void testPredictablePilesDoNotIncludeUnknown() {
        // This test verifies that UNKNOWN is never included in predictable piles
        PileIdentifier[] predictablePiles = PileIdentifier.getPredictablePiles(GameLevel.TEST_FLIGHT);
        
        for (PileIdentifier pile : predictablePiles) {
            assertNotEquals(PileIdentifier.UNKNOWN, pile, 
                    "UNKNOWN should not be included in predictable piles");
        }
    }
} 