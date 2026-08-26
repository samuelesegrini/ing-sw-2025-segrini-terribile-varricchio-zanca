package it.polimi.ingsw.server.model.ship;

import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.server.model.board.ShipBoardSpec;
import it.polimi.ingsw.server.model.component.ComponentTile;
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
 * Checks who may live where, against manual p.18.
 *
 * <p>The rule that carries the most weight is that a life support module has to be
 * <em>welded</em> to a cabin, not merely sitting next to it. A ship built with the two
 * side by side but not joined looks right on the board and supports nobody, and a model
 * that tested adjacency would hand its owner an alien they had not earned.
 *
 * <p>The rest are limits worth holding exactly: one alien of each colour per ship, never
 * an alien in the starting cabin, and an alien displacing two humans while counting as
 * one crew member.
 *
 * <p>Components involved: {@link Ship}, {@link AlienColor}.
 */
class CrewPlacementTest {

    private static final Position CABIN = new Position(2, 2);

    private static Ship ship() {
        return Ships.openShip();
    }

    private static void put(Ship ship, Position cell, ComponentKind kind) {
        Ships.put(ship, cell, kind);
    }

    /**
     * Places a tile whose north side is smooth and whose other three are universal.
     *
     * <p>Two of these stacked one above the other are legally adjacent — smooth against
     * smooth is allowed — and yet not welded to each other, which is the case that
     * separates interconnection from adjacency.
     */
    private static void putSmoothNorthSide(Ship ship, Position cell, ComponentKind kind) {
        ship.place(cell, new ComponentTile(kind + "-smooth-top", kind,
                Tiles.sides(Connector.PLAIN, Connector.UNIVERSAL,
                        Connector.UNIVERSAL, Connector.UNIVERSAL), 0), Rotation.NONE);
    }

    /** As above, but smooth on the south side. */
    private static void putSmoothSouthSide(Ship ship, Position cell, ComponentKind kind) {
        ship.place(cell, new ComponentTile(kind + "-smooth-bottom", kind,
                Tiles.sides(Connector.UNIVERSAL, Connector.UNIVERSAL,
                        Connector.PLAIN, Connector.UNIVERSAL), 0), Rotation.NONE);
    }

    @Nested
    @DisplayName("where an alien may live")
    class WhereAnAlienMayLive {

        @Test
        @DisplayName("a cabin with no life support takes humans and nothing else")
        void plainCabinTakesHumansOnly() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN);

            assertEquals(Set.of(), ship.aliensAllowedIn(new Position(1, 2)));
            assertThrows(IllegalArgumentException.class,
                    () -> ship.boardAlienIn(new Position(1, 2), AlienColor.PURPLE));
        }

        @Test
        @DisplayName("a cabin welded to a matching module takes that species")
        void weldedModuleOpensTheCabin() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN);
            put(ship, new Position(0, 2), ComponentKind.PURPLE_LIFE_SUPPORT);

            assertEquals(Set.of(AlienColor.PURPLE), ship.aliensAllowedIn(new Position(1, 2)));
        }

        @Test
        @DisplayName("a module merely next door supports nobody, on a ship that is otherwise perfectly legal")
        void adjacentButUnjoinedModuleSupportsNobody() {
            Ship ship = ship();
            // The module reaches the ship the long way round, through the two hull pieces to
            // its left, and meets the cabin below it smooth against smooth — legal, and not a
            // joint. This is the ship a player builds by accident and then wonders about.
            put(ship, new Position(2, 1), ComponentKind.STRUCTURAL_MODULE);
            put(ship, new Position(1, 1), ComponentKind.STRUCTURAL_MODULE);
            putSmoothNorthSide(ship, new Position(1, 2), ComponentKind.CABIN);
            put(ship, new Position(0, 1), ComponentKind.STRUCTURAL_MODULE);
            putSmoothSouthSide(ship, new Position(0, 2), ComponentKind.PURPLE_LIFE_SUPPORT);

            assertTrue(ship.validate().isLegal(),
                    "smooth against smooth is a legal way to build, and still not a joint");
            assertEquals(Set.of(), ship.aliensAllowedIn(new Position(1, 2)));
            assertFalse(ship.isLifeSupported(new Position(1, 2), AlienColor.PURPLE));
        }

        @Test
        @DisplayName("a cabin welded to both colours offers a real choice between them")
        void bothColoursOfferAChoice() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN);
            put(ship, new Position(0, 2), ComponentKind.PURPLE_LIFE_SUPPORT);
            put(ship, new Position(1, 1), ComponentKind.BROWN_LIFE_SUPPORT);

            assertEquals(Set.of(AlienColor.PURPLE, AlienColor.BROWN),
                    ship.aliensAllowedIn(new Position(1, 2)));

            ship.boardAlienIn(new Position(1, 2), AlienColor.BROWN);
            assertEquals(Set.of(AlienColor.BROWN), ship.aliens());
        }

        @Test
        @DisplayName("the starting cabin never takes an alien, however it is fitted out")
        void startingCabinNeverTakesAnAlien() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.PURPLE_LIFE_SUPPORT);

            assertEquals(Set.of(), ship.aliensAllowedIn(CABIN));
            assertThrows(IllegalArgumentException.class, () -> ship.boardAlienIn(CABIN, AlienColor.PURPLE));

            ship.boardHumansIn(CABIN);
            assertEquals(2, ship.humanCount());
        }

        @Test
        @DisplayName("asking about a cell that is not a cabin is a programming error")
        void askingAboutANonCabinIsRefused() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.BATTERY);

            assertThrows(IllegalArgumentException.class, () -> ship.aliensAllowedIn(new Position(1, 2)));
            assertThrows(IllegalArgumentException.class, () -> ship.boardHumansIn(new Position(0, 0)));
        }
    }

    @Nested
    @DisplayName("one alien of each colour")
    class OneOfEachColour {

        @Test
        @DisplayName("a second alien of the same colour is refused, however many cabins are fitted for it")
        void secondAlienOfTheSameColourIsRefused() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN);
            put(ship, new Position(0, 2), ComponentKind.PURPLE_LIFE_SUPPORT);
            put(ship, new Position(2, 1), ComponentKind.CABIN);
            put(ship, new Position(1, 1), ComponentKind.PURPLE_LIFE_SUPPORT);

            ship.boardAlienIn(new Position(1, 2), AlienColor.PURPLE);

            assertEquals(Set.of(), ship.aliensAllowedIn(new Position(2, 1)));
            assertThrows(IllegalArgumentException.class,
                    () -> ship.boardAlienIn(new Position(2, 1), AlienColor.PURPLE));
        }

        @Test
        @DisplayName("one of each colour is allowed, and together they count as two crew")
        void oneOfEachColourIsAllowed() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN);
            put(ship, new Position(0, 2), ComponentKind.PURPLE_LIFE_SUPPORT);
            put(ship, new Position(2, 1), ComponentKind.CABIN);
            put(ship, new Position(1, 1), ComponentKind.BROWN_LIFE_SUPPORT);

            ship.boardAlienIn(new Position(1, 2), AlienColor.PURPLE);
            ship.boardAlienIn(new Position(2, 1), AlienColor.BROWN);

            assertEquals(Set.of(AlienColor.PURPLE, AlienColor.BROWN), ship.aliens());
            assertEquals(2, ship.crewCount());
            assertEquals(0, ship.humanCount());
        }
    }

    @Nested
    @DisplayName("filling the ship")
    class FillingTheShip {

        @Test
        @DisplayName("every cabin ends up occupied, because the manual leaves no room for an empty one")
        void everyCabinEndsUpOccupied() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN);
            put(ship, new Position(2, 1), ComponentKind.CABIN);

            assertFalse(ship.crewIsAboard());

            assertEquals(3, ship.fillRemainingCabinsWithHumans());

            assertTrue(ship.crewIsAboard());
            assertEquals(6, ship.crewCount());
        }

        @Test
        @DisplayName("filling up leaves an alien where the player put it")
        void fillingUpLeavesTheAlienAlone() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN);
            put(ship, new Position(0, 2), ComponentKind.BROWN_LIFE_SUPPORT);
            ship.boardAlienIn(new Position(1, 2), AlienColor.BROWN);

            assertEquals(1, ship.fillRemainingCabinsWithHumans(), "only the starting cabin was empty");
            assertEquals(3, ship.crewCount(), "two humans and one alien");
            assertEquals(Set.of(AlienColor.BROWN), ship.aliens());
        }

        @Test
        @DisplayName("the berths on offer are the empty cabins that could take an alien")
        void berthsOnOfferAreTheEmptyFittedCabins() {
            Ship ship = ship();
            put(ship, new Position(1, 2), ComponentKind.CABIN);
            put(ship, new Position(0, 2), ComponentKind.PURPLE_LIFE_SUPPORT);
            put(ship, new Position(2, 1), ComponentKind.CABIN);

            assertEquals(Set.of(new Position(1, 2)), ship.alienBerths().keySet());
            assertEquals(Set.of(AlienColor.PURPLE), ship.alienBerths().get(new Position(1, 2)));

            ship.boardAlienIn(new Position(1, 2), AlienColor.PURPLE);

            assertEquals(Set.of(), ship.alienBerths().keySet(), "the berth is taken");
        }

        @Test
        @DisplayName("a cabin that already has crew takes nobody else")
        void occupiedCabinTakesNobodyElse() {
            Ship ship = ship();
            ship.boardHumansIn(CABIN);

            assertThrows(IllegalStateException.class, () -> ship.boardHumansIn(CABIN));
        }
    }
}
