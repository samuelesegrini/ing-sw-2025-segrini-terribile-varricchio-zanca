package it.polimi.ingsw.server.data;

import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.server.model.goods.GoodsBank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the number of goods cubes the game ships with.
 *
 * <p>Neither rulebook prints the figure: page 2 of both editions lists the cubes with a
 * picture and no number. These counts were taken from a physical copy, and they are the
 * only reason the shortage rule of manual p.19 can ever fire — with an unlimited bank it
 * would be mechanically present and practically dead.
 *
 * <p>So they are asserted rather than merely stored. A quiet edit to the data file would
 * otherwise change how often ships go home empty, and nothing would notice.
 *
 * <p>Components involved: {@link GameDataLoader}, {@link GoodsBank}.
 */
class GoodsBankDataTest {

    private static final GameData DATA = GameDataLoader.loadBundled();

    @DisplayName("the bank holds the cubes counted from the physical game")
    @ParameterizedTest(name = "{0} cubes: {1}")
    @CsvSource({
            "RED, 8",
            "YELLOW, 12",
            "GREEN, 16",
            "BLUE, 20"
    })
    void bankStock_matchesThePhysicalGame(GoodColor color, int expected) {
        assertEquals(expected, DATA.bankStock().get(color));
    }

    @Test
    @DisplayName("the scarcest cubes are the most valuable, which is what makes red worth chasing")
    void scarcityFollowsValue() {
        assertTrue(DATA.bankStock().get(GoodColor.RED) < DATA.bankStock().get(GoodColor.YELLOW));
        assertTrue(DATA.bankStock().get(GoodColor.YELLOW) < DATA.bankStock().get(GoodColor.GREEN));
        assertTrue(DATA.bankStock().get(GoodColor.GREEN) < DATA.bankStock().get(GoodColor.BLUE));
    }

    @Test
    @DisplayName("a bank built from the shipped stock starts full and hands cubes out until it runs dry")
    void aBankBuiltFromTheStockRunsDry() {
        GoodsBank bank = new GoodsBank(DATA.bankStock());

        int red = bank.available(GoodColor.RED);
        for (int i = 0; i < red; i++) {
            assertTrue(bank.take(GoodColor.RED));
        }

        assertEquals(0, bank.available(GoodColor.RED));
        assertTrue(bank.available(GoodColor.BLUE) > 0, "running out of one colour is not running out of all");
    }
}
