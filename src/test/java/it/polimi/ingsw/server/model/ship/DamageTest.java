package it.polimi.ingsw.server.model.ship;

import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.DamageReport;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.Hit;
import it.polimi.ingsw.common.game.HitKind;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.server.model.board.ShipBoardSpec;
import it.polimi.ingsw.server.model.component.CabinComponent;
import it.polimi.ingsw.server.model.component.ComponentTile;
import it.polimi.ingsw.server.model.component.Tiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what happens when something hits a ship, against manual p.10, p.13 and p.19.
 *
 * <p>The four kinds of threat differ in exactly the ways that make defending interesting,
 * and each difference is worth a test of its own. A small meteor bounces off a smooth
 * side for nothing — which is the reason a tidy ship survives a swarm. A shield stops it
 * and light fire, for a battery. Only a cannon stops a big meteor, and only from the
 * right place. Nothing stops heavy fire.
 *
 * <p>The board's printed row and column labels run 5 to 9 and 4 to 10, so a roll of two
 * dice can easily name no line at all. That is a clean miss and a normal outcome, not an
 * error.
 *
 * <p>Components involved: {@link Ship}, {@link Hit}, {@link Defence}, {@link DamageReport}.
 */
class DamageTest {

    private static final Position CABIN = new Position(2, 2);

    /** A five by five board, so printed columns run 4 to 8 and rows 5 to 9. */
    private static Ship ship() {
        return Ships.openShip();
    }

    private static void put(Ship ship, Position cell, ComponentKind kind, Rotation rotation) {
        Ships.put(ship, cell, kind, rotation);
    }

    /** Puts a structural module with a smooth north side where a meteor can bounce off it. */
    private static void putSmoothNorthSide(Ship ship, Position cell) {
        ship.place(cell, new ComponentTile("smooth-top", ComponentKind.STRUCTURAL_MODULE,
                Tiles.sides(Connector.PLAIN, Connector.UNIVERSAL,
                        Connector.UNIVERSAL, Connector.UNIVERSAL), 0), Rotation.NONE);
    }

    /** Column 2 of the grid is printed as column 6, so two dice summing to 6 name it. */
    private static final int DICE_FOR_CABIN_COLUMN = 6;

    @Nested
    @DisplayName("finding the target")
    class FindingTheTarget {

        @Test
        @DisplayName("a threat from the bow strikes the topmost component of its column")
        void threatFromTheBowStrikesTheTopmostComponent() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN, Rotation.NONE);

            Hit hit = new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, DICE_FOR_CABIN_COLUMN);

            assertEquals(Optional.of(new Position(1, 2)), ship.targetOf(hit));
        }

        @Test
        @DisplayName("a threat from the stern strikes the bottommost component instead")
        void threatFromTheSternStrikesTheBottommostComponent() {
            Ship ship = ship();
            put(ship, new Position(3, 2), ComponentKind.CABIN, Rotation.NONE);

            Hit hit = new Hit(HitKind.HEAVY_FIRE, Direction.SOUTH, DICE_FOR_CABIN_COLUMN);

            assertEquals(Optional.of(new Position(3, 2)), ship.targetOf(hit));
        }

        @Test
        @DisplayName("a roll that names no line on this board misses cleanly")
        void rollOutsideThePrintedLabelsMisses() {
            Ship ship = ship();

            assertEquals(Optional.empty(), ship.targetOf(new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 3)));
            assertEquals(Optional.empty(), ship.targetOf(new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 12)));
            assertEquals(Optional.empty(), ship.targetOf(new Hit(HitKind.HEAVY_FIRE, Direction.EAST, 4)));
        }

        @Test
        @DisplayName("a roll naming an empty line misses too, and leaves the ship whole")
        void rollDownAnEmptyLineMisses() {
            Ship ship = ship();

            DamageReport report = ship.applyHit(
                    new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 4), Defence.none());

            assertEquals(DamageReport.Outcome.MISSED, report.outcome());
            assertTrue(report.shipIsIntact());
            assertEquals(1, ship.components().size());
        }

        @Test
        @DisplayName("two dice cannot sum to one or thirteen, and pretending otherwise is refused")
        void impossibleDiceSumsAreRefused() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Hit(HitKind.SMALL_METEOR, Direction.NORTH, 1));
            assertThrows(IllegalArgumentException.class,
                    () -> new Hit(HitKind.SMALL_METEOR, Direction.NORTH, 13));
        }
    }

    @Nested
    @DisplayName("small meteors")
    class SmallMeteors {

        @Test
        @DisplayName("a small meteor bounces off a smooth side for nothing, which is why tidy ships survive swarms")
        void bouncesOffASmoothSide() {
            Ship ship = ship();
            putSmoothNorthSide(ship, new Position(1, 2));

            DamageReport report = ship.applyHit(
                    new Hit(HitKind.SMALL_METEOR, Direction.NORTH, DICE_FOR_CABIN_COLUMN), Defence.none());

            assertEquals(DamageReport.Outcome.BOUNCED, report.outcome());
            assertEquals(2, ship.components().size());
        }

        @Test
        @DisplayName("a small meteor destroys an exposed connector when nothing is done about it")
        void destroysAnExposedConnector() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN, Rotation.NONE);

            DamageReport report = ship.applyHit(
                    new Hit(HitKind.SMALL_METEOR, Direction.NORTH, DICE_FOR_CABIN_COLUMN), Defence.none());

            assertEquals(DamageReport.Outcome.DESTROYED, report.outcome());
            assertEquals(Optional.of(new Position(1, 2)), report.destroyedIfAny());
            assertTrue(ship.componentAt(new Position(1, 2)).isEmpty());
        }

        @Test
        @DisplayName("a shield turned the right way stops it, for a battery")
        void aShieldStopsIt() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.SHIELD, Rotation.NONE);
            put(ship, new Position(2, 3), ComponentKind.BATTERY, Rotation.NONE);
            ship.chargeBatteries();

            DamageReport report = ship.applyHit(
                    new Hit(HitKind.SMALL_METEOR, Direction.NORTH, DICE_FOR_CABIN_COLUMN),
                    Defence.using(new Position(2, 1)));

            assertEquals(DamageReport.Outcome.DEFENDED, report.outcome());
            assertEquals(1, ship.availableCharges(), "the shield cost one charge");
        }

        @Test
        @DisplayName("a shield facing the wrong way is refused rather than silently wasted")
        void aShieldFacingTheWrongWayIsRefused() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.SHIELD, Rotation.CLOCKWISE_180);
            put(ship, new Position(2, 3), ComponentKind.BATTERY, Rotation.NONE);
            ship.chargeBatteries();

            Hit hit = new Hit(HitKind.SMALL_METEOR, Direction.NORTH, DICE_FOR_CABIN_COLUMN);

            assertEquals(Set.of(), ship.defencesAgainst(hit));
            assertThrows(IllegalArgumentException.class,
                    () -> ship.applyHit(hit, Defence.using(new Position(2, 1))));
        }

        @Test
        @DisplayName("a meteor that was going to bounce costs no battery, even if a shield is offered")
        void bouncingBeatsDefending() {
            Ship ship = ship();
            putSmoothNorthSide(ship, new Position(1, 2));
            put(ship, new Position(2, 1), ComponentKind.SHIELD, Rotation.NONE);
            put(ship, new Position(2, 3), ComponentKind.BATTERY, Rotation.NONE);
            ship.chargeBatteries();

            DamageReport report = ship.applyHit(
                    new Hit(HitKind.SMALL_METEOR, Direction.NORTH, DICE_FOR_CABIN_COLUMN),
                    Defence.using(new Position(2, 1)));

            assertEquals(DamageReport.Outcome.BOUNCED, report.outcome());
            assertEquals(2, ship.availableCharges(), "no charge was spent on a meteor that bounced");
        }
    }

    @Nested
    @DisplayName("big meteors")
    class BigMeteors {

        @Test
        @DisplayName("a big meteor ignores a smooth side, unlike a small one")
        void ignoresSmoothSides() {
            Ship ship = ship();
            putSmoothNorthSide(ship, new Position(1, 2));

            DamageReport report = ship.applyHit(
                    new Hit(HitKind.BIG_METEOR, Direction.NORTH, DICE_FOR_CABIN_COLUMN), Defence.none());

            assertEquals(DamageReport.Outcome.DESTROYED, report.outcome());
        }

        @Test
        @DisplayName("a shield is useless against a big meteor")
        void aShieldIsUseless() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.SHIELD, Rotation.NONE);
            put(ship, new Position(2, 3), ComponentKind.BATTERY, Rotation.NONE);
            ship.chargeBatteries();

            Hit hit = new Hit(HitKind.BIG_METEOR, Direction.NORTH, DICE_FOR_CABIN_COLUMN);

            assertFalse(ship.defencesAgainst(hit).contains(new Position(2, 1)));
        }

        @Test
        @DisplayName("one coming at the bow may be shot only by a forward cannon in its own column")
        void fromTheBowOnlyItsOwnColumnCounts() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.SINGLE_CANNON, Rotation.NONE);
            put(ship, new Position(3, 2), ComponentKind.SINGLE_CANNON, Rotation.NONE);

            Hit hit = new Hit(HitKind.BIG_METEOR, Direction.NORTH, DICE_FOR_CABIN_COLUMN);

            assertEquals(Set.of(new Position(3, 2)), ship.defencesAgainst(hit),
                    "the cannon one column over cannot reach it");
        }

        @Test
        @DisplayName("one coming from a side may be shot from a neighbouring row as well as its own")
        void fromASideNeighbouringRowsCount() {
            Ship ship = ship();
            put(ship, new Position(2, 3), ComponentKind.CABIN, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.SINGLE_CANNON, Rotation.CLOCKWISE_90);
            put(ship, new Position(1, 2), ComponentKind.SINGLE_CANNON, Rotation.CLOCKWISE_90);
            put(ship, new Position(3, 2), ComponentKind.SINGLE_CANNON, Rotation.CLOCKWISE_90);

            Hit hit = new Hit(HitKind.BIG_METEOR, Direction.EAST, 7);

            assertEquals(Set.of(new Position(2, 1), new Position(1, 2), new Position(3, 2)),
                    ship.defencesAgainst(hit));
        }

        @Test
        @DisplayName("a cannon pointing the wrong way cannot shoot it, whatever row it is in")
        void aCannonPointingTheWrongWayCannotShoot() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN, Rotation.NONE);
            put(ship, new Position(3, 2), ComponentKind.SINGLE_CANNON, Rotation.CLOCKWISE_180);

            Hit hit = new Hit(HitKind.BIG_METEOR, Direction.NORTH, DICE_FOR_CABIN_COLUMN);

            assertEquals(Set.of(), ship.defencesAgainst(hit));
        }

        @Test
        @DisplayName("a single cannon shoots for free while a double one costs a battery")
        void doubleCannonsCostACharge() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN, Rotation.NONE);
            put(ship, new Position(3, 2), ComponentKind.SINGLE_CANNON, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.BATTERY, Rotation.NONE);
            ship.chargeBatteries();

            ship.applyHit(new Hit(HitKind.BIG_METEOR, Direction.NORTH, DICE_FOR_CABIN_COLUMN),
                    Defence.using(new Position(3, 2)));

            assertEquals(2, ship.availableCharges(), "a single cannon needs no charge");
        }
    }

    @Nested
    @DisplayName("cannon fire")
    class CannonFire {

        @Test
        @DisplayName("light fire is stopped by a shield and by nothing else")
        void lightFireIsStoppedByAShield() {
            Ship ship = ship();
            putSmoothNorthSide(ship, new Position(1, 2));
            put(ship, new Position(2, 1), ComponentKind.SHIELD, Rotation.NONE);
            put(ship, new Position(2, 3), ComponentKind.BATTERY, Rotation.NONE);
            ship.chargeBatteries();

            Hit hit = new Hit(HitKind.LIGHT_FIRE, Direction.NORTH, DICE_FOR_CABIN_COLUMN);

            assertEquals(DamageReport.Outcome.DEFENDED,
                    ship.applyHit(hit, Defence.using(new Position(2, 1))).outcome());
        }

        @Test
        @DisplayName("light fire does not bounce off a smooth side, unlike a small meteor")
        void lightFireDoesNotBounce() {
            Ship ship = ship();
            putSmoothNorthSide(ship, new Position(1, 2));

            DamageReport report = ship.applyHit(
                    new Hit(HitKind.LIGHT_FIRE, Direction.NORTH, DICE_FOR_CABIN_COLUMN), Defence.none());

            assertEquals(DamageReport.Outcome.DESTROYED, report.outcome());
        }

        @Test
        @DisplayName("nothing at all stops heavy fire")
        void nothingStopsHeavyFire() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN, Rotation.NONE);
            put(ship, new Position(2, 1), ComponentKind.SHIELD, Rotation.NONE);
            put(ship, new Position(3, 2), ComponentKind.SINGLE_CANNON, Rotation.NONE);
            put(ship, new Position(2, 3), ComponentKind.BATTERY, Rotation.NONE);
            ship.chargeBatteries();

            Hit hit = new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, DICE_FOR_CABIN_COLUMN);

            assertEquals(Set.of(), ship.defencesAgainst(hit));
            assertEquals(DamageReport.Outcome.DESTROYED, ship.applyHit(hit, Defence.none()).outcome());
        }
    }

    @Nested
    @DisplayName("what a loss takes with it")
    class Consequences {

        @Test
        @DisplayName("a destroyed hold sends its cubes back to the bank at once")
        void aDestroyedHoldReturnsItsCargo() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CARGO_HOLD, Rotation.NONE);
            var hold = (it.polimi.ingsw.server.model.component.CargoHoldComponent)
                    ship.componentAt(new Position(1, 2)).orElseThrow();
            hold.store(GoodColor.BLUE);

            ship.applyHit(new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, DICE_FOR_CABIN_COLUMN), Defence.none());

            assertEquals(0, hold.load());
        }

        @Test
        @DisplayName("an alien whose life support is destroyed leaves with it")
        void anAlienLosesItsLifeSupport() {
            Ship ship = ship();
            put(ship, new Position(2, 1), ComponentKind.CABIN, Rotation.NONE);
            put(ship, new Position(1, 1), ComponentKind.PURPLE_LIFE_SUPPORT, Rotation.NONE);
            CabinComponent cabin = (CabinComponent) ship.componentAt(new Position(2, 1)).orElseThrow();
            cabin.boardAlien(AlienColor.PURPLE);

            assertEquals(1, ship.crewCount());

            // Printed column 5 is grid column 1, so two dice summing to 5 name the module's column.
            ship.applyHit(new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 5), Defence.none());

            assertEquals(Optional.empty(), cabin.alien(), "the alien left in an escape pod");
            assertEquals(0, ship.crewCount());
        }

        @Test
        @DisplayName("a cabin joined to a matching module is life supported, and one merely next door is not")
        void lifeSupportNeedsAJoint() {
            Ship ship = ship();
            put(ship, new Position(2, 1), ComponentKind.CABIN, Rotation.NONE);
            put(ship, new Position(1, 1), ComponentKind.PURPLE_LIFE_SUPPORT, Rotation.NONE);

            assertTrue(ship.isLifeSupported(new Position(2, 1), AlienColor.PURPLE));
            assertFalse(ship.isLifeSupported(new Position(2, 1), AlienColor.BROWN));
        }
    }

    @Nested
    @DisplayName("a ship that comes apart")
    class BreakingUp {

        /** A ship shaped like a bar, so destroying the middle leaves two pieces. */
        private Ship barShip() {
            Ship ship = ship();
            put(ship, new Position(2, 1), ComponentKind.STRUCTURAL_MODULE, Rotation.NONE);
            put(ship, new Position(2, 0), ComponentKind.CABIN, Rotation.NONE);
            return ship;
        }

        @Test
        @DisplayName("destroying the middle of a ship leaves the player a choice of pieces")
        void destroyingTheMiddleLeavesAChoice() {
            Ship ship = barShip();

            // Printed column 5 is grid column 1, the middle of the bar.
            DamageReport report = ship.applyHit(
                    new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 5), Defence.none());

            assertTrue(report.brokeUp());
            assertEquals(2, report.fragments().size());
            assertFalse(ship.isWhole());
        }

        @Test
        @DisplayName("the piece the player keeps flies on and the rest is lost along the route")
        void theChosenPieceFliesOn() {
            Ship ship = barShip();
            ship.applyHit(new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 5), Defence.none());

            Set<Position> lost = ship.keepFragment(Set.of(CABIN));

            assertEquals(Set.of(new Position(2, 0)), lost);
            assertEquals(Set.of(CABIN), ship.components().keySet());
            assertTrue(ship.isWhole());
        }

        @Test
        @DisplayName("keeping a piece the ship is not in is refused")
        void keepingANonExistentPieceIsRefused() {
            Ship ship = barShip();
            ship.applyHit(new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 5), Defence.none());

            assertThrows(IllegalArgumentException.class,
                    () -> ship.keepFragment(Set.of(CABIN, new Position(2, 0))));
        }

        @Test
        @DisplayName("nothing else happens to a ship in pieces until the player has chosen")
        void nothingHappensUntilTheChoiceIsMade() {
            Ship ship = barShip();
            ship.applyHit(new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 5), Defence.none());

            assertThrows(IllegalStateException.class,
                    () -> ship.applyHit(new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 6), Defence.none()));
        }

        @Test
        @DisplayName("losing a component at the edge leaves one piece and no choice to make")
        void losingAnEdgeComponentLeavesNoChoice() {
            Ship ship = barShip();

            // Printed column 4 is grid column 0, the far end of the bar.
            DamageReport report = ship.applyHit(
                    new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 4), Defence.none());

            assertFalse(report.brokeUp());
            assertTrue(ship.isWhole());
            assertEquals(2, ship.components().size());
        }
    }
}
