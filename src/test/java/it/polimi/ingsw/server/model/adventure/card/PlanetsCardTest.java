package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.AdventureCardType;
import it.polimi.ingsw.server.model.adventure.CardLevel;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerChoice;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerPrompt;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.flight.FlightFixtures;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.server.model.goods.GoodsBank;
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
 * Checks Planets against manual p.12.
 *
 * <p>The card where being in front is worth the most: the leader picks first, and a planet
 * taken is a planet gone. Landing purely to deny somebody the good one is a legitimate
 * move, which the manual says outright.
 *
 * <p>Two orderings matter and they run opposite ways. Choosing and loading go in route
 * order — which is also what settles a run on the bank, since a ship in front takes the
 * last red cube and a ship behind finds none. Then the flight days are paid in
 * <em>reverse</em> route order, once everybody has decided, which stops a ship in front
 * being pushed onto a space a ship behind is about to leave.
 *
 * <p>Components involved: {@link PlanetsCard}, {@link Flight}, {@link Ship}.
 */
class PlanetsCardTest {

    private static final Position HOLD = new Position(1, 2);

    private static PlanetsCard planets(int flightDays, List<Map<GoodColor, Integer>> worlds) {
        return new PlanetsCard(
                new AdventureCardIdentity("planets_lvl1_01", AdventureCardType.PLANETS,
                        CardLevel.LEVEL_I, false),
                worlds, flightDays);
    }

    private static PlanetsCard twoPlanets(int flightDays) {
        return planets(flightDays, List.of(
                Map.of(GoodColor.RED, 1),
                Map.of(GoodColor.BLUE, 1)));
    }

    private static Ship shipWithHold() {
        return shipWithHold(Ships.deepBank());
    }

    private static Ship shipWithHold(GoodsBank bank) {
        Ship ship = Ships.openShip(bank);
        Ships.put(ship, HOLD, ComponentKind.SPECIAL_CARGO_HOLD);
        ship.fillRemainingCabinsWithHumans();
        return ship;
    }

    private static Flight flightOf(Ship leader, Ship trailer) {
        Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
        ships.put(PlayerColor.RED, leader);
        ships.put(PlayerColor.BLUE, trailer);
        return FlightFixtures.levelTwoFlight(ships);
    }

    private static void landOn(AdventureResolution resolution, PlayerColor player, int planet) {
        resolution.submit(new PlayerChoice.PlanetChosen(player, planet));
    }

    @Nested
    @DisplayName("choosing a planet")
    class ChoosingAPlanet {

        @Test
        @DisplayName("the leader picks first, from every planet on the card")
        void theLeaderPicksFirst() {
            Flight flight = flightOf(shipWithHold(), shipWithHold());

            PlayerPrompt prompt = twoPlanets(2).resolve(flight).pending().orElseThrow();

            assertTrue(prompt instanceof PlayerPrompt.ChoosePlanet);
            PlayerPrompt.ChoosePlanet call = (PlayerPrompt.ChoosePlanet) prompt;
            assertEquals(PlayerColor.RED, call.player());
            assertEquals(Set.of(1, 2), call.planets().keySet());
            assertEquals(2, call.flightDays());
        }

        @Test
        @DisplayName("a planet taken is gone, and the next player is only offered what is left")
        void aPlanetTakenIsGone() {
            Ship red = shipWithHold();
            Flight flight = flightOf(red, shipWithHold());

            AdventureResolution resolution = twoPlanets(1).resolve(flight);
            landOn(resolution, PlayerColor.RED, 1);
            red.load(HOLD, GoodColor.RED);
            resolution.submit(new PlayerChoice.Done(PlayerColor.RED));

            PlayerPrompt.ChoosePlanet next =
                    (PlayerPrompt.ChoosePlanet) resolution.pending().orElseThrow();
            assertEquals(PlayerColor.BLUE, next.player());
            assertEquals(Set.of(2), next.planets().keySet());
        }

        @Test
        @DisplayName("landing is never compulsory, and declining costs nothing")
        void decliningCostsNothing() {
            Flight flight = flightOf(shipWithHold(), shipWithHold());
            int before = flight.route().positionOf(PlayerColor.RED);

            AdventureResolution resolution = twoPlanets(3).resolve(flight);
            resolution.submit(new PlayerChoice.Leave(PlayerColor.RED));
            resolution.submit(new PlayerChoice.Leave(PlayerColor.BLUE));

            assertTrue(resolution.isComplete());
            assertEquals(before, flight.route().positionOf(PlayerColor.RED));
        }

        @Test
        @DisplayName("landing on a planet somebody is already on is refused")
        void landingOnAnOccupiedPlanetIsRefused() {
            Ship red = shipWithHold();
            Flight flight = flightOf(red, shipWithHold());

            AdventureResolution resolution = twoPlanets(1).resolve(flight);
            landOn(resolution, PlayerColor.RED, 1);
            resolution.submit(new PlayerChoice.Done(PlayerColor.RED));

            assertThrows(IllegalArgumentException.class,
                    () -> landOn(resolution, PlayerColor.BLUE, 1));
        }

        @Test
        @DisplayName("once every planet is occupied nobody else is asked")
        void nobodyIsAskedOnceThePlanetsRunOut() {
            Ship red = shipWithHold();
            Flight flight = flightOf(red, shipWithHold());

            AdventureResolution resolution =
                    planets(1, List.of(Map.of(GoodColor.RED, 1))).resolve(flight);
            landOn(resolution, PlayerColor.RED, 1);
            red.load(HOLD, GoodColor.RED);
            resolution.submit(new PlayerChoice.Done(PlayerColor.RED));

            assertTrue(resolution.isComplete(), "blue was never offered the one taken planet");
        }
    }

    @Nested
    @DisplayName("loading what is down there")
    class Loading {

        @Test
        @DisplayName("a lander stows what their planet was carrying, and nothing else")
        void aLanderStowsTheirOwnPlanet() {
            Ship red = shipWithHold();
            Flight flight = flightOf(red, shipWithHold());

            AdventureResolution resolution = twoPlanets(1).resolve(flight);
            landOn(resolution, PlayerColor.RED, 1);

            PlayerPrompt stow = resolution.pending().orElseThrow();
            assertTrue(stow instanceof PlayerPrompt.ArrangeCargo);
            assertEquals(Map.of(GoodColor.RED, 1), ((PlayerPrompt.ArrangeCargo) stow).offered());

            red.load(HOLD, GoodColor.RED);
            assertThrows(IllegalArgumentException.class, () -> red.load(HOLD, GoodColor.BLUE));

            resolution.submit(new PlayerChoice.Done(PlayerColor.RED));
            assertEquals(List.of(GoodColor.RED), red.manifest());
        }

        @Test
        @DisplayName("a run on the bank is settled in route order: the ship in front takes the last cube")
        void aRunOnTheBankFollowsRouteOrder() {
            // One red cube left in the whole game, and both planets are offering one.
            GoodsBank bank = Ships.bankOf(Map.of(GoodColor.RED, 1, GoodColor.YELLOW, 0,
                    GoodColor.GREEN, 0, GoodColor.BLUE, 0));
            Ship red = shipWithHold(bank);
            Ship blue = shipWithHold(bank);

            Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
            ships.put(PlayerColor.RED, red);
            ships.put(PlayerColor.BLUE, blue);
            Flight flight = FlightFixtures.levelTwoFlight(ships);

            AdventureResolution resolution =
                    planets(1, List.of(Map.of(GoodColor.RED, 1), Map.of(GoodColor.RED, 1)))
                            .resolve(flight);

            landOn(resolution, PlayerColor.RED, 1);
            assertTrue(red.load(HOLD, GoodColor.RED), "the ship in front got the last cube");
            resolution.submit(new PlayerChoice.Done(PlayerColor.RED));

            landOn(resolution, PlayerColor.BLUE, 2);
            assertFalse(blue.load(HOLD, GoodColor.RED), "the ship behind found the bank empty");
            resolution.submit(new PlayerChoice.Done(PlayerColor.BLUE));

            assertEquals(1, red.cargoCount());
            assertEquals(0, blue.cargoCount(), "and still pays the flight days for going down");
            assertEquals(2, flight.route().positionOf(PlayerColor.BLUE));
        }
    }

    @Nested
    @DisplayName("paying for the trip")
    class PayingForTheTrip {

        @Test
        @DisplayName("only the players who landed pay, and they pay after everybody has decided")
        void onlyLandersPayAndOnlyAtTheEnd() {
            Ship red = shipWithHold();
            Flight flight = flightOf(red, shipWithHold());
            int redBefore = flight.route().positionOf(PlayerColor.RED);
            int blueBefore = flight.route().positionOf(PlayerColor.BLUE);

            AdventureResolution resolution = twoPlanets(2).resolve(flight);
            landOn(resolution, PlayerColor.RED, 1);
            red.load(HOLD, GoodColor.RED);

            assertEquals(redBefore, flight.route().positionOf(PlayerColor.RED),
                    "nothing is paid while blue is still deciding");

            resolution.submit(new PlayerChoice.Done(PlayerColor.RED));
            resolution.submit(new PlayerChoice.Leave(PlayerColor.BLUE));

            assertEquals(redBefore - 2, flight.route().positionOf(PlayerColor.RED));
            assertEquals(blueBefore, flight.route().positionOf(PlayerColor.BLUE),
                    "blue stayed in orbit and paid nothing");
        }

        @Test
        @DisplayName("landers pay in reverse route order, the one furthest behind moving first")
        void landersPayFurthestBehindFirst() {
            Ship red = shipWithHold();
            Ship blue = shipWithHold();
            Flight flight = flightOf(red, blue);

            AdventureResolution resolution = twoPlanets(1).resolve(flight);
            landOn(resolution, PlayerColor.RED, 1);
            red.load(HOLD, GoodColor.RED);
            resolution.submit(new PlayerChoice.Done(PlayerColor.RED));
            landOn(resolution, PlayerColor.BLUE, 2);
            blue.load(HOLD, GoodColor.BLUE);
            resolution.submit(new PlayerChoice.Done(PlayerColor.BLUE));

            assertEquals(2, flight.route().positionOf(PlayerColor.BLUE),
                    "blue started on three and moved first, into a clear space");
            assertEquals(5, flight.route().positionOf(PlayerColor.RED),
                    "red started on six and moved after");
        }
    }

    @Nested
    @DisplayName("the printed values")
    class PrintedValues {

        @Test
        @DisplayName("a planet with nothing on it is not worth the flight days, and is refused")
        void anEmptyPlanetIsRefused() {
            assertThrows(IllegalArgumentException.class,
                    () -> planets(1, List.of(Map.of(GoodColor.RED, 0))));
        }

        @Test
        @DisplayName("a card with no planets on it is refused")
        void aCardWithNoPlanetsIsRefused() {
            assertThrows(IllegalArgumentException.class, () -> planets(1, List.of()));
        }

        @Test
        @DisplayName("the shipped planet cards carry the worlds printed on them, in order")
        void theShippedCardsMatchTheirArtwork() {
            GameData data = GameDataLoader.loadBundled();
            PlanetsCard card = (PlanetsCard) data.playableCard("planets_lvl1_01").orElseThrow();

            assertEquals(4, card.planets().size());
            assertEquals(3, card.flightDays());
            assertEquals(Map.of(GoodColor.RED, 1, GoodColor.BLUE, 3, GoodColor.GREEN, 1),
                    card.planet(1));
            assertEquals(Map.of(GoodColor.RED, 1, GoodColor.GREEN, 1), card.planet(4));
            assertThrows(IllegalArgumentException.class, () -> card.planet(5));
        }
    }
}
