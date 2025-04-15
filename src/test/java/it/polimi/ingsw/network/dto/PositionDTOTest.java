package it.polimi.ingsw.network.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PositionDTOTest {

    @Test
    void testConstructorAndGetters() {
        PositionDTO position = new PositionDTO(3, 4);
        assertEquals(3, position.getX());
        assertEquals(4, position.getY());
    }

    @Test
    void testSetters() {
        PositionDTO position = new PositionDTO(0, 0);
        position.setX(5);
        position.setY(6);
        assertEquals(5, position.getX());
        assertEquals(6, position.getY());
    }

    @Test
    void testDefaultConstructor() {
        PositionDTO position = new PositionDTO();
        assertEquals(0, position.getX());
        assertEquals(0, position.getY());
    }

    @Test
    void testEquals() {
        PositionDTO pos1 = new PositionDTO(1, 2);
        PositionDTO pos2 = new PositionDTO(1, 2);
        PositionDTO pos3 = new PositionDTO(2, 1);

        // Test equality with same values
        assertTrue(pos1.equals(pos2));
        assertTrue(pos2.equals(pos1));

        // Test inequality with different values
        assertFalse(pos1.equals(pos3));
        assertFalse(pos3.equals(pos1));

        // Test equality with same object
        assertTrue(pos1.equals(pos1));

        // Test inequality with null
        assertFalse(pos1.equals(null));

        // Test inequality with different type
        assertFalse(pos1.equals(new Object()));
    }

    @Test
    void testHashCode() {
        PositionDTO pos1 = new PositionDTO(1, 2);
        PositionDTO pos2 = new PositionDTO(1, 2);
        PositionDTO pos3 = new PositionDTO(2, 1);

        // Test equal objects have equal hash codes
        assertEquals(pos1.hashCode(), pos2.hashCode());

        // Test different objects have different hash codes
        assertNotEquals(pos1.hashCode(), pos3.hashCode());
    }

    @Test
    void testToString() {
        PositionDTO position = new PositionDTO(7, 8);
        String expected = "PositionDTO{x=7, y=8}";
        assertEquals(expected, position.toString());
    }
} 