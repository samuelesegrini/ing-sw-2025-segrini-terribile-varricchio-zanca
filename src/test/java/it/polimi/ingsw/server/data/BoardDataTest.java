package it.polimi.ingsw.server.data;

import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.server.model.board.LevelSpec;
import it.polimi.ingsw.common.game.Position;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the board data against the printed artwork in {@code assets/cardboard}.
 *
 * <p>Board numbers are read by the whole flight phase — movement, scoring, dice
 * targeting — so a wrong one is a rule bug everywhere at once rather than in one
 * place.
 *
 * <p>Components involved: {@link GameDataLoader}, {@link LevelSpec} and the board
 * specifications it holds.
 */
class BoardDataTest {

    private static GameData data;

    @BeforeAll
    static void loadCatalogue() {
        data = GameDataLoader.loadBundled();
    }

    @Nested
    @DisplayName("test flight board (flight-board_lvl-1.png, ship-grid_lvl-1.jpg)")
    class TestFlightBoard {

        private final LevelSpec spec = data.level(GameLevel.TEST_FLIGHT);

        @Test
        @DisplayName("has an 18 space route with start spaces at 4, 2, 1 and 0")
        void route_isEighteenSpacesWithFourStarts() {
            assertEquals(18, spec.flightBoard().routeLength());
            assertEquals(List.of(4, 2, 1, 0), spec.flightBoard().startingPositions());
            assertEquals(4, spec.flightBoard().maximumPlayers());
        }

        @Test
        @DisplayName("pays 4, 3, 2, 1 for finishing and 2 for the prettiest ship")
        void rewards_matchThePrintedBoard() {
            assertEquals(4, spec.flightBoard().rewards().finishReward(1));
            assertEquals(1, spec.flightBoard().rewards().finishReward(4));
            assertEquals(0, spec.flightBoard().rewards().finishReward(5));
            assertEquals(2, spec.flightBoard().rewards().prettiestShip());
        }

        @Test
        @DisplayName("leaves 18 usable cells with the starting cabin at the centre")
        void shipOutline_hasEighteenUsableCells() {
            assertEquals(18, spec.shipBoard().usableCells().size());
            assertEquals(new Position(2, 3), spec.shipBoard().startingCabin());
        }

        @Test
        @DisplayName("is untimed and allows neither reserving nor peeking, per manual p.8")
        void optionalRules_areAllOff() {
            assertFalse(spec.rules().hourglass());
            assertFalse(spec.rules().componentReservation());
            assertFalse(spec.rules().cardPilePeeking());
            assertFalse(spec.rules().aliens());
            assertFalse(spec.rules().illegalShipCreditPenalty());
            assertEquals(0, spec.flightBoard().hourglassSpaces());
        }

        @Test
        @DisplayName("draws a single pile of the 8 cards bearing the L mark")
        void deck_isTheEightMarkedCards() {
            assertEquals(1, spec.flightBoard().deck().piles());
            assertEquals(8, spec.flightBoard().deck().totalCards());
            assertTrue(spec.flightBoard().deck().testFlightCardsOnly());
        }
    }

    @Nested
    @DisplayName("level II board (flight-board_lvl-2.png, ship-grid_lvl-2.jpg)")
    class LevelTwoBoard {

        private final LevelSpec spec = data.level(GameLevel.LEVEL_II);

        @Test
        @DisplayName("has a 24 space route with start spaces at 6, 3, 1 and 0")
        void route_isTwentyFourSpacesWithFourStarts() {
            assertEquals(24, spec.flightBoard().routeLength());
            assertEquals(List.of(6, 3, 1, 0), spec.flightBoard().startingPositions());
        }

        @Test
        @DisplayName("pays 8, 6, 4, 2 for finishing and 4 for the prettiest ship")
        void rewards_matchThePrintedBoard() {
            assertEquals(8, spec.flightBoard().rewards().finishReward(1));
            assertEquals(2, spec.flightBoard().rewards().finishReward(4));
            assertEquals(4, spec.flightBoard().rewards().prettiestShip());
        }

        @Test
        @DisplayName("leaves 27 usable cells")
        void shipOutline_hasTwentySevenUsableCells() {
            assertEquals(27, spec.shipBoard().usableCells().size());
        }

        @Test
        @DisplayName("runs the hourglass three times and allows two reservations")
        void optionalRules_areAllOn() {
            assertEquals(3, spec.flightBoard().hourglassSpaces());
            assertTrue(spec.rules().hourglass());
            assertEquals(2, spec.shipBoard().reservationSlots());
            assertTrue(spec.rules().componentReservation());
            assertTrue(spec.rules().cardPilePeeking());
            assertTrue(spec.rules().aliens());
            assertTrue(spec.rules().illegalShipCreditPenalty());
        }

        @Test
        @DisplayName("builds four piles of one level I and two level II cards, three of them peekable")
        void deck_isFourPilesOfThree() {
            assertEquals(4, spec.flightBoard().deck().piles());
            assertEquals(12, spec.flightBoard().deck().totalCards());
            assertEquals(4, spec.flightBoard().deck().totalCardsOfLevel(CardLevel.LEVEL_I));
            assertEquals(8, spec.flightBoard().deck().totalCardsOfLevel(CardLevel.LEVEL_II));
            assertEquals(3, spec.flightBoard().deck().peekablePiles());
        }
    }

    @Test
    @DisplayName("goods sell for 4, 3, 2, 1 on both boards, red down to blue")
    void goodsPrices_matchBothPrintedPriceLists() {
        for (GameLevel level : GameLevel.values()) {
            var rewards = data.level(level).flightBoard().rewards();
            assertEquals(4, rewards.priceOf(GoodColor.RED), level.toString());
            assertEquals(3, rewards.priceOf(GoodColor.YELLOW), level.toString());
            assertEquals(2, rewards.priceOf(GoodColor.GREEN), level.toString());
            assertEquals(1, rewards.priceOf(GoodColor.BLUE), level.toString());
            assertEquals(1, rewards.lostComponentPenalty(), level.toString());
        }
    }

    @Test
    @DisplayName("the bank holds some of every colour, since a shortage of everything is not a game")
    void bankStock_holdsSomeOfEveryColour() {
        for (GoodColor color : GoodColor.values()) {
            assertTrue(data.bankStock().get(color) > 0, color + " cubes are missing from the bank");
        }
    }

    @Test
    @DisplayName("dice sums address columns 4 to 10 and rows 5 to 9, anything else is a clean miss")
    void diceSums_outsideThePrintedLabels_addressNoLine() {
        var board = data.level(GameLevel.LEVEL_II).shipBoard();

        assertEquals(OptionalInt.of(0), board.columnForDiceSum(4));
        assertEquals(OptionalInt.of(6), board.columnForDiceSum(10));
        assertEquals(OptionalInt.empty(), board.columnForDiceSum(3));
        assertEquals(OptionalInt.empty(), board.columnForDiceSum(11));

        assertEquals(OptionalInt.of(0), board.rowForDiceSum(5));
        assertEquals(OptionalInt.of(4), board.rowForDiceSum(9));
        assertEquals(OptionalInt.empty(), board.rowForDiceSum(4));
        assertEquals(OptionalInt.empty(), board.rowForDiceSum(10));
    }
}
