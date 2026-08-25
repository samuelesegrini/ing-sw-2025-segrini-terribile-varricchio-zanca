package it.polimi.ingsw.server.model.flight;

import it.polimi.ingsw.server.model.player.PlayerColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the loop the ships fly round, against manual p.10 and p.20.
 *
 * <p>Two things here are easy to implement plausibly and wrongly.
 *
 * <p>Movement counts <em>empty</em> spaces: occupied ones are jumped and do not count
 * against the distance. Treating a move as plain addition would quietly shorten every
 * overtake in the game, and Open Space is nothing but overtaking.
 *
 * <p>Positions are absolute rather than modular. A ship a full lap behind the leader has
 * to be distinguishable from one right in front of them, because being lapped is what
 * forces a player out of the race — and modulo arithmetic makes the two identical.
 *
 * <p>Components involved: {@link Route}.
 */
class RouteTest {

    private static final int LEVEL_TWO_LENGTH = 24;

    /** A route with the four level II start spaces occupied, red furthest ahead. */
    private static Route withFourPlayers() {
        Route route = new Route(LEVEL_TWO_LENGTH);
        route.enter(PlayerColor.RED, 6);
        route.enter(PlayerColor.BLUE, 3);
        route.enter(PlayerColor.GREEN, 1);
        route.enter(PlayerColor.YELLOW, 0);
        return route;
    }

    @Nested
    @DisplayName("markers on the route")
    class Markers {

        @Test
        @DisplayName("a marker stands where it entered, and the route knows who is where")
        void markerStandsWhereItEntered() {
            Route route = withFourPlayers();

            assertEquals(6, route.positionOf(PlayerColor.RED));
            assertEquals(6, route.spaceOf(PlayerColor.RED));
            assertEquals(Optional.of(PlayerColor.RED), route.occupantOf(6));
            assertEquals(Optional.empty(), route.occupantOf(5));
            assertEquals(4, route.flyingCount());
        }

        @Test
        @DisplayName("two markers cannot share a space")
        void twoMarkersCannotShareASpace() {
            Route route = withFourPlayers();

            assertThrows(IllegalArgumentException.class, () -> {
                Route other = new Route(LEVEL_TWO_LENGTH);
                other.enter(PlayerColor.RED, 3);
                other.enter(PlayerColor.BLUE, 3);
            });
            assertThrows(IllegalStateException.class, () -> route.enter(PlayerColor.RED, 10));
        }

        @Test
        @DisplayName("a player who gives up leaves the route and is asked nothing more")
        void givingUpTakesTheMarkerOff() {
            Route route = withFourPlayers();

            assertTrue(route.leave(PlayerColor.GREEN));
            assertFalse(route.isFlying(PlayerColor.GREEN));
            assertFalse(route.leave(PlayerColor.GREEN));
            assertThrows(IllegalArgumentException.class, () -> route.positionOf(PlayerColor.GREEN));
            assertEquals(3, route.flyingCount());
        }
    }

    @Nested
    @DisplayName("moving")
    class Moving {

        @Test
        @DisplayName("a ship on a clear stretch travels exactly as far as it declared")
        void clearStretchIsPlainDistance() {
            Route route = withFourPlayers();

            assertEquals(9, route.advance(PlayerColor.RED, 3));
        }

        @Test
        @DisplayName("occupied spaces are jumped and do not count, so an overtake costs nothing extra")
        void occupiedSpacesAreJumpedForFree() {
            Route route = new Route(LEVEL_TWO_LENGTH);
            route.enter(PlayerColor.RED, 0);
            route.enter(PlayerColor.BLUE, 1);
            route.enter(PlayerColor.GREEN, 2);

            assertEquals(4, route.advance(PlayerColor.RED, 2),
                    "two rivals jumped, two empty spaces travelled");
        }

        @Test
        @DisplayName("falling back jumps occupied spaces the same way")
        void fallingBackJumpsTheSameWay() {
            Route route = new Route(LEVEL_TWO_LENGTH);
            route.enter(PlayerColor.RED, 5);
            route.enter(PlayerColor.BLUE, 4);
            route.enter(PlayerColor.GREEN, 3);

            assertEquals(1, route.fallBack(PlayerColor.RED, 2));
        }

        @Test
        @DisplayName("advancing nothing leaves the marker exactly where it was")
        void advancingNothingChangesNothing() {
            Route route = withFourPlayers();

            assertEquals(6, route.advance(PlayerColor.RED, 0));
        }

        @Test
        @DisplayName("a negative distance is refused rather than reversed")
        void negativeDistanceIsRefused() {
            Route route = withFourPlayers();

            assertThrows(IllegalArgumentException.class, () -> route.advance(PlayerColor.RED, -1));
            assertThrows(IllegalArgumentException.class, () -> route.fallBack(PlayerColor.RED, -1));
        }

        @Test
        @DisplayName("moving a ship that has given up is refused")
        void movingAShipThatLeftIsRefused() {
            Route route = withFourPlayers();
            route.leave(PlayerColor.YELLOW);

            assertThrows(IllegalArgumentException.class, () -> route.advance(PlayerColor.YELLOW, 1));
        }

        @Test
        @DisplayName("a ship carries on round the loop rather than stopping at the last space")
        void theRouteIsALoop() {
            Route route = new Route(6);
            route.enter(PlayerColor.RED, 5);

            assertEquals(8, route.advance(PlayerColor.RED, 3));
            assertEquals(2, route.spaceOf(PlayerColor.RED), "three spaces on from the last one");
        }
    }

    @Nested
    @DisplayName("route order")
    class Order {

        @Test
        @DisplayName("route order runs from the leader back, and its reverse is the exact opposite")
        void routeOrderRunsFromTheLeaderBack() {
            Route route = withFourPlayers();

            assertEquals(List.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN, PlayerColor.YELLOW),
                    route.routeOrder());
            assertEquals(List.of(PlayerColor.YELLOW, PlayerColor.GREEN, PlayerColor.BLUE, PlayerColor.RED),
                    route.reverseRouteOrder());
            assertEquals(Optional.of(PlayerColor.RED), route.leader());
        }

        @Test
        @DisplayName("overtaking hands the lead over at once, which is what passes the deck on")
        void overtakingHandsTheLeadOver() {
            Route route = withFourPlayers();

            route.advance(PlayerColor.YELLOW, 8);

            assertEquals(Optional.of(PlayerColor.YELLOW), route.leader());
            assertEquals(PlayerColor.YELLOW, route.routeOrder().getFirst());
        }

        @Test
        @DisplayName("an empty route has no leader rather than an arbitrary one")
        void emptyRouteHasNoLeader() {
            Route route = new Route(LEVEL_TWO_LENGTH);

            assertEquals(Optional.empty(), route.leader());
            assertEquals(List.of(), route.routeOrder());
        }
    }

    @Nested
    @DisplayName("losing flight days together")
    class LosingDaysTogether {

        @Test
        @DisplayName("players fall back in reverse route order, the one furthest behind moving first")
        void furthestBehindMovesFirst() {
            Route route = new Route(LEVEL_TWO_LENGTH);
            route.enter(PlayerColor.RED, 10);
            route.enter(PlayerColor.BLUE, 8);

            route.loseFlightDaysTogether(Map.of(PlayerColor.RED, 3, PlayerColor.BLUE, 1));

            assertEquals(7, route.positionOf(PlayerColor.BLUE), "blue moved first, into a clear space");
            assertEquals(6, route.positionOf(PlayerColor.RED), "red then jumped blue's new space");
        }

        @Test
        @DisplayName("players not named in the card are left where they are")
        void unnamedPlayersDoNotMove() {
            Route route = withFourPlayers();

            route.loseFlightDaysTogether(Map.of(PlayerColor.RED, 2));

            assertEquals(3, route.positionOf(PlayerColor.BLUE));
            assertEquals(4, route.positionOf(PlayerColor.RED));
        }
    }

    @Nested
    @DisplayName("lapping")
    class Lapping {

        /**
         * A gap of exactly one lap cannot happen: it would put both markers on the same
         * space, and two markers never share one. So the first gap that counts as lapped
         * is a full lap plus one space, and the largest that does not is a lap minus one.
         */
        @Test
        @DisplayName("almost a full lap behind is still flying")
        void almostALapBehindIsStillFlying() {
            Route route = new Route(10);
            route.enter(PlayerColor.RED, 0);
            route.enter(PlayerColor.BLUE, 1);

            route.advance(PlayerColor.BLUE, 8);

            assertEquals(9, route.gapToLeader(PlayerColor.RED));
            assertFalse(route.isLapped(PlayerColor.RED));
            assertEquals(Set.of(), route.lappedPlayers());
        }

        @Test
        @DisplayName("a lap and one space behind is lapped, and that forces the player out")
        void moreThanALapBehindIsLapped() {
            Route route = new Route(10);
            route.enter(PlayerColor.RED, 0);
            route.enter(PlayerColor.BLUE, 1);

            route.advance(PlayerColor.BLUE, 9);

            assertEquals(11, route.gapToLeader(PlayerColor.RED));
            assertTrue(route.isLapped(PlayerColor.RED));
            assertEquals(Set.of(PlayerColor.RED), route.lappedPlayers());
        }

        @Test
        @DisplayName("a lapped ship can look like it is right next door, which is what modular positions would hide")
        void lappedShipCanLookAdjacent() {
            Route route = new Route(10);
            route.enter(PlayerColor.RED, 0);
            route.enter(PlayerColor.BLUE, 1);
            route.advance(PlayerColor.BLUE, 9);

            assertEquals(0, route.spaceOf(PlayerColor.RED));
            assertEquals(1, route.spaceOf(PlayerColor.BLUE));
            assertEquals(11, route.positionOf(PlayerColor.BLUE),
                    "side by side on the board, a whole lap apart in the race");
            assertTrue(route.isLapped(PlayerColor.RED));
        }

        @Test
        @DisplayName("a ship passing its own vacated space counts it, because the marker has been picked up")
        void ownVacatedSpaceCounts() {
            Route route = new Route(10);
            route.enter(PlayerColor.RED, 0);
            route.enter(PlayerColor.BLUE, 1);

            route.advance(PlayerColor.BLUE, 9);

            assertEquals(11, route.positionOf(PlayerColor.BLUE),
                    "eight clear spaces, red jumped for free, then blue's own old space and one more");
        }

        @Test
        @DisplayName("the leader is never lapped and the gap to themselves is nothing")
        void leaderIsNeverLapped() {
            Route route = withFourPlayers();

            assertEquals(0, route.gapToLeader(PlayerColor.RED));
            assertFalse(route.isLapped(PlayerColor.RED));
        }
    }

    @Test
    @DisplayName("standings read from the leader back and cannot be edited through the view")
    void standingsReadFromTheLeaderBack() {
        Route route = withFourPlayers();

        assertEquals(List.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN, PlayerColor.YELLOW),
                List.copyOf(route.standings().keySet()));
        assertThrows(UnsupportedOperationException.class,
                () -> route.standings().put(PlayerColor.RED, 99));
        assertEquals(Set.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN, PlayerColor.YELLOW),
                route.players());
    }
}
