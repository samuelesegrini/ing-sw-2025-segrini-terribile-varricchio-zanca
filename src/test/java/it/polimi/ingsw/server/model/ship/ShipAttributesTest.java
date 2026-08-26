package it.polimi.ingsw.server.model.ship;

import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.server.model.board.ShipBoardSpec;
import it.polimi.ingsw.server.model.component.CabinComponent;
import it.polimi.ingsw.server.model.component.Tiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a ship declares when a card asks: firepower, engine power and crew.
 *
 * <p>Firepower is where the arithmetic has to be exact. A single cannon not facing the
 * bow is worth one half, and manual p.11 says plainly that 5½ beats 5 and loses to 6, so
 * the model carries halves as whole numbers and never rounds. A combat zone decides who
 * gets shot on exactly this comparison.
 *
 * <p>The other rule with teeth is the alien bonus: two points, but only when the
 * attribute is already above zero. A purple alien will not fight bare-tentacled and a
 * brown one will not get out and push (manual p.18), so neither can rescue a ship with
 * no cannons or no engines at all.
 *
 * <p>Components involved: {@link Ship}, {@link ShipAttributes}, {@link BatteryPlan}.
 */
class ShipAttributesTest {

    private static final Position CABIN = new Position(2, 2);

    private Ship ship() {
        return Ships.openShip();
    }

    private static void put(Ship ship, Position cell, ComponentKind kind, Rotation rotation) {
        Ships.put(ship, cell, kind, rotation);
    }

    @Nested
    @DisplayName("the ship as built")
    class AsBuilt {

        @Test
        @DisplayName("a new ship is its starting cabin and nothing else")
        void newShip_isJustTheStartingCabin() {
            Ship ship = ship();

            assertEquals(1, ship.components().size());
            assertTrue(ship.componentAt(CABIN).isPresent());
            assertTrue(ship.isWhole());
            assertTrue(ship.validate().isLegal());
        }

        @Test
        @DisplayName("a component may only go on a free cell of the ship that touches what is already there")
        void placement_needsAFreeCellTouchingTheShip() {
            Ship ship = ship();

            assertTrue(ship.canPlaceAt(new Position(1, 2)));
            assertFalse(ship.canPlaceAt(CABIN), "the cell is taken");
            assertFalse(ship.canPlaceAt(new Position(0, 0)), "the cell touches nothing");
            assertFalse(ship.canPlaceAt(new Position(9, 9)), "the cell is off the board");

            assertThrows(IllegalArgumentException.class,
                    () -> put(ship, new Position(0, 0), ComponentKind.STRUCTURAL_MODULE, Rotation.NONE));
        }
    }

    @Nested
    @DisplayName("firepower")
    class Firepower {

        @Test
        @DisplayName("a bare ship has no firepower, and no alien can give it any")
        void shipWithoutCannons_hasNoFirepower() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN, Rotation.NONE);
            ((CabinComponent) ship.componentAt(new Position(1, 2)).orElseThrow())
                    .boardAlien(AlienColor.PURPLE);

            assertEquals(0, ship.attributes(BatteryPlan.none()).firepowerHalves());
        }

        @Test
        @DisplayName("a forward single cannon is worth a whole point and a side-facing one exactly half")
        void singleCannons_countFullForwardAndHalfOtherwise() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.SINGLE_CANNON, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.SINGLE_CANNON, Rotation.CLOCKWISE_90);

            assertEquals(3, ship.attributes(BatteryPlan.none()).firepowerHalves());
            assertEquals(1.5, ship.attributes(BatteryPlan.none()).firepower());
        }

        @Test
        @DisplayName("five and a half is carried exactly, so it can beat five and lose to six")
        void fiveAndAHalf_isCarriedExactly() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.SINGLE_CANNON, Rotation.NONE);
            put(ship, new Position(0, 2), ComponentKind.SINGLE_CANNON, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.SINGLE_CANNON, Rotation.NONE);
            put(ship, new Position(2, 0), ComponentKind.SINGLE_CANNON, Rotation.NONE);
            put(ship, new Position(2, 3), ComponentKind.SINGLE_CANNON, Rotation.NONE);
            put(ship, new Position(2, 4), ComponentKind.SINGLE_CANNON, Rotation.CLOCKWISE_90);

            ShipAttributes attributes = ship.attributes(BatteryPlan.none());

            assertEquals(11, attributes.firepowerHalves());
            assertEquals(5.5, attributes.firepower());
            assertTrue(attributes.firepowerHalves() > 10, "beats five");
            assertTrue(attributes.firepowerHalves() < 12, "loses to six");
        }

        @Test
        @DisplayName("a double cannon is worth nothing until a charge is spent on it")
        void doubleCannon_needsACharge() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.DOUBLE_CANNON, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.BATTERY, Rotation.NONE);
            ship.chargeBatteries();

            assertEquals(0, ship.attributes(BatteryPlan.none()).firepowerHalves());
            assertEquals(4, ship.attributes(BatteryPlan.powering(new Position(1, 2))).firepowerHalves());
        }

        @Test
        @DisplayName("a powered double cannon facing sideways is worth one point, half of its forward value")
        void sideFacingDoubleCannon_isHalved() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.DOUBLE_CANNON, Rotation.CLOCKWISE_90);
            put(ship, new Position(2, 1), ComponentKind.BATTERY, Rotation.NONE);
            ship.chargeBatteries();

            assertEquals(2, ship.attributes(BatteryPlan.powering(new Position(1, 2))).firepowerHalves());
        }

        @Test
        @DisplayName("a purple alien adds two points, but only once the cannons come to something")
        void purpleAlien_addsTwoOnlyAboveZero() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN, Rotation.NONE);
            CabinComponent cabin = (CabinComponent) ship.componentAt(new Position(1, 2)).orElseThrow();
            cabin.boardAlien(AlienColor.PURPLE);

            assertEquals(0, ship.attributes(BatteryPlan.none()).firepowerHalves());

            put(ship, new Position(2, 1), ComponentKind.SINGLE_CANNON, Rotation.CLOCKWISE_90);

            assertEquals(5, ship.attributes(BatteryPlan.none()).firepowerHalves(),
                    "half a point of cannon plus two points of alien");
        }

        @Test
        @DisplayName("a brown alien does nothing for firepower, whatever the cannons are worth")
        void brownAlien_doesNotArmTheShip() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.SINGLE_CANNON, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.CABIN, Rotation.NONE);
            ((CabinComponent) ship.componentAt(new Position(2, 1)).orElseThrow())
                    .boardAlien(AlienColor.BROWN);

            assertEquals(2, ship.attributes(BatteryPlan.none()).firepowerHalves());
        }
    }

    @Nested
    @DisplayName("engine power")
    class EnginePower {

        @Test
        @DisplayName("a single engine is worth one and a double is worth two once powered")
        void engines_countOneAndTwo() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.SINGLE_ENGINE, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.DOUBLE_ENGINE, Rotation.NONE);
            put(ship, new Position(2, 3), ComponentKind.BATTERY, Rotation.NONE);
            ship.chargeBatteries();

            assertEquals(1, ship.attributes(BatteryPlan.none()).enginePower());
            assertEquals(3, ship.attributes(BatteryPlan.powering(new Position(2, 1))).enginePower());
        }

        @Test
        @DisplayName("a brown alien adds two points, but only once the engines come to something")
        void brownAlien_addsTwoOnlyAboveZero() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN, Rotation.NONE);
            ((CabinComponent) ship.componentAt(new Position(1, 2)).orElseThrow())
                    .boardAlien(AlienColor.BROWN);

            assertEquals(0, ship.attributes(BatteryPlan.none()).enginePower());

            put(ship, new Position(2, 1), ComponentKind.SINGLE_ENGINE, Rotation.NONE);

            assertEquals(3, ship.attributes(BatteryPlan.none()).enginePower());
        }
    }

    @Nested
    @DisplayName("crew")
    class Crew {

        @Test
        @DisplayName("an alien counts as one crew member although it displaced two humans")
        void alien_countsAsOne() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN, Rotation.NONE);
            ((CabinComponent) ship.componentAt(CABIN).orElseThrow()).boardHumans();
            ((CabinComponent) ship.componentAt(new Position(1, 2)).orElseThrow())
                    .boardAlien(AlienColor.PURPLE);

            assertEquals(3, ship.crewCount());
            assertEquals(2, ship.humanCount());
            assertEquals(Set.of(AlienColor.PURPLE), ship.aliens());
        }

        @Test
        @DisplayName("humans are counted apart, because losing the last one ends a player's race")
        void humansAreCountedApart() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN, Rotation.NONE);
            ((CabinComponent) ship.componentAt(new Position(1, 2)).orElseThrow())
                    .boardAlien(AlienColor.BROWN);

            assertEquals(1, ship.crewCount());
            assertEquals(0, ship.humanCount());
        }
    }

    @Nested
    @DisplayName("battery plans")
    class BatteryPlans {

        @Test
        @DisplayName("charging fills every compartment to its printed capacity, once")
        void charging_fillsEveryCompartment() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.BATTERY, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.BATTERY, Rotation.NONE);

            assertEquals(0, ship.availableCharges());
            ship.chargeBatteries();
            assertEquals(4, ship.availableCharges());
        }

        @Test
        @DisplayName("spending takes the charges off the ship and they do not come back")
        void spending_consumesChargesForGood() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.DOUBLE_ENGINE, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.BATTERY, Rotation.NONE);
            ship.chargeBatteries();

            ship.spend(BatteryPlan.powering(new Position(1, 2)));

            assertEquals(1, ship.availableCharges());
        }

        @Test
        @DisplayName("a plan that would power a single cannon is refused, since singles run for free")
        void planPoweringASingle_isRefused() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.SINGLE_CANNON, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.BATTERY, Rotation.NONE);
            ship.chargeBatteries();

            assertThrows(IllegalArgumentException.class,
                    () -> ship.attributes(BatteryPlan.powering(new Position(1, 2))));
        }

        @Test
        @DisplayName("a plan costing more charges than the ship has is refused")
        void planBeyondTheChargesAvailable_isRefused() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.DOUBLE_CANNON, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.DOUBLE_CANNON, Rotation.NONE);
            put(ship, new Position(2, 3), ComponentKind.DOUBLE_CANNON, Rotation.NONE);

            assertThrows(IllegalArgumentException.class,
                    () -> ship.attributes(new BatteryPlan(Set.of(
                            new Position(1, 2), new Position(2, 1), new Position(2, 3)))));
        }

        @Test
        @DisplayName("a plan naming an empty cell is refused rather than quietly ignored")
        void planNamingAnEmptyCell_isRefused() {
            Ship ship = ship();

            assertThrows(IllegalArgumentException.class,
                    () -> ship.attributes(BatteryPlan.powering(new Position(0, 0))));
        }
    }
}
