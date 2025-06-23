package it.polimi.ingsw.server.model.enums;

import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameLevelTest {

    @Test
    void testGameLevelValuesAndGetters_TEST_FLIGHT() {
        GameLevel level = GameLevel.TEST_FLIGHT;

        assertEquals(0, level.getDuration());
        assertEquals(0, level.getPredictablePileCount()); // Test Flight has 0 predictable piles
        assertEquals(8, level.getCardsPerPile()); // Total cards for TF's single pile
        assertEquals(CardLevel.TEST_FLIGHT, level.getPrimaryCardLevel());

        Map<CardLevel, Integer> predictableComp = level.getCardsPerPredictablePileComposition();
        assertNotNull(predictableComp);
        assertEquals(1, predictableComp.size()); // Defines the single type of card
        assertEquals(8, predictableComp.get(CardLevel.TEST_FLIGHT));

        Map<CardLevel, Integer> totalPredictable = level.getTotalCardsForPredictablePiles();
        assertNotNull(totalPredictable);
        assertTrue(totalPredictable.isEmpty(), "Test Flight should have no cards in 'total predictable piles'");

        Map<CardLevel, Integer> unknownComp = level.getCardsForUnknownPileComposition();
        assertNotNull(unknownComp);
        assertTrue(unknownComp.isEmpty(), "Test Flight should have no separate 'unknown pile composition'");
    }

    @Test
    void testGameLevelValuesAndGetters_LEVEL_II() {
        GameLevel level = GameLevel.LEVEL_II;

        assertEquals(0, level.getDuration()); // Placeholder
        assertEquals(3, level.getPredictablePileCount());
        assertEquals(3, level.getCardsPerPile()); // Cards in each of the 3 predictable piles
        assertEquals(CardLevel.LEVEL_II, level.getPrimaryCardLevel());

        Map<CardLevel, Integer> predictableComp = level.getCardsPerPredictablePileComposition();
        assertNotNull(predictableComp);
        assertEquals(2, predictableComp.size()); // L2 and L1 cards
        assertEquals(2, predictableComp.get(CardLevel.LEVEL_II));
        assertEquals(1, predictableComp.get(CardLevel.LEVEL_I));

        Map<CardLevel, Integer> totalPredictable = level.getTotalCardsForPredictablePiles();
        assertNotNull(totalPredictable);
        assertEquals(2, totalPredictable.size());
        assertEquals(2 * 3, totalPredictable.get(CardLevel.LEVEL_II)); // 2 L2 cards * 3 piles
        assertEquals(1 * 3, totalPredictable.get(CardLevel.LEVEL_I));  // 1 L1 card * 3 piles

        Map<CardLevel, Integer> unknownComp = level.getCardsForUnknownPileComposition();
        assertNotNull(unknownComp);
        assertEquals(2, unknownComp.size()); // Assumed same composition as predictable
        assertEquals(2, unknownComp.get(CardLevel.LEVEL_II));
        assertEquals(1, unknownComp.get(CardLevel.LEVEL_I));
    }

    @Test
    void testEnumToStringAndValueOf() {
        assertEquals("TEST_FLIGHT", GameLevel.TEST_FLIGHT.toString());
        assertEquals(GameLevel.LEVEL_II, GameLevel.valueOf("LEVEL_II"));
    }

    // Old tests that might have used getCardPerPile or getCardDistributionForPile
    // need to be re-evaluated based on the new methods.

    // Example of how previous tests might be adapted:
    // If a test was checking the total cards in predictable piles:
    @Test
    void testTotalCardsInPredictablePiles_LEVEL_II() {
        GameLevel level = GameLevel.LEVEL_II;
        Map<CardLevel, Integer> totalCards = level.getTotalCardsForPredictablePiles();
        int sum = totalCards.values().stream().mapToInt(Integer::intValue).sum();
        // For LEVEL_II with 3 predictable piles, each having 2 L2 and 1 L1:
        // Total = (2 L2 + 1 L1) * 3 piles = 3 cards/pile * 3 piles = 9 cards
        assertEquals(9, sum);
        assertEquals(6, totalCards.get(CardLevel.LEVEL_II));
        assertEquals(3, totalCards.get(CardLevel.LEVEL_I));
    }

    @Test
    void testTotalCardsInPredictablePiles_TEST_FLIGHT() {
        GameLevel level = GameLevel.TEST_FLIGHT;
        Map<CardLevel, Integer> totalCards = level.getTotalCardsForPredictablePiles();
        int sum = totalCards.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(0, sum, "Test Flight has no predictable piles in the L2/L3 sense.");
    }

    @Test
    void testCardsInUnknownPile_LEVEL_II() {
        GameLevel level = GameLevel.LEVEL_II;
        Map<CardLevel, Integer> unknownPileCards = level.getCardsForUnknownPileComposition();
        int sum = unknownPileCards.values().stream().mapToInt(Integer::intValue).sum();
        // For LEVEL_II, unknown pile has (2 L2 + 1 L1) = 3 cards
        assertEquals(3, sum);
        assertEquals(2, unknownPileCards.get(CardLevel.LEVEL_II));
        assertEquals(1, unknownPileCards.get(CardLevel.LEVEL_I));
    }

    @Test
    void testCardsInUnknownPile_TEST_FLIGHT() {
        GameLevel level = GameLevel.TEST_FLIGHT;
        Map<CardLevel, Integer> unknownPileCards = level.getCardsForUnknownPileComposition();
        int sum = unknownPileCards.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(0, sum, "Test Flight has no separate unknown pile composition.");
    }
}