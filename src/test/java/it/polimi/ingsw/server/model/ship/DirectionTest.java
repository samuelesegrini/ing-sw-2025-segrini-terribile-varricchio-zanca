package it.polimi.ingsw.server.model.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks {@link Direction} and {@link Position}: the arithmetic every placement,
 * connection check and threat resolution goes through.
 */
class DirectionTest {

    @DisplayName("a direction is its own opposite twice over")
    @ParameterizedTest(name = "{0}")
    @EnumSource(Direction.class)
    void oppositeIsAnInvolution(Direction direction) {
        assertEquals(direction, direction.opposite().opposite());
    }

    @Test
    @DisplayName("north faces south, which is how two stacked tiles meet")
    void northFacesSouth() {
        assertEquals(Direction.SOUTH, Direction.NORTH.opposite());
        assertEquals(Direction.WEST, Direction.EAST.opposite());
    }

    @DisplayName("four quarter turns bring a direction back to itself")
    @ParameterizedTest(name = "{0}")
    @EnumSource(Direction.class)
    void fourClockwiseTurnsReturnToStart(Direction direction) {
        assertEquals(direction, direction.clockwise().clockwise().clockwise().clockwise());
    }

    @DisplayName("rotating and rotating back leaves a direction where it started")
    @ParameterizedTest(name = "{0}")
    @EnumSource(Rotation.class)
    void rotatingByAndBack_isIdentity(Rotation rotation) {
        for (Direction direction : Direction.values()) {
            assertEquals(direction, direction.rotatedBy(rotation).rotatedBy(rotation.inverse()));
        }
    }

    @Test
    @DisplayName("north and south address a column, east and west a row, which decides which dice roll applies")
    void verticalDirectionsAddressColumns() {
        assertTrue(Direction.NORTH.addressesColumn());
        assertTrue(Direction.SOUTH.addressesColumn());
        assertFalse(Direction.EAST.addressesColumn());
        assertFalse(Direction.WEST.addressesColumn());
    }

    @Test
    @DisplayName("stepping north moves up a row, because row zero is the top of the board")
    void steppingNorthDecreasesTheRow() {
        Position centre = new Position(2, 3);
        assertEquals(new Position(1, 3), centre.neighbour(Direction.NORTH));
        assertEquals(new Position(3, 3), centre.neighbour(Direction.SOUTH));
        assertEquals(new Position(2, 2), centre.neighbour(Direction.WEST));
        assertEquals(new Position(2, 4), centre.neighbour(Direction.EAST));
    }

    @DisplayName("a cell's neighbour has that cell back on the facing side")
    @ParameterizedTest(name = "{0}")
    @EnumSource(Direction.class)
    void neighbourhoodIsSymmetric(Direction direction) {
        Position cell = new Position(2, 3);
        assertEquals(cell, cell.neighbour(direction).neighbour(direction.opposite()));
    }
}
