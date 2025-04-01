package it.polimi.ingsw.model.domain.ship;

import it.polimi.ingsw.model.domain.ship.components.Battery;
import it.polimi.ingsw.model.domain.ship.components.Cannon;
import it.polimi.ingsw.model.domain.ship.components.Engine;
import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ShipTest {
    Ship ship1;
    Map<Direction, ConnectorType> connectors1;
    Battery battery1;
    Cannon cannon1;
    Engine engine1;

    Position position1;
    Position position2;

    @BeforeEach
    void setUp() {
        connectors1 = new HashMap<Direction, ConnectorType>() {{
            put(Direction.UP, ConnectorType.PLAIN);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.DOWN, ConnectorType.DOUBLE);
            put(Direction.LEFT, ConnectorType.SINGLE);
        }};

        battery1 = new Battery(ComponentType.BATTERY, connectors1, 1);
        cannon1 = new Cannon(ComponentType.CANNON_SINGLE, connectors1);
        engine1 = new Engine(ComponentType.ENGINE_SINGLE, connectors1);

        position1 = new Position(1, 0);
        position2 = new Position(1, 1);

        ship1 = new Ship(null, GameLevel.LEVEL_II);
        //System.out.println(Arrays.toString(ship1.forbiddenPositions.toArray()));
    }

    @Test
    void testAddComponent() {
        assertTrue(ship1.forbiddenPositions.contains(position1));


        IllegalArgumentException exception1 = Assertions.assertThrows(IllegalArgumentException.class, () -> ship1.addComponent(battery1, position1));
        assertEquals("Forbidden position", exception1.getMessage());

        ship1.addComponent(cannon1, position2);
        assertNotNull(ship1.getBoard()[position2.getRow()][position2.getCol()]);

        //ship1.addComponent(cannon1, position2);
        //GIUSTO assertEquals(battery1, ship1.getBoard()[position2.getRow()][position2.getCol()]);
    }

    @Test
    void testRemoveComponent() {
        ship1.addComponent(cannon1, position2);
        ship1.removeComponent(position2);
        assertNull(ship1.getBoard()[position2.getRow()][position2.getCol()]);
    }

    @Test
    void testReserveComponent() {
    }

    @Test
    void testUpdateStats() {
    }

    @Test
    void testAddResources() {
    }

    @Test
    void testRemoveValuableResources() {
    }

    @Test
    void testFindFirstComponent() {
    }

    @Test
    void testProtectedByShield() {
    }

    @Test
    void testProtectedByCannon() {
    }

    @Test
    void testGetExposedConnectors() {
    }

    @Test
    void testCountAllConnectedCabins() {
    }
}