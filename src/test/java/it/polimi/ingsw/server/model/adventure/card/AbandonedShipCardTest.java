package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.AdventureCardType;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.flight.FlightFixtures;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks Abandoned Ship against manual p.12.
 *
 * <p>The harsher of the two abandoned sites: it pays cash and the price is crew. A ship
 * without enough people to give away is never offered it, and the player who takes it
 * chooses which cabins the losses come out of.
 *
 * <p>That choice is real. An alien is one crew member rather than two, so giving one up
 * costs less headcount and more capability — and a ship handing over its last human is
 * out of the race as soon as the card is done.
 *
 * <p>Components involved: {@link AbandonedShipCard}, {@link Flight}, {@link Ship}.
 */
class AbandonedShipCardTest {

    private static final Position SECOND_CABIN = new Position(1, 2);
    private static final Position THIRD_CABIN = new Position(2, 1);

    private static AbandonedShipCard wreck(int crewCost, int credits, int flightDays) {
        return new AbandonedShipCard(
                new AdventureCardIdentity("abandoned-ship_lvl1_01", AdventureCardType.ABANDONED_SHIP,
                        CardLevel.LEVEL_I, false),
                crewCost, credits, flightDays);
    }

    /** A ship with the given number of cabins, all crewed with humans. */
    private static Ship shipWithCabins(int cabins) {
        Ship ship = Ships.openShip();
        if (cabins > 1) {
            Ships.put(ship, SECOND_CABIN, ComponentKind.CABIN);
        }
        if (cabins > 2) {
            Ships.put(ship, THIRD_CABIN, ComponentKind.CABIN);
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

    @Nested
    @DisplayName("who gets offered it")
    class WhoGetsOffered {

        @Test
        @DisplayName("a ship with nobody to spare is passed over, not refused")
        void aShipWithoutSpareCrewIsPassedOver() {
            Flight flight = flightOf(shipWithCabins(1), shipWithCabins(3));

            PlayerPrompt prompt = wreck(4, 5, 1).resolve(flight).pending().orElseThrow();

            assertEquals(PlayerColor.BLUE, prompt.player(), "red has two crew and the deal costs four");
        }

        @Test
        @DisplayName("with nobody able to pay, the card passes without anything happening")
        void nobodyAbleToPayEndsTheCard() {
            Flight flight = flightOf(shipWithCabins(1), shipWithCabins(1));

            assertTrue(wreck(5, 5, 1).resolve(flight).isComplete());
        }
    }

    @Nested
    @DisplayName("taking the deal")
    class TakingTheDeal {

        @Test
        @DisplayName("the crew leave, the credits arrive and the days are paid")
        void theDealCostsCrewAndDaysAndPaysCredits() {
            Ship red = shipWithCabins(2);
            Flight flight = flightOf(red, shipWithCabins(2));
            int before = flight.route().positionOf(PlayerColor.RED);

            AdventureResolution resolution = wreck(2, 5, 1).resolve(flight);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));
            resolution.submit(new PlayerChoice.CrewGiven(PlayerColor.RED,
                    List.of(SECOND_CABIN, SECOND_CABIN)));

            assertEquals(2, red.crewCount(), "the second cabin was emptied");
            assertEquals(5, flight.creditsEarned(PlayerColor.RED));
            assertEquals(before - 1, flight.route().positionOf(PlayerColor.RED));
            assertTrue(resolution.isComplete());
        }

        @Test
        @DisplayName("the player is asked which cabins the losses come out of")
        void theCrewChoiceIsThePlayers() {
            Flight flight = flightOf(shipWithCabins(2), shipWithCabins(2));

            AdventureResolution resolution = wreck(2, 5, 1).resolve(flight);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));

            PlayerPrompt prompt = resolution.pending().orElseThrow();
            assertTrue(prompt instanceof PlayerPrompt.GiveUpCrew);
            PlayerPrompt.GiveUpCrew call = (PlayerPrompt.GiveUpCrew) prompt;
            assertEquals(2, call.count());
            assertEquals(java.util.Set.of(Ships.CABIN, SECOND_CABIN), call.cabins());
        }

        @Test
        @DisplayName("giving up an alien costs one crew member rather than two humans")
        void anAlienIsOneCrewMember() {
            Ship red = Ships.openShip();
            Ships.put(red, SECOND_CABIN, ComponentKind.CABIN);
            Ships.put(red, new Position(0, 2), ComponentKind.PURPLE_LIFE_SUPPORT);
            red.boardAlienIn(SECOND_CABIN, AlienColor.PURPLE);
            red.fillRemainingCabinsWithHumans();

            Flight flight = flightOf(red, shipWithCabins(2));

            AdventureResolution resolution = wreck(1, 4, 1).resolve(flight);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));
            resolution.submit(new PlayerChoice.CrewGiven(PlayerColor.RED, List.of(SECOND_CABIN)));

            assertEquals(2, red.crewCount(), "the alien left and both humans stayed");
            assertTrue(red.aliens().isEmpty());
        }

        @Test
        @DisplayName("a ship handing over its last human is out as soon as the card is done")
        void handingOverTheLastHumanEndsTheRace() {
            Ship red = shipWithCabins(1);
            Flight flight = flightOf(red, shipWithCabins(2));

            AdventureResolution resolution = wreck(2, 6, 1).resolve(flight);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));
            resolution.submit(new PlayerChoice.CrewGiven(PlayerColor.RED,
                    List.of(Ships.CABIN, Ships.CABIN)));

            assertEquals(0, red.humanCount());
            assertEquals(java.util.Set.of(PlayerColor.RED), flight.playersForcedOut());
        }

        @Test
        @DisplayName("naming the wrong number of cabins is refused rather than half-applied")
        void namingTheWrongNumberOfCabinsIsRefused() {
            Ship red = shipWithCabins(2);
            Flight flight = flightOf(red, shipWithCabins(2));

            AdventureResolution resolution = wreck(2, 5, 1).resolve(flight);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));

            assertThrows(IllegalArgumentException.class,
                    () -> resolution.submit(new PlayerChoice.CrewGiven(PlayerColor.RED,
                            List.of(SECOND_CABIN))));
            assertEquals(4, red.crewCount(), "nobody left");
        }

        @Test
        @DisplayName("naming a cabin that is already empty is refused")
        void namingAnEmptyCabinIsRefused() {
            Ship red = shipWithCabins(2);
            Flight flight = flightOf(red, shipWithCabins(2));

            AdventureResolution resolution = wreck(3, 5, 1).resolve(flight);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));

            assertThrows(IllegalStateException.class,
                    () -> resolution.submit(new PlayerChoice.CrewGiven(PlayerColor.RED,
                            List.of(SECOND_CABIN, SECOND_CABIN, SECOND_CABIN))));
        }
    }

    @Nested
    @DisplayName("only one player gets it")
    class OnlyOnePlayer {

        @Test
        @DisplayName("declining passes the wreck down the route")
        void decliningPassesItOn() {
            Flight flight = flightOf(shipWithCabins(2), shipWithCabins(2));
            AdventureResolution resolution = wreck(2, 5, 1).resolve(flight);

            resolution.submit(new PlayerChoice.Leave(PlayerColor.RED));

            assertEquals(PlayerColor.BLUE, resolution.pending().orElseThrow().player());
        }

        @Test
        @DisplayName("once somebody takes it, everybody behind them is cut out")
        void takingItCutsOutEverybodyBehind() {
            Ship blue = shipWithCabins(2);
            Flight flight = flightOf(shipWithCabins(2), blue);

            AdventureResolution resolution = wreck(2, 5, 1).resolve(flight);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));
            resolution.submit(new PlayerChoice.CrewGiven(PlayerColor.RED,
                    List.of(SECOND_CABIN, SECOND_CABIN)));

            assertTrue(resolution.isComplete());
            assertEquals(4, blue.crewCount());
            assertEquals(0, flight.creditsEarned(PlayerColor.BLUE));
        }
    }

    @Nested
    @DisplayName("the printed values")
    class PrintedValues {

        @Test
        @DisplayName("a wreck that costs no crew is not a deal, and is refused")
        void aFreeWreckIsRefused() {
            assertThrows(IllegalArgumentException.class, () -> wreck(0, 5, 1));
        }

        @Test
        @DisplayName("the shipped abandoned ships carry the values printed on them")
        void theShippedCardsMatchTheirArtwork() {
            GameData data = GameDataLoader.loadBundled();
            AbandonedShipCard card =
                    (AbandonedShipCard) data.playableCard("abandoned-ship_lvl1_01").orElseThrow();

            assertEquals(2, card.crewCost());
            assertEquals(3, card.credits());
            assertEquals(1, card.flightDays());
        }
    }
}
