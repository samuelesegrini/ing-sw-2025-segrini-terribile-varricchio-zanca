package it.polimi.ingsw.server.model.flight;

import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.ScoreSheet;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the accounts at journey's end, against manual p.15 and p.20.
 *
 * <p>Three of the four lines treat a player who gave up differently, and the fourth
 * deliberately does not. Retiring costs the finishing reward and the prettiest ship
 * prize, and halves what the cargo fetches — but the bill for everything lost along the
 * way arrives all the same. That asymmetry is what makes giving up a real decision
 * rather than a free escape.
 *
 * <p>Two details are easy to get subtly wrong and are tested for their own sake: every
 * tied ship collects the <em>full</em> prettiest-ship reward rather than sharing it, and
 * a retiree gets half of the whole manifest rounded up rather than half of each cube,
 * which for a mixed cargo is a slightly better deal.
 *
 * <p>Components involved: {@link FlightScorer}, {@link ScoreSheet}, {@link Flight}.
 */
class ScoringTest {

    private static final Position HOLD = new Position(1, 2);

    /** A ship with a special hold carrying the given cubes, and nothing else added. */
    private static Ship shipCarrying(GoodColor... cubes) {
        Ship ship = Ships.openShip();
        Ships.put(ship, HOLD, ComponentKind.SPECIAL_CARGO_HOLD);
        ship.beginCargoOperations(Map.of(GoodColor.RED, 9, GoodColor.YELLOW, 9,
                GoodColor.GREEN, 9, GoodColor.BLUE, 9));
        for (GoodColor cube : cubes) {
            ship.load(HOLD, cube);
        }
        ship.endCargoOperations();
        return ship;
    }

    private static Flight flightOf(Map<PlayerColor, Ship> ships) {
        return FlightFixtures.levelTwoFlight(ships);
    }

    private static Map<PlayerColor, Ship> ships(PlayerColor first, Ship firstShip,
                                                PlayerColor second, Ship secondShip) {
        Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
        ships.put(first, firstShip);
        ships.put(second, secondShip);
        return ships;
    }

    private static ScoreSheet sheetFor(List<ScoreSheet> sheets, PlayerColor player) {
        return sheets.stream()
                .filter(sheet -> sheet.player() == player)
                .findFirst()
                .orElseThrow();
    }

    @Nested
    @DisplayName("the finishing reward")
    class FinishingReward {

        @Test
        @DisplayName("finishers are paid by their place on the route: eight, six, four, two")
        void finishersArePaidByPlace() {
            Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
            ships.put(PlayerColor.RED, Ships.openShip());
            ships.put(PlayerColor.BLUE, Ships.openShip());
            ships.put(PlayerColor.GREEN, Ships.openShip());

            List<ScoreSheet> sheets = FlightScorer.settle(flightOf(ships));

            assertEquals(8, sheetFor(sheets, PlayerColor.RED).finishReward());
            assertEquals(6, sheetFor(sheets, PlayerColor.BLUE).finishReward());
            assertEquals(4, sheetFor(sheets, PlayerColor.GREEN).finishReward());
        }

        @Test
        @DisplayName("a player who gave up is paid nothing for arriving, because they did not")
        void aRetireeIsPaidNothingForArriving() {
            Flight flight = flightOf(ships(PlayerColor.RED, Ships.openShip(),
                    PlayerColor.BLUE, Ships.openShip()));
            flight.giveUp(PlayerColor.BLUE);

            List<ScoreSheet> sheets = FlightScorer.settle(flight);

            assertEquals(8, sheetFor(sheets, PlayerColor.RED).finishReward());
            assertEquals(0, sheetFor(sheets, PlayerColor.BLUE).finishReward());
            assertFalse(sheetFor(sheets, PlayerColor.BLUE).finishedTheFlight());
        }

        @Test
        @DisplayName("the player who overtook is paid first, whatever order they started in")
        void theRewardFollowsTheFinalOrder() {
            Flight flight = flightOf(ships(PlayerColor.RED, Ships.openShip(),
                    PlayerColor.BLUE, Ships.openShip()));
            flight.route().advance(PlayerColor.BLUE, 8);

            List<ScoreSheet> sheets = FlightScorer.settle(flight);

            assertEquals(8, sheetFor(sheets, PlayerColor.BLUE).finishReward());
            assertEquals(6, sheetFor(sheets, PlayerColor.RED).finishReward());
        }
    }

    @Nested
    @DisplayName("the prettiest ship")
    class PrettiestShip {

        @Test
        @DisplayName("the fewest exposed connectors wins four credits")
        void fewestExposedConnectorsWins() {
            Ship tidy = Ships.openShip();
            Ship untidy = Ships.openShip();
            Ships.put(untidy, HOLD, ComponentKind.STRUCTURAL_MODULE);

            List<ScoreSheet> sheets = FlightScorer.settle(
                    flightOf(ships(PlayerColor.RED, untidy, PlayerColor.BLUE, tidy)));

            assertTrue(tidy.exposedConnectors() < untidy.exposedConnectors());
            assertEquals(4, sheetFor(sheets, PlayerColor.BLUE).prettiestShip());
            assertEquals(0, sheetFor(sheets, PlayerColor.RED).prettiestShip());
        }

        @Test
        @DisplayName("every tied ship collects the full reward rather than sharing it")
        void tiedShipsBothCollectInFull() {
            List<ScoreSheet> sheets = FlightScorer.settle(
                    flightOf(ships(PlayerColor.RED, Ships.openShip(),
                            PlayerColor.BLUE, Ships.openShip())));

            assertEquals(4, sheetFor(sheets, PlayerColor.RED).prettiestShip());
            assertEquals(4, sheetFor(sheets, PlayerColor.BLUE).prettiestShip());
        }

        @Test
        @DisplayName("a player who gave up does not enter the contest, however tidy their ship is")
        void aRetireeDoesNotEnterTheContest() {
            Ship tidy = Ships.openShip();
            Ship untidy = Ships.openShip();
            Ships.put(untidy, HOLD, ComponentKind.STRUCTURAL_MODULE);

            Flight flight = flightOf(ships(PlayerColor.RED, untidy, PlayerColor.BLUE, tidy));
            flight.giveUp(PlayerColor.BLUE);

            List<ScoreSheet> sheets = FlightScorer.settle(flight);

            assertEquals(0, sheetFor(sheets, PlayerColor.BLUE).prettiestShip());
            assertEquals(4, sheetFor(sheets, PlayerColor.RED).prettiestShip(),
                    "with the tidier ship out of the running, the untidy one wins by default");
        }
    }

    @Nested
    @DisplayName("selling the cargo")
    class SellingTheCargo {

        @Test
        @DisplayName("a finisher is paid the full price list: four, three, two, one")
        void aFinisherIsPaidInFull() {
            Ship laden = shipCarrying(GoodColor.RED, GoodColor.GREEN);

            List<ScoreSheet> sheets = FlightScorer.settle(
                    flightOf(ships(PlayerColor.RED, laden, PlayerColor.BLUE, Ships.openShip())));

            assertEquals(6, sheetFor(sheets, PlayerColor.RED).goodsSold());
            assertEquals(0, sheetFor(sheets, PlayerColor.BLUE).goodsSold());
        }

        @Test
        @DisplayName("a retiree gets half the whole manifest rounded up, not half of each cube")
        void aRetireeGetsHalfTheManifestRoundedUp() {
            Ship laden = shipCarrying(GoodColor.RED, GoodColor.BLUE);

            Flight flight = flightOf(ships(PlayerColor.RED, Ships.openShip(),
                    PlayerColor.BLUE, laden));
            flight.giveUp(PlayerColor.BLUE);

            List<ScoreSheet> sheets = FlightScorer.settle(flight);

            assertEquals(3, sheetFor(sheets, PlayerColor.BLUE).goodsSold(),
                    "five credits of cargo, halved and rounded up");
        }

        @Test
        @DisplayName("selling empties the holds and hands every cube back to the bank")
        void sellingEmptiesTheHolds() {
            Ship laden = shipCarrying(GoodColor.RED, GoodColor.GREEN);

            FlightScorer.settle(flightOf(ships(PlayerColor.RED, laden,
                    PlayerColor.BLUE, Ships.openShip())));

            assertEquals(0, laden.cargoCount());
        }
    }

    @Nested
    @DisplayName("the bill for what was lost")
    class TheBill {

        @Test
        @DisplayName("every component in the discard pile costs a credit, finisher or not")
        void everyLostComponentCostsACredit() {
            Ship battered = Ships.openShip();
            Ships.put(battered, HOLD, ComponentKind.STRUCTURAL_MODULE);
            Ships.put(battered, new Position(2, 1), ComponentKind.STRUCTURAL_MODULE);
            battered.discard(HOLD);
            battered.discard(new Position(2, 1));

            Flight flight = flightOf(ships(PlayerColor.RED, battered,
                    PlayerColor.BLUE, Ships.openShip()));
            flight.giveUp(PlayerColor.RED);

            List<ScoreSheet> sheets = FlightScorer.settle(flight);

            assertEquals(2, battered.lostComponentCount());
            assertEquals(2, sheetFor(sheets, PlayerColor.RED).lostComponents(),
                    "the bill arrives whether or not the player finished");
        }

        @Test
        @DisplayName("a player can end the flight owing money, because trucking is a risky business")
        void aPlayerCanEndInDebt() {
            Ship wreck = Ships.openShip();
            Ships.put(wreck, HOLD, ComponentKind.STRUCTURAL_MODULE);
            wreck.discard(HOLD);
            wreck.writeOffAbandoned(20);

            Flight flight = flightOf(ships(PlayerColor.RED, Ships.openShip(),
                    PlayerColor.BLUE, wreck));
            flight.giveUp(PlayerColor.BLUE);

            ScoreSheet sheet = sheetFor(FlightScorer.settle(flight), PlayerColor.BLUE);

            assertEquals(-21, sheet.total());
            assertFalse(sheet.isProfitable());
        }

        @Test
        @DisplayName("a tile left in the reservation corner is written off when building ends")
        void abandonedReservationsAreWrittenOff() {
            Ship ship = Ships.openShip();

            ship.writeOffAbandoned(2);

            assertEquals(2, ship.lostComponentCount());
        }
    }

    @Nested
    @DisplayName("the final table")
    class FinalTable {

        @Test
        @DisplayName("one credit is enough to have won, which is the manual being generous")
        void oneCreditIsEnoughToHaveWon() {
            Ship ship = Ships.openShip();
            ship.writeOffAbandoned(11);

            Flight flight = flightOf(ships(PlayerColor.RED, ship, PlayerColor.BLUE, Ships.openShip()));
            ScoreSheet sheet = sheetFor(FlightScorer.settle(flight), PlayerColor.RED);

            assertEquals(1, sheet.total(), "eight for arriving and four for the ship, less eleven lost");
            assertTrue(sheet.isProfitable());
        }

        @Test
        @DisplayName("the table reads richest first, so the winner is the first row")
        void theTableReadsRichestFirst() {
            Ship poor = Ships.openShip();
            poor.writeOffAbandoned(20);

            List<ScoreSheet> sheets = FlightScorer.settle(
                    flightOf(ships(PlayerColor.RED, poor, PlayerColor.BLUE, Ships.openShip())));

            assertEquals(PlayerColor.BLUE, sheets.getFirst().player());
            assertEquals(PlayerColor.RED, sheets.getLast().player());
        }

        @Test
        @DisplayName("a retiree still appears in the table, because they are still in the game")
        void aRetireeStillAppears() {
            Flight flight = flightOf(ships(PlayerColor.RED, Ships.openShip(),
                    PlayerColor.BLUE, Ships.openShip()));
            flight.giveUp(PlayerColor.BLUE);

            List<ScoreSheet> sheets = FlightScorer.settle(flight);

            assertEquals(2, sheets.size());
            assertFalse(sheetFor(sheets, PlayerColor.BLUE).finishedTheFlight());
        }
    }
}
