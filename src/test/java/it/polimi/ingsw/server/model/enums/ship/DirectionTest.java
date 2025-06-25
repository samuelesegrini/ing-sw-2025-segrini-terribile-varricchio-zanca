package it.polimi.ingsw.server.model.enums.ship;

import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DirectionTest {

    @Test

    void test() {

        Direction dir;

        dir = Direction.UP;

        assertEquals(Direction.UP, dir);

        Direction dir2;

        dir2 = Direction.DOWN;

        assertEquals(Direction.DOWN, dir2);

        assertEquals(dir.getRotationSteps(dir, dir2), 2);

        dir2 = Direction.LEFT;
        assertEquals(dir.getRotationSteps(dir, dir2),1);

        dir2 = Direction.DOWN;
        assertEquals(dir.getOpposite(), Direction.DOWN);


        assertEquals(dir2.rotateClockwise(), Direction.LEFT);
        dir2 = dir2.rotateClockwise();
        assertEquals(Direction.UP, dir2.rotateClockwise());
        dir2 = dir2.rotateClockwise();
        assertEquals(dir2.rotateClockwise(), Direction.RIGHT);
        dir2 = dir2.rotateClockwise();
        assertEquals(dir2.rotateClockwise(), Direction.DOWN);
        dir2 = dir2.rotateClockwise();


        assertEquals(Direction.RIGHT, dir2.rotateCounterClockwise());
        dir2 = dir2.rotateCounterClockwise();
        assertEquals(Direction.UP, dir2.rotateCounterClockwise());
        dir2 = dir2.rotateCounterClockwise();
        assertEquals(dir2.rotateCounterClockwise(), Direction.LEFT);
        dir2 = dir2.rotateCounterClockwise();
        assertEquals(dir2.rotateCounterClockwise(), Direction.DOWN);


    }



}


