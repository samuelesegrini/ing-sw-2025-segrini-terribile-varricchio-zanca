package it.polimi.ingsw.model.domain.ship;

import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.*;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShipTest {
    Ship ship1;
    Map<Direction, ConnectorType> connectors1;
    Battery battery1;
    Cannon cannon1;
    Engine engine1;
    CargoHold cargoHoldNormal;
    CargoHold cargoHoldSpecial;
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
        cargoHoldNormal = new CargoHold(ComponentType.CARGO_HOLD, connectors1, 2);
        cargoHoldSpecial = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors1, 2);

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
        Ship ship = new Ship(null, GameLevel.LEVEL_II);
        Map<Direction, ConnectorType> connectors = new HashMap<Direction, ConnectorType>() {{
            put(Direction.UP, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};

        CargoHold cargoHoldNormal = new CargoHold(ComponentType.CARGO_HOLD, connectors, 5);
        CargoHold cargoHoldSpecial = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 3);
        Battery battery = new Battery(ComponentType.BATTERY, connectors, 2);
        Cannon cannon = new Cannon(ComponentType.CANNON_SINGLE, connectors);
        Engine engine = new Engine(ComponentType.ENGINE_SINGLE, connectors);

        ship.addComponent(cargoHoldNormal, new Position(2, 3));
        ship.addComponent(cargoHoldSpecial, new Position(3, 3));
        ship.addComponent(battery, new Position(1, 1));
        ship.addComponent(cannon, new Position(1, 2)); // Posizione valida
        ship.addComponent(engine, new Position(4, 4));

        ship.updateStats();

        assertEquals(1.0, ship.getCannons(), "Il numero di cannoni dovrebbe essere 1.0");
        assertEquals(1.0, ship.getEngines(), "Il numero di motori dovrebbe essere 1.0");
        assertEquals(2, ship.getBatteries(), "Il numero di batterie dovrebbe essere 2");
        assertEquals(0, ship.getCrew(), "Il numero di membri dell'equipaggio dovrebbe essere 0");
        assertEquals(0, ship.getResources().get(GoodType.RED), "Il numero di merci rosse dovrebbe essere 0");
        assertEquals(0, ship.getResources().get(GoodType.BLUE), "Il numero di merci blu dovrebbe essere 0");
        assertEquals(0, ship.getResources().get(GoodType.GREEN), "Il numero di merci verdi dovrebbe essere 0");
        assertEquals(0, ship.getResources().get(GoodType.YELLOW), "Il numero di merci gialle dovrebbe essere 0");
    }

    @Test
    void testCalculateNormalGoodsCapacity() {
        Ship ship = new Ship(null, GameLevel.LEVEL_II);
        Map<Direction, ConnectorType> connectors = new HashMap<Direction, ConnectorType>() {{
            put(Direction.UP, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};
        CargoHold cargoHold1 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 5);
        CargoHold cargoHold2 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 3);
        ship.addComponent(cargoHold1, new Position(2, 3));
        ship.addComponent(cargoHold2, new Position(3, 3));

        int normalGoodsCapacity = ship.calculateNormalGoodsCapacity();

        assertEquals(8, normalGoodsCapacity, "La capacità delle merci normali dovrebbe essere 8");
    }

    @Test
    void testAddToSpecificCargoType_NormalGoods() {
        Ship ship = new Ship(null, GameLevel.LEVEL_II);
        Map<Direction, ConnectorType> connectors = new HashMap<Direction, ConnectorType>() {{
            put(Direction.UP, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};
        CargoHold cargoHold1 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 5);
        CargoHold cargoHold2 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 3);
        ship.addComponent(cargoHold1, new Position(2, 3));
        ship.addComponent(cargoHold2, new Position(3, 3));

        int remaining = ship.addToSpecificCargoType(GoodType.BLUE, 6, ComponentType.CARGO_HOLD);

        assertEquals(0, remaining, "Tutte le merci normali dovrebbero essere aggiunte con successo");
        assertEquals(5, cargoHold1.getStoredGoods().get(GoodType.BLUE), "Il primo cargo hold dovrebbe contenere 5 merci blu");
        assertEquals(1, cargoHold2.getStoredGoods().get(GoodType.BLUE), "Il secondo cargo hold dovrebbe contenere 1 merce blu");
    }

    @Test
    void testAddToSpecificCargoType_SpecialGoods() {
        Ship ship = new Ship(null, GameLevel.LEVEL_II);
        Map<Direction, ConnectorType> connectors = new HashMap<Direction, ConnectorType>() {{
            put(Direction.UP, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};
        CargoHold cargoHoldSpecial1 = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 4);
        CargoHold cargoHoldSpecial2 = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 6);
        ship.addComponent(cargoHoldSpecial1, new Position(2, 3));
        ship.addComponent(cargoHoldSpecial2, new Position(3, 3));

        int remaining = ship.addToSpecificCargoType(GoodType.RED, 8, ComponentType.CARGO_HOLD_SPECIAL);

        assertEquals(0, remaining, "Tutte le merci speciali dovrebbero essere aggiunte con successo");
        assertEquals(4, cargoHoldSpecial1.getStoredGoods().get(GoodType.RED), "Il primo cargo hold speciale dovrebbe contenere 4 merci rosse");
        assertEquals(4, cargoHoldSpecial2.getStoredGoods().get(GoodType.RED), "Il secondo cargo hold speciale dovrebbe contenere 4 merci rosse");
    }

    @Test
    void testAddToSpecificCargoType_NotEnoughSpace() {
        Ship ship = new Ship(null, GameLevel.LEVEL_II);
        Map<Direction, ConnectorType> connectors = new HashMap<Direction, ConnectorType>() {{
            put(Direction.UP, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};
        CargoHold cargoHold1 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 2);
        CargoHold cargoHold2 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 3);
        ship.addComponent(cargoHold1, new Position(2, 3));
        ship.addComponent(cargoHold2, new Position(3, 3));

        int remaining = ship.addToSpecificCargoType(GoodType.BLUE, 6, ComponentType.CARGO_HOLD);

        assertEquals(1, remaining, "Dovrebbe rimanere 1 merce blu non aggiunta per mancanza di spazio");
        assertEquals(2, cargoHold1.getStoredGoods().get(GoodType.BLUE), "Il primo cargo hold dovrebbe contenere 2 merci blu");
        assertEquals(3, cargoHold2.getStoredGoods().get(GoodType.BLUE), "Il secondo cargo hold dovrebbe contenere 3 merci blu");
    }

    @Test
    void testCalculateSpecialGoodsCapacity() {
        Ship ship = new Ship(null, GameLevel.LEVEL_II);
        Map<Direction, ConnectorType> connectors = new HashMap<Direction, ConnectorType>() {{
            put(Direction.UP, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};
        CargoHold cargoHoldSpecial1 = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 4);
        CargoHold cargoHoldSpecial2 = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 6);
        ship.addComponent(cargoHoldSpecial1, new Position(2, 3));
        ship.addComponent(cargoHoldSpecial2, new Position(3, 3));

        int specialGoodsCapacity = ship.calculateSpecialGoodsCapacity();

        assertEquals(10, specialGoodsCapacity, "La capacità delle merci speciali dovrebbe essere 10");
    }
    @Test
    void testAddResources_Success() {
        Ship ship = new Ship(null, GameLevel.LEVEL_II);
        Map<Direction, ConnectorType> connectors = new HashMap<Direction, ConnectorType>() {{
            put(Direction.UP, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};
        CargoHold cargoHoldNormal = new CargoHold(ComponentType.CARGO_HOLD, connectors, 5);
        CargoHold cargoHoldSpecial = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 3);
        ship.addComponent(cargoHoldNormal, new Position(2, 3));
        ship.addComponent(cargoHoldSpecial, new Position(3, 3));

        Map<GoodType, Integer> newResources = new HashMap<>();
        newResources.put(GoodType.RED, 2);
        newResources.put(GoodType.BLUE, 3);

        boolean result = ship.addResources(newResources);

        assertTrue(result, "Le risorse dovrebbero essere aggiunte con successo");
        assertEquals(2, ship.getResources().get(GoodType.RED), "Dovrebbero esserci 2 merci rosse");
        assertEquals(3, ship.getResources().get(GoodType.BLUE), "Dovrebbero esserci 3 merci blu");
    }

    @Test
    void testAddResources_Failure_NotEnoughSpace() {
        Ship ship = new Ship(null, GameLevel.LEVEL_II);
        Map<Direction, ConnectorType> connectors = new HashMap<Direction, ConnectorType>() {{
            put(Direction.UP, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};
        CargoHold cargoHoldNormal = new CargoHold(ComponentType.CARGO_HOLD, connectors, 2);
        CargoHold cargoHoldSpecial = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 1);
        ship.addComponent(cargoHoldNormal, new Position(2, 3));
        ship.addComponent(cargoHoldSpecial, new Position(3, 3));

        Map<GoodType, Integer> newResources = new HashMap<>();
        newResources.put(GoodType.RED, 2);
        newResources.put(GoodType.BLUE, 3);

        boolean result = ship.addResources(newResources);

        assertFalse(result, "Le risorse non dovrebbero essere aggiunte per mancanza di spazio");
        assertEquals(0, ship.getResources().get(GoodType.RED), "Non dovrebbero esserci merci rosse");
        assertEquals(0, ship.getResources().get(GoodType.BLUE), "Non dovrebbero esserci merci blu");
    }

    @Test
    void testAddResources_PartialSuccess() {
        Ship ship = new Ship(null, GameLevel.LEVEL_II);
        Map<Direction, ConnectorType> connectors = new HashMap<Direction, ConnectorType>() {{
            put(Direction.UP, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};
        CargoHold cargoHoldNormal = new CargoHold(ComponentType.CARGO_HOLD, connectors, 2);
        CargoHold cargoHoldSpecial = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 2);
        ship.addComponent(cargoHoldNormal, new Position(2, 3));
        ship.addComponent(cargoHoldSpecial, new Position(3, 3));

        Map<GoodType, Integer> newResources = new HashMap<>();
        newResources.put(GoodType.RED, 3);
        newResources.put(GoodType.BLUE, 2);

        boolean result = ship.addResources(newResources);

        assertFalse(result, "Le risorse non dovrebbero essere aggiunte per mancanza di spazio");
        assertEquals(0, ship.getResources().get(GoodType.RED), "Dovrebbero esserci 2 merci rosse");
        assertEquals(0, ship.getResources().get(GoodType.BLUE), "Dovrebbero esserci 2 merci blu");
    }

    @Test
    void testRemoveValuableResources_Success() {
        Ship ship = new Ship(null, GameLevel.LEVEL_II);
        Map<Direction, ConnectorType> connectors = new HashMap<Direction, ConnectorType>() {{
            put(Direction.UP, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};
        CargoHold cargoHoldNormal = new CargoHold(ComponentType.CARGO_HOLD, connectors, 5);
        CargoHold cargoHoldSpecial = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors, 3);
        ship.addComponent(cargoHoldNormal, new Position(2, 3));
        ship.addComponent(cargoHoldSpecial, new Position(3, 3));

        Map<GoodType, Integer> newResources = new HashMap<>();
        newResources.put(GoodType.RED, 2);
        newResources.put(GoodType.BLUE, 3);
        ship.addResources(newResources);

        boolean result = ship.removeValuableResources(3);

        assertTrue(result, "Le risorse dovrebbero essere rimosse con successo");
        assertEquals(0, ship.getResources().get(GoodType.RED), "Non dovrebbero esserci merci rosse");
        assertEquals(2, ship.getResources().get(GoodType.BLUE), "Dovrebbero esserci 2 merci blu");
    }

    @Test
    void testRemoveValuableResources_Failure() {
        Ship ship = new Ship(null, GameLevel.LEVEL_II);
        CargoHold cargoHoldNormal = new CargoHold(ComponentType.CARGO_HOLD, connectors1, 2);
        CargoHold cargoHoldSpecial = new CargoHold(ComponentType.CARGO_HOLD_SPECIAL, connectors1, 1);
        ship.addComponent(cargoHoldNormal, new Position(2, 3));
        ship.addComponent(cargoHoldSpecial, new Position(3, 3));

        Map<GoodType, Integer> newResources = new HashMap<>();
        newResources.put(GoodType.RED, 1);
        newResources.put(GoodType.BLUE, 1);
        ship.addResources(newResources);

        boolean result = ship.removeValuableResources(3);

        assertFalse(result, "Le risorse non dovrebbero essere rimosse completamente per mancanza di risorse");
        assertEquals(0, ship.getResources().get(GoodType.RED), "Non dovrebbero esserci merci rosse");
        assertEquals(0, ship.getResources().get(GoodType.BLUE), "Non dovrebbero esserci merci blu");
    }

    @Test
    void testFindFirstComponent_UpDirection() {
        Ship ship =new Ship(null, GameLevel.LEVEL_II);
        Component cabin = new Cabin(ComponentType.CABIN, connectors1);
        ship.addComponent(cabin, new Position(2, 3));

        Position result = ship.findFirstComponent(Direction.UP, 3);
        assertNotNull(result);
        assertEquals(2, result.getRow());
        assertEquals(3, result.getCol());
    }

    @Test
    void testFindFirstComponent_DownDirection() {
        Ship ship =new Ship(null, GameLevel.LEVEL_II);
        Component cabin = new Cabin(ComponentType.CABIN, connectors1);
        ship.addComponent(cabin, new Position(2, 3));

        Position result = ship.findFirstComponent(Direction.DOWN, 3);
        assertNotNull(result);
        assertEquals(2, result.getRow());
        assertEquals(3, result.getCol());
    }

    @Test
    void testFindFirstComponent_LeftDirection() {
        Ship ship =new Ship(null, GameLevel.LEVEL_II);
        Component cabin = new Cabin(ComponentType.CABIN, connectors1);
        ship.addComponent(cabin, new Position(2, 3));

        Position result = ship.findFirstComponent(Direction.LEFT, 2);
        assertNotNull(result);
        assertEquals(2, result.getRow());
        assertEquals(3, result.getCol());
    }

    @Test
    void testFindFirstComponent_RightDirection() {
        Ship ship =new Ship(null, GameLevel.LEVEL_II);
        Component cabin = new Cabin(ComponentType.CABIN, connectors1);
        ship.addComponent(cabin, new Position(2, 3));

        Position result = ship.findFirstComponent(Direction.RIGHT, 2);
        assertNotNull(result);
        assertEquals(2, result.getRow());
        assertEquals(3, result.getCol());
    }

    @Test
    void testFindFirstComponent_NoComponentFound() {
        Ship ship = new Ship(null, GameLevel.LEVEL_II);
        Position result = ship.findFirstComponent(Direction.UP, 3);
        assertNull(result, "Expected null when no component is found");
    }

    @Test
    void testProtectedByShield() {
        Ship ship =new Ship(null, GameLevel.LEVEL_II);
        Shield shield = new Shield(ComponentType.SHIELD, connectors1);
        shield.setProtectedDirections(Set.of(Direction.UP));
        ship.addComponent(shield, new Position(2, 3));

        assertTrue(ship.protectedByShield(Direction.UP), "La nave dovrebbe essere protetta dallo scudo nella direzione UP");
        assertFalse(ship.protectedByShield(Direction.DOWN), "La nave non dovrebbe essere protetta dallo scudo nella direzione DOWN");
    }

    @Test
    void testProtectedByCannon() {
        Ship ship =new Ship(null, GameLevel.LEVEL_II);
        Cannon singleCannon = new Cannon(ComponentType.CANNON_SINGLE, connectors1);
        ship.addComponent(singleCannon, new Position(2, 3));

        Cannon doubleCannon = new Cannon(ComponentType.CANNON_DOUBLE, connectors1);
        ship.addComponent(doubleCannon, new Position(3, 3));

        assertTrue(ship.protectedByCannon(Direction.UP, 3), "La nave dovrebbe essere protetta dal cannone singolo nella direzione UP");
        assertTrue(ship.protectedByCannon(Direction.UP, 3), "La nave dovrebbe essere protetta dal cannone doppio nella direzione UP");
        assertFalse(ship.protectedByCannon(Direction.DOWN, 3), "La nave non dovrebbe essere protetta dal cannone nella direzione DOWN");
    }

    @Test
    void testGetExposedConnectors() {
        Ship ship = new Ship(null, GameLevel.LEVEL_II);
        Map<Direction, ConnectorType> connectors = new HashMap<Direction, ConnectorType>() {{
            put(Direction.UP, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};
        CargoHold cargoHold1 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 5);
        CargoHold cargoHold2 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 5);
        ship.addComponent(cargoHold1, new Position(2, 3));
        ship.addComponent(cargoHold1, new Position(3, 3));

        int exposedConnectors = ship.getExposedConnectors();

        assertEquals(6, exposedConnectors, "Il numero di connettori esposti dovrebbe essere 6");
    }

    @Test
    void testGetExposedConnectors_Plain() {
        Ship ship = new Ship(null, GameLevel.LEVEL_II);
        Map<Direction, ConnectorType> connectors = new HashMap<Direction, ConnectorType>() {{
            put(Direction.UP, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.PLAIN);
        }};
        CargoHold cargoHold1 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 5);
        CargoHold cargoHold2 = new CargoHold(ComponentType.CARGO_HOLD, connectors, 5);
        ship.addComponent(cargoHold1, new Position(2, 3));
        ship.addComponent(cargoHold1, new Position(3, 3));

        int exposedConnectors = ship.getExposedConnectors();

        assertEquals(4, exposedConnectors, "Il numero di connettori esposti dovrebbe essere 6");
    }

    @Test
    void testCountAllAdjacentCabins() {
        Ship ship = new Ship(null, GameLevel.LEVEL_II);
        Map<Direction, ConnectorType> connectors = new HashMap<Direction, ConnectorType>() {{
            put(Direction.UP, ConnectorType.UNIVERSAL);
            put(Direction.RIGHT, ConnectorType.UNIVERSAL);
            put(Direction.DOWN, ConnectorType.UNIVERSAL);
            put(Direction.LEFT, ConnectorType.UNIVERSAL);
        }};
        Cabin cabin1 = new Cabin(ComponentType.CABIN, connectors);
        Cabin cabin2 = new Cabin(ComponentType.CABIN, connectors);
        Cabin cabin3 = new Cabin(ComponentType.CABIN, connectors);
        ship.addComponent(cabin1, new Position(2, 3));
        ship.addComponent(cabin2, new Position(2, 4));
        ship.addComponent(cabin3, new Position(3, 3));

        int adjacentCabins = ship.countAllAdjacentCabins();

        assertEquals(3, adjacentCabins, "Il numero di cabine adiacenti dovrebbe essere 3");
    }
}