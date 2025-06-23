package it.polimi.ingsw.server.model.enums;

import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

public enum GameLevel {
    TEST_FLIGHT(0, 0, 8, CardLevel.TEST_FLIGHT,
            Map.of(CardLevel.TEST_FLIGHT, 8) // All 8 cards are TEST_FLIGHT
    ),
    LEVEL_II(0, // duration - placeholder
            3, // predictablePileCount (3 bottom piles)
            3, // cardsPerPile (e.g., 2xL2, 1xL1 per predictable pile)
            CardLevel.LEVEL_II, // primaryCardLevel for the game
            Map.of( // Example distribution for ONE predictable pile for LEVEL_II (or III)
                    CardLevel.LEVEL_II, 2,
                    CardLevel.LEVEL_I, 1
                    // TEST_FLIGHT cards can be considered part of LEVEL_I for this purpose
            )
    );

    private final int duration;
    private final int predictablePileCount; // Number of piles that can be previewed
    private final int cardsPerPile;
    private final CardLevel primaryCardLevel; // Overall level of the game/flight
    private final Map<CardLevel, Integer> cardsPerPredictablePileComposition; // Composition of ONE predictable pile

    GameLevel(int duration, int predictablePileCount, int cardsPerPile, CardLevel primaryCardLevel,
              Map<CardLevel, Integer> cardsPerPredictablePileComposition) {
        this.duration = duration;
        this.predictablePileCount = predictablePileCount;
        this.cardsPerPile = cardsPerPile;
        this.primaryCardLevel = primaryCardLevel;
        this.cardsPerPredictablePileComposition = cardsPerPredictablePileComposition;
    }

    public int getDuration() { return duration; }
    public int getPredictablePileCount() { return predictablePileCount; }

    public int getCardsPerPile() { return cardsPerPile; }
    public CardLevel getPrimaryCardLevel() { return primaryCardLevel; }

    /**
     * Gets the composition for ONE predictable pile.
     * The "unknown" top pile might need separate logic or be what's left over.
     */
    public Map<CardLevel, Integer> getCardsPerPredictablePileComposition() {
        return cardsPerPredictablePileComposition;
    }

    /**
     * Returns the total number of cards of each CardLevel needed for all predictable piles.
     */
    public Map<CardLevel, Integer> getTotalCardsForPredictablePiles() {
        Map<CardLevel, Integer> total = new HashMap<>();
        if (predictablePileCount == 0) return total;

        cardsPerPredictablePileComposition.forEach((level, count) -> {
            total.put(level, total.getOrDefault(level, 0) + count * predictablePileCount);
        });
        return total;
    }

    /**
     * Returns the total number of cards for the "unknown" top pile.
     * This is a placeholder - the actual number needs to be defined by game rules.
     * For Level II/III, if there are 4 piles total and 3 are predictable, this is for the 4th pile.
     * Assuming the unknown pile has the same composition as predictable ones for now.
     */
    public Map<CardLevel, Integer> getCardsForUnknownPileComposition() {
        if (this == TEST_FLIGHT) return Map.of(); // No separate unknown pile in Test Flight
        return cardsPerPredictablePileComposition;
    }
}