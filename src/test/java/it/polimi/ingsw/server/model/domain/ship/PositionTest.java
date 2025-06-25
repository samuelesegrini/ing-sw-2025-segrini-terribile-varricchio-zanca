package it.polimi.ingsw.server.model.domain.ship;

import it.polimi.ingsw.server.model.enums.ship.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PositionTest {

    @Test

    void test(){

        Position pos;
        pos = new Position(5, 4);

        assertEquals(pos.getRow(), 5);
        assertEquals(pos.getCol(), 4);

        Direction dire = null;
        assertThrows(IllegalArgumentException.class, () -> {pos.offsetBy(dire);} );

        Direction direction = Direction.UP;
        Position position;
        position = new Position(4, 4);
        assertEquals(pos.offsetBy(Direction.UP), position);

        position = new Position(6, 4);
        assertEquals(pos.offsetBy(Direction.DOWN), position);

        position = new Position(5, 3);
        assertEquals(pos.offsetBy(Direction.LEFT), position);

        position = new Position(5, 5);
        assertEquals(pos.offsetBy(Direction.RIGHT), position);

        position = null;

        assertTrue(pos.equals(pos));
        assertTrue(pos.equals(new Position(5, 4)));

        assertFalse(pos.equals(position));
        assertFalse(pos.equals(direction));

        assertFalse(pos.equals(new Position(5, 5)));
        assertFalse(pos.equals(new Position(1, 4)));




    }

}