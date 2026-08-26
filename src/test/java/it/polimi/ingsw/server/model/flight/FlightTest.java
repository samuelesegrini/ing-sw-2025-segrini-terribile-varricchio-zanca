package it.polimi.ingsw.server.model.flight;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.server.model.board.LevelSpec;
import it.polimi.ingsw.server.model.component.CabinComponent;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.server.model.ship.Ship;
import it.polimi.ingsw.server.model.ship.Ships;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks who is forced out of a flight and what leaving means, against manual p.20.
 *
 * <p>Giving up is not losing. A player who walks away still sells their goods, at half
 * price, and can still end with more credits than anyone who saw the flight through —
 * the manual is explicit that any profit counts as a win. So a retired player is still
 * in the game and their ship is still scored; only their marker is gone.
 *
 * <p>The timing rule is the one worth guarding: the forced conditions are checked once a
 * card is <em>fully</em> resolved, never in the middle of one. A player who loses their
 * last human partway through a combat zone still suffers the rest of it.
 *
 * <p>Components involved: {@link Flight}, {@link Route}, {@link Ship}.
 */
class FlightTest {

    private static final LevelSpec LEVEL = FlightFixtures.levelSpec(GameLevel.LEVEL_II);

    /** A flight of the given players on the level II start spaces, red furthest ahead. */
    private static Flight flightOf(PlayerColor... players) {
        Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
        Map<PlayerColor, Integer> starts = new LinkedHashMap<>();
        List<Integer> spaces = LEVEL.flightBoard().startingPositions();
        for (int i = 0; i < players.length; i++) {
            Ship ship = Ships.openShip();
            ship.boardHumansIn(Ships.CABIN);
            ships.put(players[i], ship);
            starts.put(players[i], spaces.get(i));
        }
        return new Flight(LEVEL, ships, starts, Dice.scripted(7));
    }

    @Nested
    @DisplayName("launching")
    class Launching {

        @Test
        @DisplayName("every ship starts on its own space, in the order they finished building")
        void everyShipStartsOnItsOwnSpace() {
            Flight flight = flightOf(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN);

            assertEquals(List.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN),
                    flight.stillFlying());
            assertEquals(6, flight.route().positionOf(PlayerColor.RED));
            assertEquals(Set.of(), flight.retired());
        }

        @Test
        @DisplayName("a ship without a start space, or a space without a ship, is refused")
        void mismatchedShipsAndSpacesAreRefused() {
            Map<PlayerColor, Ship> ships = Map.of(PlayerColor.RED, Ships.openShip());

            assertThrows(IllegalArgumentException.class,
                    () -> new Flight(LEVEL, ships, Map.of(PlayerColor.BLUE, 0), Dice.scripted(7)));
        }

        @Test
        @DisplayName("asking after a player who is not in this flight is a programming error")
        void unknownPlayerIsRefused() {
            Flight flight = flightOf(PlayerColor.RED, PlayerColor.BLUE);

            assertThrows(IllegalArgumentException.class, () -> flight.shipOf(PlayerColor.YELLOW));
        }
    }

    @Nested
    @DisplayName("giving up voluntarily")
    class GivingUpVoluntarily {

        @Test
        @DisplayName("the marker leaves the route and no later card touches that player")
        void theMarkerLeavesTheRoute() {
            Flight flight = flightOf(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN);

            flight.giveUp(PlayerColor.BLUE);

            assertTrue(flight.hasGivenUp(PlayerColor.BLUE));
            assertFalse(flight.route().isFlying(PlayerColor.BLUE));
            assertEquals(List.of(PlayerColor.RED, PlayerColor.GREEN), flight.stillFlying());
        }

        @Test
        @DisplayName("the ship stays, because a retired player is still scored")
        void theShipStaysBehindToBeScored() {
            Flight flight = flightOf(PlayerColor.RED, PlayerColor.BLUE);
            Ship blue = flight.shipOf(PlayerColor.BLUE);

            flight.giveUp(PlayerColor.BLUE);

            assertEquals(blue, flight.shipOf(PlayerColor.BLUE));
            assertTrue(flight.players().contains(PlayerColor.BLUE));
        }

        @Test
        @DisplayName("giving up twice is refused")
        void givingUpTwiceIsRefused() {
            Flight flight = flightOf(PlayerColor.RED, PlayerColor.BLUE);
            flight.giveUp(PlayerColor.BLUE);

            assertThrows(IllegalStateException.class, () -> flight.giveUp(PlayerColor.BLUE));
        }
    }

    @Nested
    @DisplayName("forced out")
    class ForcedOut {

        @Test
        @DisplayName("a ship with no humans left cannot fly, because aliens will not do it alone")
        void aShipWithoutHumansIsForcedOut() {
            Flight flight = flightOf(PlayerColor.RED, PlayerColor.BLUE);
            Ship blue = flight.shipOf(PlayerColor.BLUE);
            ((CabinComponent) blue.componentAt(Ships.CABIN).orElseThrow()).evacuate();

            assertEquals(Set.of(PlayerColor.BLUE), flight.playersForcedOut());

            flight.enforceGiveUpRules();

            assertTrue(flight.hasGivenUp(PlayerColor.BLUE));
            assertFalse(flight.hasGivenUp(PlayerColor.RED));
        }

        @Test
        @DisplayName("a ship carrying only an alien is forced out, since an alien is crew but not a pilot")
        void anAlienCrewCannotFlyAlone() {
            Flight flight = flightOf(PlayerColor.RED, PlayerColor.BLUE);
            Ship blue = flight.shipOf(PlayerColor.BLUE);
            Ships.put(blue, new Position(1, 2), ComponentKind.CABIN);
            Ships.put(blue, new Position(0, 2), ComponentKind.PURPLE_LIFE_SUPPORT);
            ((CabinComponent) blue.componentAt(Ships.CABIN).orElseThrow()).evacuate();
            blue.boardAlienIn(new Position(1, 2), AlienColor.PURPLE);

            assertEquals(1, blue.crewCount());
            assertEquals(0, blue.humanCount());
            assertEquals(Set.of(PlayerColor.BLUE), flight.playersForcedOut());
        }

        @Test
        @DisplayName("a lapped ship is forced out")
        void aLappedShipIsForcedOut() {
            Flight flight = flightOf(PlayerColor.RED, PlayerColor.BLUE);
            // Red starts on 6 and blue on 3; twenty-two empty spaces put red more than a lap up.
            flight.route().advance(PlayerColor.RED, 22);

            assertTrue(flight.route().isLapped(PlayerColor.BLUE));
            assertEquals(Set.of(PlayerColor.BLUE), flight.playersForcedOut());
        }

        @Test
        @DisplayName("nobody is forced out while every ship still has a crew and is on the same lap")
        void aHealthyFlightForcesNobodyOut() {
            Flight flight = flightOf(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN);

            assertEquals(Set.of(), flight.playersForcedOut());
            assertEquals(Set.of(), flight.enforceGiveUpRules());
        }

        @Test
        @DisplayName("lapping is measured before anybody leaves, so a departing leader cannot rewrite it")
        void lappingIsMeasuredBeforeAnybodyLeaves() {
            Flight flight = flightOf(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN);
            flight.route().advance(PlayerColor.RED, 22);
            ((CabinComponent) flight.shipOf(PlayerColor.RED)
                    .componentAt(Ships.CABIN).orElseThrow()).evacuate();

            Set<PlayerColor> forced = flight.enforceGiveUpRules();

            assertTrue(forced.contains(PlayerColor.RED), "no humans left");
            assertTrue(forced.contains(PlayerColor.BLUE), "lapped by red, who left in the same breath");
            assertTrue(forced.contains(PlayerColor.GREEN), "lapped as well");
        }
    }

    @Nested
    @DisplayName("flying alone")
    class FlyingAlone {

        @Test
        @DisplayName("one ship left means Combat Zone has nobody to compare against")
        void oneShipLeftIsSolo() {
            Flight flight = flightOf(PlayerColor.RED, PlayerColor.BLUE);

            assertFalse(flight.isSolo());

            flight.giveUp(PlayerColor.BLUE);

            assertTrue(flight.isSolo());
            assertFalse(flight.isDeserted());
        }

        @Test
        @DisplayName("everybody giving up leaves the route deserted")
        void everybodyGivingUpDesertsTheRoute() {
            Flight flight = flightOf(PlayerColor.RED, PlayerColor.BLUE);
            flight.giveUp(PlayerColor.BLUE);
            flight.giveUp(PlayerColor.RED);

            assertTrue(flight.isDeserted());
            assertFalse(flight.isSolo());
            assertEquals(2, flight.players().size(), "both are still in the game, just not in the race");
        }
    }
}
