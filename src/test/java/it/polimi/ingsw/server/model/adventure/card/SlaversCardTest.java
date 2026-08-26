package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.AdventureCardType;
import it.polimi.ingsw.server.model.adventure.CardLevel;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerChoice;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerPrompt;
import it.polimi.ingsw.server.model.adventure.resolution.ShipAttribute;
import it.polimi.ingsw.server.model.component.ComponentKind;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.flight.FlightFixtures;
import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.BatteryPlan;
import it.polimi.ingsw.server.model.ship.Position;
import it.polimi.ingsw.server.model.ship.Rotation;
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
 * Checks the Slavers, and with them the fight every enemy card puts up (manual p.12,
 * p.19).
 *
 * <p>Three outcomes, and the middle one is the one people forget: a draw leaves the player
 * untouched and the enemy <em>undefeated</em>, so it carries on down the route. Only
 * beating it stops it, and then nobody behind is attacked at all.
 *
 * <p>Firepower is compared in halves. A ship on 5½ against an enemy of 5 wins; a ship on 5
 * draws. Rounding either way here would decide fights the manual says go the other way,
 * which is why the boundary gets three tests rather than one.
 *
 * <p>Components involved: {@link SlaversCard}, {@link EnemyResolution}, {@link Ship}.
 */
class SlaversCardTest {

    private static final Position SECOND_CABIN = new Position(3, 2);

    /** Cells a cannon can be welded into, in an order where each one touches the ship. */
    private static final List<Position> CANNON_BAYS = List.of(
            new Position(1, 2), new Position(0, 2), new Position(2, 1),
            new Position(2, 0), new Position(2, 3), new Position(2, 4));

    private static SlaversCard slavers(int firepower, int credits, int crewPenalty, int flightDays) {
        return new SlaversCard(
                new AdventureCardIdentity("slavers_lvl1", AdventureCardType.SLAVERS,
                        CardLevel.LEVEL_I, false),
                firepower, credits, crewPenalty, flightDays);
    }

    /**
     * A ship with exactly the given firepower in halves.
     *
     * <p>A forward single cannon is worth two halves and a side-facing one is worth one, so
     * an odd total needs exactly one cannon turned sideways. That is the only way to build
     * a half, and half-points are the whole reason these comparisons are worth testing.
     */
    private static Ship shipWithFirepowerHalves(int halves) {
        Ship ship = Ships.openShip();
        Ships.put(ship, SECOND_CABIN, ComponentKind.CABIN);

        int forward = halves / 2;
        int bay = 0;
        for (int i = 0; i < forward; i++) {
            Ships.put(ship, CANNON_BAYS.get(bay++), ComponentKind.SINGLE_CANNON, Rotation.NONE);
        }
        if (halves % 2 == 1) {
            Ships.put(ship, CANNON_BAYS.get(bay), ComponentKind.SINGLE_CANNON, Rotation.CLOCKWISE_90);
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

    private static void declare(AdventureResolution resolution, PlayerColor player) {
        resolution.submit(new PlayerChoice.Declaration(player, BatteryPlan.none()));
    }

    @Nested
    @DisplayName("the fight")
    class TheFight {

        @Test
        @DisplayName("the leader is asked first, and asked for firepower")
        void theLeaderDeclaresFirst() {
            Flight flight = flightOf(shipWithFirepowerHalves(2), shipWithFirepowerHalves(2));

            PlayerPrompt prompt = slavers(1, 5, 2, 1).resolve(flight).pending().orElseThrow();

            assertTrue(prompt instanceof PlayerPrompt.DeclarePower);
            assertEquals(PlayerColor.RED, prompt.player());
            assertEquals(ShipAttribute.FIREPOWER, ((PlayerPrompt.DeclarePower) prompt).attribute());
        }

        @Test
        @DisplayName("half a point over is a win: five and a half beats five")
        void halfAPointOverIsAWin() {
            Ship red = shipWithFirepowerHalves(11);
            Flight flight = flightOf(red, shipWithFirepowerHalves(2));

            AdventureResolution resolution = slavers(5, 5, 2, 1).resolve(flight);
            declare(resolution, PlayerColor.RED);

            PlayerPrompt offer = resolution.pending().orElseThrow();
            assertTrue(offer instanceof PlayerPrompt.TakeOrLeave, "red won and is being offered the reward");
            assertEquals(PlayerColor.RED, offer.player());
        }

        @Test
        @DisplayName("a draw leaves the player alone and the enemy undefeated, so it carries on")
        void aDrawIsNotAWin() {
            Ship red = shipWithFirepowerHalves(4);
            Flight flight = flightOf(red, shipWithFirepowerHalves(2));

            AdventureResolution resolution = slavers(2, 5, 2, 1).resolve(flight);
            declare(resolution, PlayerColor.RED);

            assertEquals(4, red.crewCount(), "nothing happened to red");
            assertEquals(0, flight.creditsEarned(PlayerColor.RED));
            assertEquals(PlayerColor.BLUE, resolution.pending().orElseThrow().player(),
                    "the slavers moved on to the next ship");
        }

        @Test
        @DisplayName("half a point short is a loss: four and a half loses to five")
        void halfAPointShortIsALoss() {
            Ship red = shipWithFirepowerHalves(9);
            Flight flight = flightOf(red, shipWithFirepowerHalves(2));

            AdventureResolution resolution = slavers(5, 5, 2, 1).resolve(flight);
            declare(resolution, PlayerColor.RED);

            PlayerPrompt prompt = resolution.pending().orElseThrow();
            assertTrue(prompt instanceof PlayerPrompt.GiveUpCrew, "red lost and owes crew");
        }

        @Test
        @DisplayName("beating them stops them, so nobody behind is attacked")
        void beatingThemProtectsEverybodyBehind() {
            Ship blue = shipWithFirepowerHalves(0);
            Flight flight = flightOf(shipWithFirepowerHalves(4), blue);

            AdventureResolution resolution = slavers(1, 5, 2, 1).resolve(flight);
            declare(resolution, PlayerColor.RED);
            resolution.submit(new PlayerChoice.Leave(PlayerColor.RED));

            assertTrue(resolution.isComplete());
            assertEquals(4, blue.crewCount(), "blue was never attacked");
        }
    }

    @Nested
    @DisplayName("the reward")
    class TheReward {

        @Test
        @DisplayName("taking it pays credits and costs flight days")
        void takingItPaysAndCosts() {
            Flight flight = flightOf(shipWithFirepowerHalves(4), shipWithFirepowerHalves(2));
            int before = flight.route().positionOf(PlayerColor.RED);

            AdventureResolution resolution = slavers(1, 5, 2, 2).resolve(flight);
            declare(resolution, PlayerColor.RED);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));

            assertEquals(5, flight.creditsEarned(PlayerColor.RED));
            assertEquals(before - 2, flight.route().positionOf(PlayerColor.RED));
            assertTrue(resolution.isComplete());
        }

        @Test
        @DisplayName("declining costs nothing at all, not even the days")
        void decliningCostsNothing() {
            Flight flight = flightOf(shipWithFirepowerHalves(4), shipWithFirepowerHalves(2));
            int before = flight.route().positionOf(PlayerColor.RED);

            AdventureResolution resolution = slavers(1, 5, 2, 2).resolve(flight);
            declare(resolution, PlayerColor.RED);
            resolution.submit(new PlayerChoice.Leave(PlayerColor.RED));

            assertEquals(0, flight.creditsEarned(PlayerColor.RED));
            assertEquals(before, flight.route().positionOf(PlayerColor.RED));
        }
    }

    @Nested
    @DisplayName("the penalty")
    class ThePenalty {

        @Test
        @DisplayName("the loser chooses which cabins the losses come out of")
        void theLoserChoosesWhoGoes() {
            Ship red = shipWithFirepowerHalves(0);
            Flight flight = flightOf(red, shipWithFirepowerHalves(2));

            AdventureResolution resolution = slavers(3, 5, 2, 1).resolve(flight);
            declare(resolution, PlayerColor.RED);
            resolution.submit(new PlayerChoice.CrewGiven(PlayerColor.RED,
                    List.of(SECOND_CABIN, SECOND_CABIN)));

            assertEquals(2, red.crewCount(), "the second cabin was emptied and the first left alone");
        }

        @Test
        @DisplayName("the slavers move on to the next ship once they have been paid")
        void theyMoveOnAfterBeingPaid() {
            Flight flight = flightOf(shipWithFirepowerHalves(0), shipWithFirepowerHalves(0));

            AdventureResolution resolution = slavers(3, 5, 1, 1).resolve(flight);
            declare(resolution, PlayerColor.RED);
            resolution.submit(new PlayerChoice.CrewGiven(PlayerColor.RED, List.of(SECOND_CABIN)));

            assertEquals(PlayerColor.BLUE, resolution.pending().orElseThrow().player());
        }

        @Test
        @DisplayName("a ship with fewer crew than they want gives up everyone and no more")
        void aShipCannotGiveMoreCrewThanItHas() {
            Ship red = Ships.openShip();
            red.fillRemainingCabinsWithHumans();
            Flight flight = flightOf(red, shipWithFirepowerHalves(0));

            assertEquals(2, red.crewCount());

            AdventureResolution resolution = slavers(3, 5, 5, 1).resolve(flight);
            declare(resolution, PlayerColor.RED);
            resolution.submit(new PlayerChoice.CrewGiven(PlayerColor.RED,
                    List.of(Ships.CABIN, Ships.CABIN)));

            assertEquals(0, red.crewCount());
        }

        @Test
        @DisplayName("naming the wrong number of cabins is refused rather than half-applied")
        void namingTheWrongNumberIsRefused() {
            Ship red = shipWithFirepowerHalves(0);
            Flight flight = flightOf(red, shipWithFirepowerHalves(2));

            AdventureResolution resolution = slavers(3, 5, 2, 1).resolve(flight);
            declare(resolution, PlayerColor.RED);

            assertThrows(IllegalArgumentException.class,
                    () -> resolution.submit(new PlayerChoice.CrewGiven(PlayerColor.RED,
                            List.of(SECOND_CABIN))));
            assertEquals(4, red.crewCount(), "nobody left");
        }
    }

    @Nested
    @DisplayName("the printed values")
    class PrintedValues {

        @Test
        @DisplayName("slavers who take nobody are not slavers, and are refused")
        void slaversTakingNobodyAreRefused() {
            assertThrows(IllegalArgumentException.class, () -> slavers(5, 5, 0, 1));
        }

        @Test
        @DisplayName("the shipped slaver cards carry the values printed on them")
        void theShippedCardsMatchTheirArtwork() {
            GameData data = GameDataLoader.loadBundled();
            SlaversCard card = (SlaversCard) data.playableCard("slavers_lvl1").orElseThrow();

            assertEquals(6, card.firepower());
            assertEquals(5, card.credits());
            assertEquals(3, card.crewPenalty());
            assertEquals(1, card.flightDays());
        }
    }
}
