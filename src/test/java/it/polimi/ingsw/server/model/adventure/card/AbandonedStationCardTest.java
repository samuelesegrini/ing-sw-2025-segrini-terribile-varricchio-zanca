package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import it.polimi.ingsw.server.model.adventure.AdventureCard;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.AdventureCardType;
import it.polimi.ingsw.server.model.adventure.CardLevel;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerChoice;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerPrompt;
import it.polimi.ingsw.server.model.component.ComponentKind;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.flight.FlightFixtures;
import it.polimi.ingsw.server.model.goods.GoodColor;
import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.Position;
import it.polimi.ingsw.server.model.ship.Ship;
import it.polimi.ingsw.server.model.ship.Ships;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks Abandoned Station against manual p.12.
 *
 * <p>The gentler of the two abandoned sites: it costs flight days and nothing else. No
 * crew is lost, which is the whole difference from an abandoned ship, and a model that
 * copied one from the other would quietly charge players twice.
 *
 * <p>What it asks for instead is a crew large enough to search the place. A ship that is
 * short is not <em>refused</em> — it is never offered the station at all, and the offer
 * passes straight down the route.
 *
 * <p>Components involved: {@link AbandonedStationCard}, {@link Flight}, {@link Ship}.
 */
class AbandonedStationCardTest {

    private static final Position HOLD = new Position(1, 2);
    private static final Position SECOND_HOLD = new Position(2, 1);

    private static AbandonedStationCard station(int minimumCrew, int flightDays,
                                                Map<GoodColor, Integer> goods) {
        return new AbandonedStationCard(
                new AdventureCardIdentity("abandoned-station_lvl1_01", AdventureCardType.ABANDONED_STATION,
                        CardLevel.LEVEL_I, true),
                minimumCrew, goods, flightDays);
    }

    /** A ship with the given crew and one special hold. */
    private static Ship shipWithCrew(int cabins) {
        Ship ship = Ships.openShip();
        Ships.put(ship, HOLD, ComponentKind.SPECIAL_CARGO_HOLD);
        if (cabins > 1) {
            Ships.put(ship, SECOND_HOLD, ComponentKind.CABIN);
        }
        ship.fillRemainingCabinsWithHumans();
        return ship;
    }

    private static Flight flightOf(Ship leader, Ship trailer) {
        Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
        ships.put(PlayerColor.RED, leader);
        ships.put(PlayerColor.BLUE, trailer);
        return FlightFixtures.levelTwoFlight(ships);
    }

    private static PlayerPrompt promptOf(AdventureResolution resolution) {
        return resolution.pending().orElseThrow();
    }

    @Nested
    @DisplayName("who gets offered it")
    class WhoGetsOffered {

        @Test
        @DisplayName("the leader is offered it first, with the cost stated up front")
        void theLeaderIsOfferedItFirst() {
            Flight flight = flightOf(shipWithCrew(2), shipWithCrew(2));

            PlayerPrompt prompt = promptOf(station(2, 3, Map.of(GoodColor.GREEN, 2)).resolve(flight));

            assertTrue(prompt instanceof PlayerPrompt.TakeOrLeave);
            assertEquals(PlayerColor.RED, prompt.player());
            assertEquals(3, ((PlayerPrompt.TakeOrLeave) prompt).flightDays());
        }

        @Test
        @DisplayName("a ship without the crew to search it is passed over, not refused")
        void aShipWithTooLittleCrewIsPassedOver() {
            Flight flight = flightOf(shipWithCrew(1), shipWithCrew(2));

            PlayerPrompt prompt = promptOf(station(4, 1, Map.of(GoodColor.GREEN, 1)).resolve(flight));

            assertEquals(PlayerColor.BLUE, prompt.player(), "red has two crew and the station wants four");
        }

        @Test
        @DisplayName("aliens count toward the crew the station wants")
        void aliensCountTowardTheMinimum() {
            Ship ship = Ships.openShip();
            Ships.put(ship, HOLD, ComponentKind.SPECIAL_CARGO_HOLD);
            Ships.put(ship, SECOND_HOLD, ComponentKind.CABIN);
            Ships.put(ship, new Position(1, 1), ComponentKind.BROWN_LIFE_SUPPORT);
            ship.boardAlienIn(SECOND_HOLD, it.polimi.ingsw.server.model.crew.AlienColor.BROWN);
            ship.fillRemainingCabinsWithHumans();

            assertEquals(3, ship.crewCount(), "two humans and one alien");

            Flight flight = flightOf(ship, shipWithCrew(1));

            assertEquals(PlayerColor.RED, promptOf(station(3, 1, Map.of(GoodColor.GREEN, 1))
                    .resolve(flight)).player());
        }

        @Test
        @DisplayName("with nobody big enough to dock, the card passes without anything happening")
        void nobodyBigEnoughEndsTheCard() {
            Flight flight = flightOf(shipWithCrew(1), shipWithCrew(1));
            int before = flight.route().positionOf(PlayerColor.RED);

            AdventureResolution resolution = station(9, 2, Map.of(GoodColor.GREEN, 1)).resolve(flight);

            assertTrue(resolution.isComplete());
            assertEquals(before, flight.route().positionOf(PlayerColor.RED));
        }
    }

    @Nested
    @DisplayName("docking")
    class Docking {

        @Test
        @DisplayName("the player who docks loads the salvage and pays the days")
        void dockingLoadsTheSalvageAndCostsDays() {
            Ship red = shipWithCrew(2);
            Flight flight = flightOf(red, shipWithCrew(2));
            int before = flight.route().positionOf(PlayerColor.RED);

            AdventureResolution resolution =
                    station(2, 3, Map.of(GoodColor.GREEN, 1, GoodColor.RED, 1)).resolve(flight);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));

            PlayerPrompt stow = promptOf(resolution);
            assertTrue(stow instanceof PlayerPrompt.ArrangeCargo);
            assertEquals(PlayerColor.RED, stow.player());

            red.load(HOLD, GoodColor.RED);
            red.load(HOLD, GoodColor.GREEN);
            resolution.submit(new PlayerChoice.Done(PlayerColor.RED));

            assertEquals(List.of(GoodColor.RED, GoodColor.GREEN), red.manifest());
            assertEquals(6, before);
            assertEquals(2, flight.route().positionOf(PlayerColor.RED),
                    "three empty spaces back from six, jumping blue's square on three");
            assertTrue(resolution.isComplete());
        }

        @Test
        @DisplayName("no crew is lost, which is the whole difference from an abandoned ship")
        void dockingCostsNoCrew() {
            Ship red = shipWithCrew(2);
            Flight flight = flightOf(red, shipWithCrew(2));
            int crewBefore = red.crewCount();

            AdventureResolution resolution = station(2, 1, Map.of(GoodColor.GREEN, 1)).resolve(flight);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));
            resolution.submit(new PlayerChoice.Done(PlayerColor.RED));

            assertEquals(crewBefore, red.crewCount());
        }

        @Test
        @DisplayName("only what the station holds may be loaded, however much room a ship has")
        void onlyTheOfferMayBeLoaded() {
            Ship red = shipWithCrew(2);
            Flight flight = flightOf(red, shipWithCrew(2));

            AdventureResolution resolution = station(2, 1, Map.of(GoodColor.GREEN, 1)).resolve(flight);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));

            red.load(HOLD, GoodColor.GREEN);

            assertThrows(IllegalArgumentException.class, () -> red.load(HOLD, GoodColor.GREEN));
            assertThrows(IllegalArgumentException.class, () -> red.load(HOLD, GoodColor.RED));
        }

        @Test
        @DisplayName("a player who found nowhere to put it still pays for having gone to look")
        void goingToLookCostsDaysEvenIfNothingFits() {
            Ship red = Ships.openShip();
            Ships.put(red, SECOND_HOLD, ComponentKind.CABIN);
            red.fillRemainingCabinsWithHumans();
            Flight flight = flightOf(red, shipWithCrew(2));
            int before = flight.route().positionOf(PlayerColor.RED);

            AdventureResolution resolution = station(2, 2, Map.of(GoodColor.GREEN, 1)).resolve(flight);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));
            resolution.submit(new PlayerChoice.Done(PlayerColor.RED));

            assertEquals(0, red.cargoCount(), "the ship has no holds at all");
            assertEquals(before - 2, flight.route().positionOf(PlayerColor.RED));
        }

        @Test
        @DisplayName("the holds are sealed again once the player is done")
        void theHoldsSealAgainAfterwards() {
            Ship red = shipWithCrew(2);
            Flight flight = flightOf(red, shipWithCrew(2));

            AdventureResolution resolution = station(2, 1, Map.of(GoodColor.GREEN, 1)).resolve(flight);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));
            resolution.submit(new PlayerChoice.Done(PlayerColor.RED));

            assertFalse(red.cargoOperationsOpen());
        }
    }

    @Nested
    @DisplayName("only one player gets it")
    class OnlyOnePlayer {

        @Test
        @DisplayName("declining passes the offer down the route")
        void decliningPassesTheOfferOn() {
            Flight flight = flightOf(shipWithCrew(2), shipWithCrew(2));
            AdventureResolution resolution = station(2, 1, Map.of(GoodColor.GREEN, 1)).resolve(flight);

            resolution.submit(new PlayerChoice.Leave(PlayerColor.RED));

            assertEquals(PlayerColor.BLUE, promptOf(resolution).player());
        }

        @Test
        @DisplayName("once somebody docks, everybody behind them is cut out")
        void dockingCutsOutEverybodyBehind() {
            Ship blue = shipWithCrew(2);
            Flight flight = flightOf(shipWithCrew(2), blue);
            int blueBefore = flight.route().positionOf(PlayerColor.BLUE);

            AdventureResolution resolution = station(2, 1, Map.of(GoodColor.GREEN, 1)).resolve(flight);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));
            resolution.submit(new PlayerChoice.Done(PlayerColor.RED));

            assertTrue(resolution.isComplete());
            assertEquals(blueBefore, flight.route().positionOf(PlayerColor.BLUE));
            assertEquals(0, blue.cargoCount());
        }

        @Test
        @DisplayName("everybody declining ends the card with nothing having happened")
        void everybodyDecliningEndsTheCard() {
            Flight flight = flightOf(shipWithCrew(2), shipWithCrew(2));
            AdventureResolution resolution = station(2, 1, Map.of(GoodColor.GREEN, 1)).resolve(flight);

            resolution.submit(new PlayerChoice.Leave(PlayerColor.RED));
            resolution.submit(new PlayerChoice.Leave(PlayerColor.BLUE));

            assertTrue(resolution.isComplete());
        }
    }

    @Nested
    @DisplayName("the printed values")
    class PrintedValues {

        @Test
        @DisplayName("a station holding nothing is not worth docking at, and is refused")
        void anEmptyStationIsRefused() {
            assertThrows(IllegalArgumentException.class,
                    () -> station(2, 1, Map.of(GoodColor.GREEN, 0)));
        }

        @Test
        @DisplayName("the shipped station cards carry the values printed on them")
        void theShippedCardsMatchTheirArtwork() {
            GameData data = GameDataLoader.loadBundled();
            AdventureCard card = data.playableCard("abandoned-station_lvl1_01").orElseThrow();

            assertTrue(card instanceof AbandonedStationCard);
            AbandonedStationCard station = (AbandonedStationCard) card;
            assertEquals(5, station.minimumCrew());
            assertEquals(1, station.flightDays());
            assertEquals(Map.of(GoodColor.YELLOW, 1, GoodColor.GREEN, 1), station.goods());
        }

        @Test
        @DisplayName("every abandoned station in the data has its rules built")
        void everyStationInTheDataIsPlayable() {
            GameData data = GameDataLoader.loadBundled();

            long stations = data.cards().stream()
                    .filter(card -> card.type() == AdventureCardType.ABANDONED_STATION)
                    .peek(card -> assertTrue(data.playableCard(card.id()).isPresent(), card.id()))
                    .count();

            assertEquals(4, stations);
        }
    }
}
