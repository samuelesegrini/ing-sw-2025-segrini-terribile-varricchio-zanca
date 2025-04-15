package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.model.enums.resource.GoodType;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UseComponentVisitorTest {
    Ship ship1;
    Map<Direction, ConnectorType> connectors1;
    Battery battery1;
    Cabin cabin1;
    Cannon cannon1;
    Cannon cannon2;
    Engine engine1;
    Engine engine2;
    CargoHold cargoHoldNormal;
    CargoHold cargoHoldSpecial;
    Shield shield1;

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

        battery1 = new Battery(ComponentType.BATTERY, connectors1, 3);
        cabin1 = new Cabin(ComponentType.CABIN, connectors1);
        cannon1 = new Cannon(ComponentType.CANNON_SINGLE, connectors1);
        cannon2 = new Cannon(ComponentType.CANNON_DOUBLE, connectors1);
        engine1 = new Engine(ComponentType.ENGINE_SINGLE, connectors1);
        engine2 = new Engine(ComponentType.ENGINE_DOUBLE, connectors1);
        cargoHoldNormal = new CargoHold(ComponentType.CARGO_HOLD, connectors1, 3);
        cargoHoldSpecial = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors1, 2);
        shield1 = new Shield(ComponentType.SHIELD, connectors1);

        position1 = new Position(1, 1);
        position2 = new Position(2, 2);

        ship1 = new Ship(null, GameLevel.LEVEL_II);
    }

    @Test
    void testUseBattery() {
        ship1.addComponent(battery1, position1);
        UseComponentVisitor v = new UseComponentVisitor();
        v.setQuantity(2);
        battery1.use(v);
        assertEquals(1, battery1.getCurrentBatteries(), "Battery should now have 1 battery stored.");
        assertEquals(2, ship1.getChargingBatteries(), "Ship should now have 2 charging batteries.");
    }

    @Test
    void testUseCabin() {
        ship1.addComponent(cabin1, position1);
        ship1.updateStats();
        UseComponentVisitor v = new UseComponentVisitor();
        v.setQuantity(2);
        cabin1.use(v);
        assertEquals(0, cabin1.getCurrentCrew(), "Cabin should now have 0 crew.");
        assertEquals(0, ship1.getCrew(), "Ship should now have 0 crew.");
    }

    // TODO
    @Test
    void testUseCargoHold() {
        ship1.addComponent(cargoHoldNormal, position1);
        Map<GoodType, Integer> goods = new HashMap<>() {{
            put(GoodType.RED, 0);
            put(GoodType.BLUE, 1);
            put(GoodType.GREEN, 2);
            put(GoodType.YELLOW, 0);
        }};
        cargoHoldNormal.setStoredGoods(goods);
        ship1.updateStats();
        assertEquals(3, cargoHoldNormal.getOccupiedCapacity(), "Cargo should now have 3 goods stored.");

        UseComponentVisitor v = new UseComponentVisitor();
        goods = new HashMap<>() {{
            put(GoodType.RED, 0);
            put(GoodType.BLUE, -1);
            put(GoodType.GREEN, -1);
            put(GoodType.YELLOW, 0);
        }};
        v.setGoods(goods);
        cargoHoldNormal.use(v);
        //assertEquals(1, cargoHoldNormal.getOccupiedCapacity(), "Cargo should now have 1 goods stored.");
        //assertEquals(2, ship1.getNormalGoods(), "Ship should now have 2 normal goods stored.");
    }

    @Test
    void testUseCannon() {
        ship1.addComponent(cannon2, position1);
        ship1.addComponent(battery1, position2);
        ship1.updateStats();
        UseComponentVisitor v = new UseComponentVisitor();
        v.setQuantity(2);
        battery1.use(v);
        v = new UseComponentVisitor();
        cannon2.use(v);
        assertEquals(1, ship1.getChargingBatteries(), "chargingBatteries should now be 1.");
        assertTrue(cannon2.isCharged(), "Cannon should now be charged.");
    }

    @Test
    void testUseEngine() {
        ship1.addComponent(engine2, position1);
        ship1.addComponent(battery1, position2);
        ship1.updateStats();
        UseComponentVisitor v = new UseComponentVisitor();
        v.setQuantity(2);
        battery1.use(v);
        v = new UseComponentVisitor();
        engine2.use(v);
        assertEquals(1, ship1.getChargingBatteries(), "chargingBatteries should now be 1.");
        assertTrue(engine2.isCharged(), "Engine should now be charged.");
    }

    @Test
    void testUseShield() {
        ship1.addComponent(engine2, position1);
        ship1.addComponent(battery1, position2);
        ship1.updateStats();
        UseComponentVisitor v = new UseComponentVisitor();
        v.setQuantity(2);
        battery1.use(v);
        v = new UseComponentVisitor();
        engine2.use(v);
        assertEquals(1, ship1.getChargingBatteries(), "chargingBatteries should now be 1.");
        assertTrue(engine2.isCharged(), "Shield should now be charged.");
    }
}