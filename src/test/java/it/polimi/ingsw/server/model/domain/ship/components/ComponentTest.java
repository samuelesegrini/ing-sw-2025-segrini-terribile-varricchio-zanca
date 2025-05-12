package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Battery;
import it.polimi.ingsw.server.model.domain.ship.components.Cannon;
import it.polimi.ingsw.server.model.domain.ship.components.Engine;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentTest {
    Ship ship1;
    Map<Direction, ConnectorType> connectors1;
    Battery battery1;
    Cannon cannon1;
    Engine engine1;

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

        ship1 = new Ship(null, GameLevel.LEVEL_II);

        ship1.addComponent(battery1, new Position(2, 2));
        ship1.addComponent(cannon1, new Position(3, 3));
        ship1.addComponent(engine1, new Position(2, 3));
    }


    @Test
    void testRotate() {
        battery1.rotate();
        assertEquals(ConnectorType.SINGLE, battery1.getConnectorAt(Direction.UP));
        assertEquals(ConnectorType.PLAIN, battery1.getConnectorAt(Direction.RIGHT));
        assertEquals(ConnectorType.UNIVERSAL, battery1.getConnectorAt(Direction.DOWN));
        assertEquals(ConnectorType.DOUBLE, battery1.getConnectorAt(Direction.LEFT));
    }

    @Test
    void testCheck() {
        assertTrue(battery1.check(ship1));
        assertFalse(cannon1.check(ship1));
        assertFalse(engine1.check(ship1));
    }
}