package it.polimi.ingsw.server.projection;

import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.common.game.ViolationKind;
import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.AdventureCardType;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.view.BuildingView;
import it.polimi.ingsw.common.protocol.view.CellView;
import it.polimi.ingsw.common.protocol.view.FlightView;
import it.polimi.ingsw.common.protocol.view.ShipView;
import it.polimi.ingsw.server.model.building.BuildingFixtures;
import it.polimi.ingsw.server.model.building.BuildingTimer;
import it.polimi.ingsw.server.model.building.StartSpacePolicy;
import it.polimi.ingsw.server.model.building.StartSpaces;
import it.polimi.ingsw.server.model.component.Tiles;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.flight.FlightFixtures;
import it.polimi.ingsw.server.model.ship.Ship;
import it.polimi.ingsw.server.model.ship.Ships;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.InstantSource;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a view says what the model says.
 *
 * <p>The interesting failures in a projection are not crashes. They are a cell that draws as
 * empty because nobody wrote its case, a firepower rounded on the way out, or a hole in the
 * printed board that a client offers to build on. All three are silent, and all three are
 * what these tests are for.
 *
 * <p>Components involved: {@link Projections}, {@link ComponentKind}, {@link Ship}.
 */
class ProjectionsTest {

    private static final Position CABIN = Ships.CABIN;
    private static final Position EAST = new Position(2, 3);
    private static final Position WEST = new Position(2, 1);

    @Nested
    @DisplayName("a ship")
    class Ships_ {

        @Test
        @DisplayName("a battery shows its charges, a hold its cubes, a cabin its people")
        void everyKindShowsWhatItHolds() {
            Ship ship = Ships.openShip();
            Ships.put(ship, EAST, ComponentKind.BATTERY);
            Ships.put(ship, WEST, ComponentKind.CARGO_HOLD);
            ship.chargeBatteries();
            ship.beginCargoOperations(java.util.Map.of(GoodColor.BLUE, 1, GoodColor.GREEN, 1));
            ship.load(WEST, GoodColor.BLUE);
            ship.load(WEST, GoodColor.GREEN);
            ship.endCargoOperations();
            ship.boardHumansIn(CABIN);

            ShipView view = Projections.of(ship);

            assertEquals(2, view.cells().get(EAST).batteries(), "a full battery holds two");
            assertEquals(Set.of(GoodColor.BLUE, GoodColor.GREEN),
                    Set.copyOf(view.cells().get(WEST).cargo()),
                    "both cubes are aboard; the order a hold keeps them in is its own business");
            assertEquals(2, view.cells().get(CABIN).humans());
        }

        @Test
        @DisplayName("an alien in a cabin is shown, and nobody is shown beside it")
        void aliensAreVisible() {
            Ship ship = Ships.openShip();
            Ships.put(ship, EAST, ComponentKind.CABIN);
            Ships.put(ship, new Position(1, 3), ComponentKind.PURPLE_LIFE_SUPPORT);
            ship.boardAlienIn(EAST, AlienColor.PURPLE);

            CellView cell = Projections.of(ship).cells().get(EAST);

            assertEquals(AlienColor.PURPLE, cell.alien());
            assertEquals(0, cell.humans(), "a cabin holds people or an alien, never both");
        }

        @Test
        @DisplayName("a component that holds nothing is still drawn")
        void componentsWithNothingInThem() {
            Ship ship = Ships.openShip();
            Ships.put(ship, EAST, ComponentKind.SINGLE_CANNON);
            Ships.put(ship, WEST, ComponentKind.SHIELD);

            ShipView view = Projections.of(ship);

            assertEquals(ComponentKind.SINGLE_CANNON, view.cells().get(EAST).tile().kind());
            assertEquals(ComponentKind.SHIELD, view.cells().get(WEST).tile().kind());
            assertTrue(view.cells().get(EAST).cargo().isEmpty());
            assertEquals(0, view.cells().get(EAST).batteries());
        }

        @Test
        @DisplayName("connectors are given as they face, so a view need not know how a tile turns")
        void connectorsAreAlreadyRotated() {
            Ship ship = Ships.openShip();
            ship.place(EAST, Tiles.of(ComponentKind.STRUCTURAL_MODULE), Rotation.NONE);
            Connector north = ship.componentAt(EAST).orElseThrow().connectorFacing(Direction.NORTH);

            ship.lift(EAST);
            ship.place(EAST, Tiles.of(ComponentKind.STRUCTURAL_MODULE), Rotation.CLOCKWISE_90);
            CellView turned = Projections.of(ship).cells().get(EAST);

            assertEquals(Rotation.CLOCKWISE_90, turned.tile().rotation(), "the artwork still turns");
            assertEquals(north, turned.tile().connectors().get(Direction.EAST),
                    "what faced north now faces east, and the view is told so");
        }

        @Test
        @DisplayName("the outline says where a tile may go, which an empty square cannot")
        void theOutlineIsSent() {
            Ship ship = Ships.openShip();

            ShipView view = Projections.of(ship);

            assertEquals(view.rows() * view.columns(), view.outline().size(),
                    "this fixture has an unbroken board");
            assertTrue(view.outline().contains(CABIN));
            assertTrue(view.outline().containsAll(view.cells().keySet()),
                    "nothing may be welded off the board");
        }

        @Test
        @DisplayName("firepower crosses in halves, so five and a half stays five and a half")
        void firepowerIsNotRounded() {
            Ship ship = Ships.openShip();
            Ships.put(ship, EAST, ComponentKind.SINGLE_CANNON, Rotation.CLOCKWISE_90);

            ShipView view = Projections.of(ship);

            assertEquals(1, view.attributes().firepowerHalves(),
                    "a single cannon pointing sideways is half a point, and half a point is one half");
        }

        @Test
        @DisplayName("the attributes are what the ship can do now, not what it could with every battery spent")
        void attributesAreUnpowered() {
            Ship ship = Ships.openShip();
            Ships.put(ship, EAST, ComponentKind.DOUBLE_CANNON);
            Ships.put(ship, WEST, ComponentKind.BATTERY);
            ship.chargeBatteries();

            ShipView view = Projections.of(ship);

            assertEquals(0, view.attributes().firepowerHalves(),
                    "a double cannon with nothing powering it is worth nothing, and saying "
                            + "otherwise would show a number the player cannot act on");
        }

        @Test
        @DisplayName("what is wrong with a ship travels with it")
        void violationsAreCarried() {
            Ship ship = Ships.openShip();
            Ships.put(ship, EAST, ComponentKind.SINGLE_ENGINE, Rotation.CLOCKWISE_180);

            ShipView view = Projections.of(ship);

            assertFalse(view.validation().isLegal());
            assertTrue(view.validation().hasViolationOf(ViolationKind.ENGINE_NOT_FACING_STERN),
                    "a player cannot repair a ship without being told what is wrong with it");
        }

        @Test
        @DisplayName("reserved tiles are public, because they count against their owner")
        void reservedTilesAreShown() {
            Ship ship = Ships.openShip();

            ShipView view = Projections.of(ship,
                    List.of(Tiles.of(ComponentKind.SHIELD), Tiles.of(ComponentKind.BATTERY, 2)));

            assertEquals(2, view.reserved().size());
            assertEquals(Set.of(ComponentKind.SHIELD, ComponentKind.BATTERY),
                    view.reserved().stream().map(tile -> tile.kind())
                            .collect(java.util.stream.Collectors.toSet()));
        }

        @Test
        @DisplayName("components thrown away are counted, because they cost points")
        void lostComponentsAreCounted() {
            Ship ship = Ships.openShip();
            Ships.put(ship, EAST, ComponentKind.SHIELD);
            ship.discard(EAST);

            assertEquals(1, Projections.of(ship).lostComponents());
        }
    }

    @Nested
    @DisplayName("the shipyard")
    class Shipyard {

        private final BuildingFixtures.Site site = BuildingFixtures.site(2);
        private final BuildingTimer timer =
                new BuildingTimer(3, Duration.ofSeconds(90), InstantSource.system());
        private final StartSpaces starts = new StartSpaces(
                FlightFixtures.levelSpec(GameLevel.LEVEL_II).flightBoard(), 4,
                StartSpacePolicy.CHOSEN_BY_PLAYER);

        @Test
        @DisplayName("the heap and the discard pile are public, and counted honestly")
        void thePoolIsPublic() {
            site.builder().drawFaceDown();
            site.builder().returnToPool();

            BuildingView view = shipyard(List.of());

            assertEquals(7, view.faceDownRemaining(), "one of the eight has been turned over");
            assertEquals(1, view.faceUpPile().size());
        }

        @Test
        @DisplayName("a player sees the tile in their own hand")
        void yourOwnHand() {
            site.builder().drawFaceDown();

            assertTrue(shipyard(List.of()).handIfAny().isPresent());
        }

        @Test
        @DisplayName("and there is no hand to see when their hands are empty")
        void nobodyElsesHand() {
            // The projection is built per recipient precisely so that this is what a player
            // with nothing in hand is shown, whoever else is holding something.
            assertTrue(shipyard(List.of()).handIfAny().isEmpty());
        }

        @Test
        @DisplayName("the cards a player peeked at are theirs alone")
        void peekedCardsArePrivate() {
            AdventureCardIdentity card = new AdventureCardIdentity(
                    "peeked", AdventureCardType.OPEN_SPACE, CardLevel.LEVEL_I, false);

            assertEquals(List.of(card), shipyard(List.of(card)).scouted());
            assertEquals(List.of(), shipyard(List.of()).scouted(),
                    "somebody who did not pick up a pile sees nothing of it");
        }

        @Test
        @DisplayName("a timer that has not started is not counting down")
        void theHourglass() {
            BuildingView view = shipyard(List.of());

            assertEquals(3, view.hourglassSpaces());
            assertEquals(0, view.secondsRemaining());
        }

        @Test
        @DisplayName("the free start spaces are the ones nobody has taken")
        void theStartingLine() {
            starts.claim(PlayerColor.RED, java.util.OptionalInt.of(starts.free().get(0)));

            assertEquals(3, shipyard(List.of()).freeStartSpaces().size());
        }

        private BuildingView shipyard(List<AdventureCardIdentity> peeked) {
            return Projections.of(site.builder(), site.pool(), timer, starts,
                    Set.of(PlayerColor.BLUE), peeked);
        }
    }

    @Nested
    @DisplayName("the route")
    class TheRoute {

        @Test
        @DisplayName("positions are absolute, so a lapped ship is not mistaken for a slow one")
        void absolutePositions() {
            Flight flight = FlightFixtures.levelTwoFlight(java.util.Map.of(
                    PlayerColor.RED, Ships.openShip(), PlayerColor.BLUE, Ships.openShip()));
            flight.route().advance(PlayerColor.RED, 30);

            FlightView view = Projections.of(flight, null, 7);

            assertTrue(view.positions().get(PlayerColor.RED) > view.routeLength(),
                    "a whole lap ahead is a bigger number, not the same one again");
            assertEquals(PlayerColor.RED, view.order().get(0), "and the leader is the leader");
        }

        @Test
        @DisplayName("between cards there is no card")
        void betweenCards() {
            Flight flight = FlightFixtures.levelTwoFlight(java.util.Map.of(
                    PlayerColor.RED, Ships.openShip(), PlayerColor.BLUE, Ships.openShip()));

            assertTrue(Projections.of(flight, null, 7).cardIfAny().isEmpty());
            assertEquals(7, Projections.of(flight, null, 7).cardsLeft());
        }
    }
}
