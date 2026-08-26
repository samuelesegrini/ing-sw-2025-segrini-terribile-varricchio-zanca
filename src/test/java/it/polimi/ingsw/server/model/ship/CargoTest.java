package it.polimi.ingsw.server.model.ship;

import it.polimi.ingsw.server.model.component.ComponentKind;
import it.polimi.ingsw.server.model.goods.Forfeit;
import it.polimi.ingsw.server.model.goods.GoodColor;
import it.polimi.ingsw.server.model.goods.GoodsBank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks how a ship carries, loses and sells goods, against manual p.11 and p.19.
 *
 * <p>Cubes are conserved: every one a ship loads comes out of the bank, and every one it
 * sells, jettisons or has taken from it goes straight back. That matters because the
 * bank is finite, and a cube that quietly disappeared would make a shortage arrive
 * sooner than it should — or never.
 *
 * <p>The other rule with teeth is the forfeit cascade. A card that takes goods takes the
 * most valuable first, then battery charges once the holds are empty, and then nothing at
 * all: a ship with neither cannot be taken from.
 *
 * <p>Components involved: {@link Ship}, {@link GoodsBank}, {@link Forfeit}.
 */
class CargoTest {

    private static final Position STANDARD_HOLD = new Position(1, 2);
    private static final Position SPECIAL_HOLD = new Position(2, 1);
    private static final Position BATTERY = new Position(2, 3);

    /** An offer generous enough that a test only fails on the rule it is about. */
    private static final Map<GoodColor, Integer> EVERYTHING = Map.of(
            GoodColor.RED, 9, GoodColor.YELLOW, 9, GoodColor.GREEN, 9, GoodColor.BLUE, 9);

    /** A ship with one standard hold, one special hold and a battery, drawing on a deep bank. */
    private static Ship loadedShip() {
        return loadedShip(Ships.deepBank());
    }

    private static Ship loadedShip(GoodsBank bank) {
        Ship ship = Ships.openShip(bank);
        Ships.put(ship, STANDARD_HOLD, ComponentKind.CARGO_HOLD);
        Ships.put(ship, SPECIAL_HOLD, ComponentKind.SPECIAL_CARGO_HOLD);
        Ships.put(ship, BATTERY, ComponentKind.BATTERY);
        ship.chargeBatteries();
        return ship;
    }

    @Nested
    @DisplayName("loading")
    class Loading {

        @Test
        @DisplayName("cubes may only be moved while a card is letting the ship load")
        void holdsAreSealedOutsideALoadingWindow() {
            Ship ship = loadedShip();

            assertFalse(ship.cargoOperationsOpen());
            assertThrows(IllegalStateException.class, () -> ship.load(STANDARD_HOLD, GoodColor.BLUE));
            assertThrows(IllegalStateException.class, () -> ship.jettison(STANDARD_HOLD, GoodColor.BLUE));
            assertThrows(IllegalStateException.class,
                    () -> ship.moveCargo(STANDARD_HOLD, SPECIAL_HOLD, GoodColor.BLUE));
        }

        @Test
        @DisplayName("a loaded cube comes out of the bank, so cubes are never invented")
        void loadingTakesFromTheBank() {
            GoodsBank bank = Ships.bankOf(Map.of(GoodColor.BLUE, 3, GoodColor.RED, 1,
                    GoodColor.GREEN, 0, GoodColor.YELLOW, 0));
            Ship ship = loadedShip(bank);
            ship.beginCargoOperations(EVERYTHING);

            assertTrue(ship.load(STANDARD_HOLD, GoodColor.BLUE));

            assertEquals(2, bank.available(GoodColor.BLUE));
            assertEquals(1, ship.cargoCount());
        }

        @Test
        @DisplayName("a hold will not take a colour it is not rated for, and the bank keeps the cube")
        void aHoldRefusesAColourItCannotCarry() {
            GoodsBank bank = Ships.bankOf(Map.of(GoodColor.RED, 2, GoodColor.BLUE, 0,
                    GoodColor.GREEN, 0, GoodColor.YELLOW, 0));
            Ship ship = loadedShip(bank);
            ship.beginCargoOperations(EVERYTHING);

            assertThrows(IllegalArgumentException.class, () -> ship.load(STANDARD_HOLD, GoodColor.RED));
            assertEquals(2, bank.available(GoodColor.RED), "a refused load costs the bank nothing");

            assertEquals(Set.of(SPECIAL_HOLD), ship.holdsAccepting(GoodColor.RED));
            assertTrue(ship.load(SPECIAL_HOLD, GoodColor.RED));
        }

        @Test
        @DisplayName("an empty bank means no cube, which is a rule rather than an error")
        void anEmptyBankHandsOverNothing() {
            GoodsBank bank = Ships.bankOf(Map.of(GoodColor.BLUE, 0, GoodColor.RED, 0,
                    GoodColor.GREEN, 0, GoodColor.YELLOW, 0));
            Ship ship = loadedShip(bank);
            ship.beginCargoOperations(EVERYTHING);

            assertFalse(ship.load(STANDARD_HOLD, GoodColor.BLUE));
            assertEquals(0, ship.cargoCount());
        }

        @Test
        @DisplayName("a jettisoned cube goes back to the bank, where a ship behind can pick it up")
        void jettisoningReturnsTheCubeToTheBank() {
            GoodsBank bank = Ships.bankOf(Map.of(GoodColor.BLUE, 1, GoodColor.RED, 0,
                    GoodColor.GREEN, 0, GoodColor.YELLOW, 0));
            Ship ship = loadedShip(bank);
            ship.beginCargoOperations(EVERYTHING);
            ship.load(STANDARD_HOLD, GoodColor.BLUE);

            assertEquals(0, bank.available(GoodColor.BLUE));

            ship.jettison(STANDARD_HOLD, GoodColor.BLUE);

            assertEquals(1, bank.available(GoodColor.BLUE), "the cube is on offer again");
            assertEquals(0, ship.cargoCount());
        }

        @Test
        @DisplayName("cargo can be redistributed between holds while the window is open")
        void cargoCanBeRedistributed() {
            Ship ship = loadedShip();
            ship.beginCargoOperations(EVERYTHING);
            ship.load(STANDARD_HOLD, GoodColor.GREEN);

            ship.moveCargo(STANDARD_HOLD, SPECIAL_HOLD, GoodColor.GREEN);

            assertEquals(List.of(), ship.cargo().get(STANDARD_HOLD));
            assertEquals(List.of(GoodColor.GREEN), ship.cargo().get(SPECIAL_HOLD));
        }

        @Test
        @DisplayName("a red cube cannot be moved into a standard hold, however open the window is")
        void redCubesStayInSpecialHolds() {
            Ship ship = loadedShip();
            ship.beginCargoOperations(EVERYTHING);
            ship.load(SPECIAL_HOLD, GoodColor.RED);

            assertThrows(IllegalArgumentException.class,
                    () -> ship.moveCargo(SPECIAL_HOLD, STANDARD_HOLD, GoodColor.RED));
        }

        @Test
        @DisplayName("closing the window seals the holds again")
        void closingTheWindowSealsTheHolds() {
            Ship ship = loadedShip();
            ship.beginCargoOperations(EVERYTHING);
            ship.load(STANDARD_HOLD, GoodColor.BLUE);
            ship.endCargoOperations();

            assertThrows(IllegalStateException.class, () -> ship.jettison(STANDARD_HOLD, GoodColor.BLUE));
        }
    }

    @Nested
    @DisplayName("the forfeit cascade")
    class ForfeitCascade {

        private Ship shipCarrying(GoodColor... cubes) {
            Ship ship = loadedShip();
            ship.beginCargoOperations(EVERYTHING);
            for (GoodColor cube : cubes) {
                ship.load(cube.requiresSpecialHold() ? SPECIAL_HOLD : STANDARD_HOLD, cube);
            }
            ship.endCargoOperations();
            return ship;
        }

        @Test
        @DisplayName("the most valuable goods go first, whichever hold they are in")
        void theMostValuableGoFirst() {
            Ship ship = shipCarrying(GoodColor.BLUE, GoodColor.GREEN, GoodColor.RED);

            Forfeit forfeit = ship.surrender(2);

            assertEquals(List.of(GoodColor.RED, GoodColor.GREEN), forfeit.goods());
            assertEquals(List.of(GoodColor.BLUE), ship.manifest());
        }

        @Test
        @DisplayName("battery charges are given up once the holds are empty")
        void chargesGoOnceTheHoldsAreEmpty() {
            Ship ship = shipCarrying(GoodColor.BLUE);

            Forfeit forfeit = ship.surrender(3);

            assertEquals(List.of(GoodColor.BLUE), forfeit.goods());
            assertEquals(2, forfeit.batteries());
            assertEquals(0, ship.availableCharges());
            assertEquals(3, forfeit.total());
        }

        @Test
        @DisplayName("a ship with nothing left cannot be taken from any further")
        void aShipWithNothingCannotBeTakenFrom() {
            Ship ship = shipCarrying(GoodColor.BLUE);
            ship.surrender(3);

            Forfeit second = ship.surrender(2);

            assertEquals(List.of(), second.goods());
            assertEquals(0, second.batteries());
            assertEquals(2, second.unpaid());
            assertTrue(second.wasCappedByPoverty());
        }

        @Test
        @DisplayName("surrendered goods go back to the bank rather than out of the game")
        void surrenderedGoodsReturnToTheBank() {
            GoodsBank bank = Ships.bankOf(Map.of(GoodColor.BLUE, 2, GoodColor.RED, 0,
                    GoodColor.GREEN, 0, GoodColor.YELLOW, 0));
            Ship ship = loadedShip(bank);
            ship.beginCargoOperations(EVERYTHING);
            ship.load(STANDARD_HOLD, GoodColor.BLUE);
            ship.endCargoOperations();

            ship.surrender(1);

            assertEquals(2, bank.available(GoodColor.BLUE));
        }

        @Test
        @DisplayName("no cargo window is needed, because a forfeit is not a choice the player is making")
        void aForfeitNeedsNoWindow() {
            Ship ship = shipCarrying(GoodColor.GREEN);

            assertFalse(ship.cargoOperationsOpen());
            assertEquals(1, ship.surrender(1).goods().size());
        }

        @Test
        @DisplayName("demanding a negative amount is a programming error")
        void negativeDemandIsRefused() {
            assertThrows(IllegalArgumentException.class, () -> loadedShip().surrender(-1));
        }
    }

    @Nested
    @DisplayName("journey's end and losses")
    class EndOfJourney {

        @Test
        @DisplayName("selling hands the whole manifest back, most valuable first")
        void sellingHandsTheManifestBack() {
            GoodsBank bank = Ships.bankOf(Map.of(GoodColor.BLUE, 1, GoodColor.RED, 1,
                    GoodColor.GREEN, 1, GoodColor.YELLOW, 0));
            Ship ship = loadedShip(bank);
            ship.beginCargoOperations(EVERYTHING);
            ship.load(STANDARD_HOLD, GoodColor.BLUE);
            ship.load(STANDARD_HOLD, GoodColor.GREEN);
            ship.load(SPECIAL_HOLD, GoodColor.RED);
            ship.endCargoOperations();

            List<GoodColor> sold = ship.sellAllCargo();

            assertEquals(List.of(GoodColor.RED, GoodColor.GREEN, GoodColor.BLUE), sold);
            assertEquals(0, ship.cargoCount());
            assertEquals(1, bank.available(GoodColor.RED));
            assertEquals(1, bank.available(GoodColor.BLUE));
        }

        @Test
        @DisplayName("a destroyed hold sends its cubes back to the bank rather than out of the game")
        void aDestroyedHoldReturnsItsCubes() {
            GoodsBank bank = Ships.bankOf(Map.of(GoodColor.BLUE, 2, GoodColor.RED, 0,
                    GoodColor.GREEN, 0, GoodColor.YELLOW, 0));
            Ship ship = loadedShip(bank);
            ship.beginCargoOperations(EVERYTHING);
            ship.load(STANDARD_HOLD, GoodColor.BLUE);
            ship.endCargoOperations();

            assertEquals(1, bank.available(GoodColor.BLUE));

            // Printed column 6 is grid column 2, where the standard hold sits.
            ship.applyHit(new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 6), Defence.none());

            assertEquals(2, bank.available(GoodColor.BLUE), "the cube came back when the hold was lost");
        }
    }
}
