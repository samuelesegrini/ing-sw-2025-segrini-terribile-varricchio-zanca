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
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.server.model.ship.BatteryPlan;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.Rotation;
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
 * Checks the Smugglers against manual p.12.
 *
 * <p>The enemy that trades in cargo rather than cash, which is what makes winning against
 * them awkward. Credits arrive whatever shape a ship is in; goods need a hold with room,
 * and a red cube needs a reinforced one. A player can beat the smugglers and still come
 * away with less than the card promised — and still pay the flight days for the privilege.
 *
 * <p>Losing takes the most valuable cubes first, then batteries once the holds are empty,
 * then nothing: a ship with neither cannot be robbed further.
 *
 * <p>Components involved: {@link SmugglersCard}, {@link EnemyResolution}, {@link Ship}.
 */
class SmugglersCardTest {

    private static final Position HOLD = new Position(1, 2);
    private static final Position SECOND_HOLD = new Position(0, 2);
    private static final Position BATTERY = new Position(3, 2);

    /** Cells a cannon can be welded into, in an order where each one touches the ship. */
    private static final List<Position> CANNON_BAYS = List.of(
            new Position(2, 1), new Position(2, 0), new Position(2, 3), new Position(2, 4));

    private static SmugglersCard smugglers(int firepower, Map<GoodColor, Integer> goods,
                                           int goodsPenalty, int flightDays) {
        return new SmugglersCard(
                new AdventureCardIdentity("smugglers_lvl1", AdventureCardType.SMUGGLERS,
                        CardLevel.LEVEL_I, true),
                firepower, goods, goodsPenalty, flightDays);
    }

    /** A ship with the given firepower in halves, one special hold and a charged battery. */
    private static Ship shipWithFirepowerHalves(int halves) {
        Ship ship = Ships.openShip();
        Ships.put(ship, HOLD, ComponentKind.SPECIAL_CARGO_HOLD);
        Ships.put(ship, SECOND_HOLD, ComponentKind.SPECIAL_CARGO_HOLD);
        Ships.put(ship, BATTERY, ComponentKind.BATTERY);
        int forward = halves / 2;
        for (int i = 0; i < forward; i++) {
            Ships.put(ship, CANNON_BAYS.get(i), ComponentKind.SINGLE_CANNON, Rotation.NONE);
        }
        if (halves % 2 == 1) {
            Ships.put(ship, CANNON_BAYS.get(forward), ComponentKind.SINGLE_CANNON, Rotation.CLOCKWISE_90);
        }
        ship.chargeBatteries();
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

    /** Puts the given cubes aboard, as an earlier card would have done. */
    private static void loadCargo(Ship ship, GoodColor... cubes) {
        Map<GoodColor, Integer> offer = new LinkedHashMap<>();
        for (GoodColor cube : cubes) {
            offer.merge(cube, 1, Integer::sum);
        }
        ship.beginCargoOperations(offer);
        for (GoodColor cube : cubes) {
            ship.load(ship.holdsAccepting(cube).iterator().next(), cube);
        }
        ship.endCargoOperations();
    }

    @Nested
    @DisplayName("beating them")
    class BeatingThem {

        @Test
        @DisplayName("the winner is offered the contraband and has to stow it")
        void theWinnerHasToStowIt() {
            Ship red = shipWithFirepowerHalves(4);
            Flight flight = flightOf(red, shipWithFirepowerHalves(2));
            int before = flight.route().positionOf(PlayerColor.RED);

            AdventureResolution resolution =
                    smugglers(1, Map.of(GoodColor.GREEN, 1, GoodColor.BLUE, 1), 2, 1).resolve(flight);
            declare(resolution, PlayerColor.RED);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));

            PlayerPrompt stow = resolution.pending().orElseThrow();
            assertTrue(stow instanceof PlayerPrompt.ArrangeCargo);

            red.load(HOLD, GoodColor.GREEN);
            red.load(HOLD, GoodColor.BLUE);
            resolution.submit(new PlayerChoice.Done(PlayerColor.RED));

            assertEquals(List.of(GoodColor.GREEN, GoodColor.BLUE), red.manifest());
            assertEquals(before - 1, flight.route().positionOf(PlayerColor.RED));
            assertTrue(resolution.isComplete());
            assertFalse(red.cargoOperationsOpen());
        }

        @Test
        @DisplayName("a winner with nowhere to put it still pays the days, which credits never cost")
        void aWinnerWithNoRoomStillPaysTheDays() {
            Ship red = Ships.openShip();
            Ships.put(red, BATTERY, ComponentKind.BATTERY);
            Ships.put(red, CANNON_BAYS.getFirst(), ComponentKind.SINGLE_CANNON, Rotation.NONE);
            Ships.put(red, CANNON_BAYS.get(1), ComponentKind.SINGLE_CANNON, Rotation.NONE);
            red.fillRemainingCabinsWithHumans();

            Flight flight = flightOf(red, shipWithFirepowerHalves(2));
            int before = flight.route().positionOf(PlayerColor.RED);

            AdventureResolution resolution =
                    smugglers(1, Map.of(GoodColor.GREEN, 2), 2, 2).resolve(flight);
            declare(resolution, PlayerColor.RED);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));
            resolution.submit(new PlayerChoice.Done(PlayerColor.RED));

            assertEquals(0, red.cargoCount(), "the ship has no holds at all");
            assertEquals(before - 2, flight.route().positionOf(PlayerColor.RED));
        }

        @Test
        @DisplayName("only what the smugglers were carrying may be taken")
        void onlyTheHaulMayBeTaken() {
            Ship red = shipWithFirepowerHalves(4);
            Flight flight = flightOf(red, shipWithFirepowerHalves(2));

            AdventureResolution resolution =
                    smugglers(1, Map.of(GoodColor.GREEN, 1), 2, 1).resolve(flight);
            declare(resolution, PlayerColor.RED);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));

            red.load(HOLD, GoodColor.GREEN);

            assertThrows(IllegalArgumentException.class, () -> red.load(HOLD, GoodColor.RED));
            assertThrows(IllegalArgumentException.class, () -> red.load(HOLD, GoodColor.GREEN));
        }

        @Test
        @DisplayName("declining leaves the contraband and costs nothing")
        void decliningCostsNothing() {
            Ship red = shipWithFirepowerHalves(4);
            Flight flight = flightOf(red, shipWithFirepowerHalves(2));
            int before = flight.route().positionOf(PlayerColor.RED);

            AdventureResolution resolution =
                    smugglers(1, Map.of(GoodColor.GREEN, 1), 2, 2).resolve(flight);
            declare(resolution, PlayerColor.RED);
            resolution.submit(new PlayerChoice.Leave(PlayerColor.RED));

            assertEquals(0, red.cargoCount());
            assertEquals(before, flight.route().positionOf(PlayerColor.RED));
            assertTrue(resolution.isComplete());
        }
    }

    @Nested
    @DisplayName("losing to them")
    class LosingToThem {

        @Test
        @DisplayName("they take the most valuable cubes first")
        void theyTakeTheMostValuableFirst() {
            Ship red = shipWithFirepowerHalves(0);
            loadCargo(red, GoodColor.BLUE, GoodColor.RED, GoodColor.GREEN);
            Flight flight = flightOf(red, shipWithFirepowerHalves(2));

            AdventureResolution resolution =
                    smugglers(3, Map.of(GoodColor.GREEN, 1), 2, 1).resolve(flight);
            declare(resolution, PlayerColor.RED);

            assertEquals(List.of(GoodColor.BLUE), red.manifest(), "the red and green cubes went");
        }

        @Test
        @DisplayName("batteries go once the holds are empty")
        void batteriesGoOnceTheHoldsAreEmpty() {
            Ship red = shipWithFirepowerHalves(0);
            loadCargo(red, GoodColor.BLUE);
            Flight flight = flightOf(red, shipWithFirepowerHalves(2));

            assertEquals(2, red.availableCharges());

            AdventureResolution resolution =
                    smugglers(3, Map.of(GoodColor.GREEN, 1), 3, 1).resolve(flight);
            declare(resolution, PlayerColor.RED);

            assertEquals(0, red.cargoCount());
            assertEquals(0, red.availableCharges(), "one cube and both charges");
        }

        @Test
        @DisplayName("a ship with neither cargo nor charges cannot be robbed further")
        void aStrippedShipCannotBeRobbedFurther() {
            Ship red = Ships.openShip();
            red.fillRemainingCabinsWithHumans();
            Flight flight = flightOf(red, shipWithFirepowerHalves(2));

            AdventureResolution resolution =
                    smugglers(3, Map.of(GoodColor.GREEN, 1), 2, 1).resolve(flight);
            declare(resolution, PlayerColor.RED);

            assertEquals(0, red.cargoCount());
            assertEquals(2, red.crewCount(), "the smugglers want cargo, not people");
            assertEquals(PlayerColor.BLUE, resolution.pending().orElseThrow().player());
        }
    }

    @Nested
    @DisplayName("the printed values")
    class PrintedValues {

        @Test
        @DisplayName("smugglers carrying nothing are not worth fighting, and are refused")
        void anEmptyHaulIsRefused() {
            assertThrows(IllegalArgumentException.class,
                    () -> smugglers(4, Map.of(GoodColor.GREEN, 0), 2, 1));
        }

        @Test
        @DisplayName("the shipped smuggler cards carry the values printed on them")
        void theShippedCardsMatchTheirArtwork() {
            GameData data = GameDataLoader.loadBundled();
            SmugglersCard card = (SmugglersCard) data.playableCard("smugglers_lvl1").orElseThrow();

            assertEquals(4, card.firepower());
            assertEquals(2, card.goodsPenalty());
            assertEquals(1, card.flightDays());
            assertEquals(Map.of(GoodColor.YELLOW, 1, GoodColor.GREEN, 1, GoodColor.BLUE, 1),
                    card.goods());
        }
    }
}
