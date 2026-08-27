package it.polimi.ingsw.server.model.ship;

import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.DamageReport;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.Hit;
import it.polimi.ingsw.common.game.HitKind;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.server.model.component.ShipComponent;
import it.polimi.ingsw.server.model.component.StructuralComponent;
import it.polimi.ingsw.server.model.component.Tile;
import it.polimi.ingsw.server.model.goods.GoodsBank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a component lets go of when it is destroyed.
 *
 * <p>Cubes are conserved, and the bank is finite: a cube that quietly disappeared would make
 * a shortage arrive sooner than it should, or never, and nothing at the table would show why.
 * So a hold destroyed with cargo aboard has to hand back what it held.
 *
 * <p>This used to be a {@code switch} over {@link ShipComponent} inside {@link Ship} — the one
 * switch over that hierarchy in the whole project with a {@code default} arm in it, and so the
 * one place where adding a ninth kind of component would compile cleanly and lose its contents
 * for the rest of the flight. These tests pin the behaviour now that each component answers
 * for itself.
 *
 * <p>Components involved: {@link Ship}, {@link ShipComponent}, {@link GoodsBank}.
 */
class ScrappingTest {

    private static final Position HOLD = new Position(1, 2);
    private static final Position BATTERY = new Position(2, 3);
    private static final Position CABIN = new Position(3, 2);

    private static final Map<GoodColor, Integer> STOCK = Map.of(
            GoodColor.RED, 4, GoodColor.YELLOW, 4, GoodColor.GREEN, 4, GoodColor.BLUE, 4);

    @Nested
    @DisplayName("a destroyed component")
    class WhenDestroyed {

        @Test
        @DisplayName("hands its cubes back, so the bank ends where it started")
        void aHoldGivesItsCargoBack() {
            GoodsBank bank = Ships.bankOf(STOCK);
            Ship ship = Ships.openShip(bank);
            Ships.put(ship, HOLD, ComponentKind.CARGO_HOLD);
            ship.beginCargoOperations(Map.of(GoodColor.BLUE, 2));
            assertTrue(ship.load(HOLD, GoodColor.BLUE));
            assertTrue(ship.load(HOLD, GoodColor.BLUE));
            ship.endCargoOperations();
            assertEquals(2, bank.available(GoodColor.BLUE), "two cubes are aboard");

            ship.discard(HOLD);

            assertEquals(4, bank.available(GoodColor.BLUE),
                    "cubes destroyed with their hold go back on the table");
            assertEquals(0, ship.cargoCount());
        }

        @Test
        @DisplayName("loses its charges, which were never the bank's to begin with")
        void aBatteryLosesItsCharges() {
            GoodsBank bank = Ships.bankOf(STOCK);
            Ship ship = Ships.openShip(bank);
            Ships.put(ship, BATTERY, ComponentKind.BATTERY);
            ship.chargeBatteries();
            assertEquals(2, ship.availableCharges());

            ship.discard(BATTERY);

            assertEquals(0, ship.availableCharges());
            assertEquals(STOCK, bank.stock(), "charges are not cubes and owe the bank nothing");
        }

        @Test
        @DisplayName("loses everybody aboard, because crew destroyed with a cabin are dead")
        void aCabinLosesItsCrew() {
            GoodsBank bank = Ships.bankOf(STOCK);
            Ship ship = Ships.openShip(bank);
            Ships.put(ship, CABIN, ComponentKind.CABIN);
            ship.boardHumansIn(CABIN);
            int aboard = ship.crewCount();
            assertTrue(aboard > 0);

            ship.discard(CABIN);

            assertEquals(aboard - 2, ship.crewCount(), "a cabin holds two");
            assertEquals(STOCK, bank.stock(), "crew are not cubes either");
        }
    }

    @Nested
    @DisplayName("a component destroyed by the route, rather than in the shipyard")
    class WhenShotOff {

        // The tests above go through Ship.discard — a mistake put right in the shipyard, or
        // the trial flight's forgiveness rule. That is one of the two ways a component is
        // scrapped, and much the rarer one. Enemy fire and meteors go through Ship.destroy,
        // and so does every piece that flies away when a ship breaks up. A leak on those
        // paths would be a cube lost on most cards of most flights.

        private static final Position IN_THE_LINE_OF_FIRE = new Position(1, 2);
        private static final int DICE_FOR_THE_CABIN_COLUMN = 6;

        @Test
        @DisplayName("a hold shot off the ship still hands its cubes back")
        void aHoldShotOffGivesItsCargoBack() {
            GoodsBank bank = Ships.bankOf(STOCK);
            Ship ship = Ships.openShip(bank);
            Ships.put(ship, IN_THE_LINE_OF_FIRE, ComponentKind.CARGO_HOLD);
            ship.beginCargoOperations(Map.of(GoodColor.BLUE, 2));
            assertTrue(ship.load(IN_THE_LINE_OF_FIRE, GoodColor.BLUE));
            assertTrue(ship.load(IN_THE_LINE_OF_FIRE, GoodColor.BLUE));
            ship.endCargoOperations();
            assertEquals(2, bank.available(GoodColor.BLUE), "two cubes are aboard");

            DamageReport report = ship.applyHit(
                    new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, DICE_FOR_THE_CABIN_COLUMN),
                    Defence.none());

            assertEquals(DamageReport.Outcome.DESTROYED, report.outcome());
            assertTrue(ship.componentAt(IN_THE_LINE_OF_FIRE).isEmpty());
            assertEquals(4, bank.available(GoodColor.BLUE),
                    "cubes shot off the ship go back on the table, or the bank quietly drains");
        }

        @Test
        @DisplayName("and so does a piece that flies away when the ship comes apart")
        void aFragmentLeftBehindGivesItsCargoBack() {
            GoodsBank bank = Ships.bankOf(STOCK);
            Ship ship = Ships.openShip(bank);
            // The hold hangs off the joint rather than beyond it, so that the joint is the
            // first thing a shot from the west meets: a hit always takes the outermost
            // component in its line, and one aimed past the joint would simply take the hold.
            Position joint = new Position(2, 1);
            Position faraway = new Position(3, 1);
            Ships.put(ship, joint, ComponentKind.STRUCTURAL_MODULE);
            Ships.put(ship, faraway, ComponentKind.CARGO_HOLD);
            ship.beginCargoOperations(Map.of(GoodColor.BLUE, 2));
            assertTrue(ship.load(faraway, GoodColor.BLUE));
            ship.endCargoOperations();
            assertEquals(3, bank.available(GoodColor.BLUE), "one cube is aboard");

            // Take out the joint, and the hold is on a piece of its own.
            ship.applyHit(new Hit(HitKind.HEAVY_FIRE, Direction.WEST, diceForRow(joint)),
                    Defence.none());
            assertFalse(ship.isWhole(), "the ship should be in pieces now");

            ship.keepFragment(ship.pieces().stream()
                    .filter(piece -> piece.contains(Ships.CABIN))
                    .findFirst().orElseThrow());

            assertTrue(ship.componentAt(faraway).isEmpty(), "the far piece flew away");
            assertEquals(4, bank.available(GoodColor.BLUE),
                    "a piece that flies away takes its cargo off the ship, not out of the game");
        }

        /** The roll that names a row, on the five by five fixture board. */
        private static int diceForRow(Position cell) {
            return cell.row() + 5;
        }
    }

    @Nested
    @DisplayName("the default on the interface")
    class TheDefault {

        @Test
        @DisplayName("does nothing, which is what a component holding nothing should do")
        void aComponentHoldingNothingLeavesTheBankAlone() {
            GoodsBank bank = Ships.bankOf(STOCK);
            Tile plate = it.polimi.ingsw.server.model.component.Tiles
                    .of(ComponentKind.STRUCTURAL_MODULE, 0);
            ShipComponent structural = new StructuralComponent(plate,
                    it.polimi.ingsw.common.game.Rotation.NONE);

            structural.scrapped(bank);

            // The default is empty and has to stay reachable: it is the answer for five of the
            // eight kinds, and the thing a ninth kind's author overrides if theirs holds
            // anything. An exception here would make writing a plain component a chore.
            assertEquals(STOCK, bank.stock());
        }
    }
}
