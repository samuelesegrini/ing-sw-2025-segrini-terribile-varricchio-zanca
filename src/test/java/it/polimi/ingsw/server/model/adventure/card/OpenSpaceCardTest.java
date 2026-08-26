package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.AdventureCardType;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.ShipAttribute;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.flight.FlightFixtures;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.BatteryPlan;
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
 * Checks Open Space against manual p.13.
 *
 * <p>The card that reorders a flight, and the only one where a ship gains ground rather
 * than merely losing less of it. Two things decide whether it is right: that the distance
 * travelled counts <em>empty</em> spaces, so overtaking costs nothing extra, and that the
 * order of play is settled when the card is turned over rather than re-read after each
 * ship moves.
 *
 * <p>It is also the one card that puts a player out for a reason unrelated to damage — a
 * ship with no engine power is left behind — and that give-up waits until every other
 * ship has moved, because forced conditions are checked once the card is fully resolved.
 *
 * <p>Components involved: {@link OpenSpaceCard}, {@link Flight}, {@link Ship}.
 */
class OpenSpaceCardTest {

    private static final AdventureCardIdentity IDENTITY = new AdventureCardIdentity(
            "open-space_lvl1_01", AdventureCardType.OPEN_SPACE, CardLevel.LEVEL_I, true);

    private static final Position ENGINE = new Position(1, 2);
    private static final Position BATTERY = new Position(3, 2);

    /** Cells a single engine can be welded into, all touching the starting cabin's row. */
    private static final List<Position> ENGINE_BAYS = List.of(
            ENGINE, new Position(2, 1), new Position(2, 3), new Position(2, 0));

    /** A ship with the given number of single engines and no batteries. */
    private static Ship shipWithEngines(int engines) {
        Ship ship = Ships.openShip();
        for (int i = 0; i < engines; i++) {
            Ships.put(ship, ENGINE_BAYS.get(i), ComponentKind.SINGLE_ENGINE);
        }
        return ship;
    }

    /** A ship with one double engine and a charged battery. */
    private static Ship shipWithDoubleEngine() {
        Ship ship = Ships.openShip();
        Ships.put(ship, ENGINE, ComponentKind.DOUBLE_ENGINE);
        Ships.put(ship, BATTERY, ComponentKind.BATTERY);
        ship.chargeBatteries();
        return ship;
    }

    private static Flight flightOf(Ship leader, Ship trailer) {
        Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
        ships.put(PlayerColor.RED, leader);
        ships.put(PlayerColor.BLUE, trailer);
        return FlightFixtures.levelTwoFlight(ships);
    }

    private static void declare(AdventureResolution resolution, PlayerColor player, BatteryPlan plan) {
        resolution.submit(new PlayerChoice.Declaration(player, plan));
    }

    private static void declareNothingExtra(AdventureResolution resolution) {
        while (resolution.pending().isPresent()) {
            declare(resolution, resolution.pending().orElseThrow().player(), BatteryPlan.none());
        }
    }

    @Nested
    @DisplayName("declaring and moving")
    class DeclaringAndMoving {

        @Test
        @DisplayName("each ship travels as far as the engines it declared")
        void eachShipTravelsItsEnginePower() {
            Flight flight = flightOf(shipWithEngines(2), shipWithEngines(1));
            int redBefore = flight.route().positionOf(PlayerColor.RED);
            int blueBefore = flight.route().positionOf(PlayerColor.BLUE);

            declareNothingExtra(new OpenSpaceCard(IDENTITY).resolve(flight));

            assertEquals(redBefore + 2, flight.route().positionOf(PlayerColor.RED));
            assertEquals(blueBefore + 1, flight.route().positionOf(PlayerColor.BLUE));
        }

        @Test
        @DisplayName("the leader is asked first, then the rest in route order")
        void theLeaderIsAskedFirst() {
            Flight flight = flightOf(shipWithEngines(1), shipWithEngines(1));
            AdventureResolution resolution = new OpenSpaceCard(IDENTITY).resolve(flight);

            assertEquals(PlayerColor.RED, resolution.pending().orElseThrow().player());
            declare(resolution, PlayerColor.RED, BatteryPlan.none());
            assertEquals(PlayerColor.BLUE, resolution.pending().orElseThrow().player());
        }

        @Test
        @DisplayName("the call names the doubles that could be run and the charges to run them")
        void theCallNamesWhatCouldBeRun() {
            Flight flight = flightOf(shipWithDoubleEngine(), shipWithEngines(1));

            PlayerPrompt prompt = new OpenSpaceCard(IDENTITY).resolve(flight).pending().orElseThrow();

            assertTrue(prompt instanceof PlayerPrompt.DeclarePower);
            PlayerPrompt.DeclarePower call = (PlayerPrompt.DeclarePower) prompt;
            assertEquals(ShipAttribute.ENGINE_POWER, call.attribute());
            assertEquals(Set.of(ENGINE), call.activatable());
            assertEquals(2, call.chargesAvailable());
        }

        @Test
        @DisplayName("a double engine moves a ship twice as far, and the charge is gone")
        void aDoubleEngineCostsACharge() {
            Ship ship = shipWithDoubleEngine();
            Flight flight = flightOf(ship, shipWithEngines(1));
            int before = flight.route().positionOf(PlayerColor.RED);

            AdventureResolution resolution = new OpenSpaceCard(IDENTITY).resolve(flight);
            declare(resolution, PlayerColor.RED, BatteryPlan.powering(ENGINE));
            declareNothingExtra(resolution);

            assertEquals(before + 2, flight.route().positionOf(PlayerColor.RED));
            assertEquals(1, ship.availableCharges(), "the charge was spent on declaring");
        }

        @Test
        @DisplayName("overtaking costs nothing extra, because occupied spaces are jumped for free")
        void overtakingIsFree() {
            // Red starts on 6 and blue on 3, so blue needs four empty spaces to get past.
            Flight flight = flightOf(shipWithEngines(1), shipWithEngines(4));
            AdventureResolution resolution = new OpenSpaceCard(IDENTITY).resolve(flight);

            declare(resolution, PlayerColor.RED, BatteryPlan.none());
            int redNow = flight.route().positionOf(PlayerColor.RED);
            declare(resolution, PlayerColor.BLUE, BatteryPlan.none());

            assertEquals(7, redNow);
            assertEquals(8, flight.route().positionOf(PlayerColor.BLUE),
                    "four empty spaces travelled, red's square jumped for free");
        }

        @Test
        @DisplayName("the order was settled when the card was turned over, so overtaking mid-card changes nothing")
        void overtakingDoesNotReorderTheCard() {
            Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
            ships.put(PlayerColor.RED, shipWithEngines(1));
            ships.put(PlayerColor.BLUE, shipWithEngines(4));
            ships.put(PlayerColor.GREEN, shipWithEngines(1));
            Flight flight = FlightFixtures.levelTwoFlight(ships);

            AdventureResolution resolution = new OpenSpaceCard(IDENTITY).resolve(flight);
            declare(resolution, PlayerColor.RED, BatteryPlan.none());
            declare(resolution, PlayerColor.BLUE, BatteryPlan.none());

            assertEquals(PlayerColor.BLUE, flight.stillFlying().getFirst(), "blue now leads");
            assertEquals(PlayerColor.GREEN, resolution.pending().orElseThrow().player(),
                    "green is still next, because the queue was fixed when the card came up");
        }
    }

    @Nested
    @DisplayName("a ship with no engines")
    class NoEngines {

        @Test
        @DisplayName("a ship declaring nothing is left behind and out of the race")
        void aShipDeclaringNothingIsLeftBehind() {
            Flight flight = flightOf(shipWithEngines(1), shipWithEngines(0));

            declareNothingExtra(new OpenSpaceCard(IDENTITY).resolve(flight));

            assertTrue(flight.hasGivenUp(PlayerColor.BLUE));
            assertFalse(flight.hasGivenUp(PlayerColor.RED));
        }

        @Test
        @DisplayName("it stays on the route until everybody else has moved, because the card resolves first")
        void itLeavesOnlyOnceTheCardIsDone() {
            Flight flight = flightOf(shipWithEngines(0), shipWithEngines(1));
            AdventureResolution resolution = new OpenSpaceCard(IDENTITY).resolve(flight);

            declare(resolution, PlayerColor.RED, BatteryPlan.none());

            assertFalse(flight.hasGivenUp(PlayerColor.RED),
                    "red declared nothing but blue has not moved yet");

            declare(resolution, PlayerColor.BLUE, BatteryPlan.none());

            assertTrue(flight.hasGivenUp(PlayerColor.RED));
        }

        @Test
        @DisplayName("a ship with a double engine and no charge to run it is left behind too")
        void anUnpoweredDoubleEngineIsWorthNothing() {
            Ship ship = Ships.openShip();
            Ships.put(ship, ENGINE, ComponentKind.DOUBLE_ENGINE);
            Flight flight = flightOf(ship, shipWithEngines(1));

            declareNothingExtra(new OpenSpaceCard(IDENTITY).resolve(flight));

            assertTrue(flight.hasGivenUp(PlayerColor.RED));
        }
    }

    @Nested
    @DisplayName("refusing what it did not ask for")
    class RefusingBadInput {

        @Test
        @DisplayName("an answer that is not a declaration is refused")
        void aNonDeclarationIsRefused() {
            Flight flight = flightOf(shipWithEngines(1), shipWithEngines(1));
            AdventureResolution resolution = new OpenSpaceCard(IDENTITY).resolve(flight);

            assertThrows(IllegalArgumentException.class,
                    () -> resolution.submit(new PlayerChoice.Take(PlayerColor.RED)));
        }

        @Test
        @DisplayName("a plan the ship cannot afford is refused rather than quietly reduced")
        void anUnaffordablePlanIsRefused() {
            Ship ship = Ships.openShip();
            Ships.put(ship, ENGINE, ComponentKind.DOUBLE_ENGINE);
            Flight flight = flightOf(ship, shipWithEngines(1));
            AdventureResolution resolution = new OpenSpaceCard(IDENTITY).resolve(flight);

            assertThrows(IllegalArgumentException.class,
                    () -> declare(resolution, PlayerColor.RED, BatteryPlan.powering(ENGINE)));
        }
    }

    @Test
    @DisplayName("the card knows what it is, so deck building can place it without asking")
    void theCardKnowsWhatItIs() {
        OpenSpaceCard card = new OpenSpaceCard(IDENTITY);

        assertEquals(AdventureCardType.OPEN_SPACE, card.type());
        assertEquals(CardLevel.LEVEL_I, card.level());
        assertEquals("open-space_lvl1_01", card.identity().id());
    }
}
