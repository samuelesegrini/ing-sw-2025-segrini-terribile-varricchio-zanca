package it.polimi.ingsw.server.model.enums;

import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import java.util.Map;

public enum GameLevel {
    // sistemare valori
    TEST_FLIGHT(0, 0, 8, null),

    // sistema valori
    LEVEL_II(0, 3, 0, null);

    private final int duration;
    private final int predictablePileCount;
    private final int cardPerPile;
    private final CardLevel primaryCardLevel;

    private GameLevel(int duration, int predictablePileCount, int cardPerPile, CardLevel primaryCardLevel) {
        this.duration = duration;
        this.predictablePileCount = predictablePileCount;
        this.cardPerPile = cardPerPile;
        this.primaryCardLevel = primaryCardLevel;
    }

    /**
     * Gets the duration of the game level (hourglass).
     * @return The duration of the game level (hourglass).
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Gets the total number of predictable piles.
     * @return The total number of predictable piles
     */
    public int getPredictablePileCount() {
        return predictablePileCount;
    }

    /**
     * Get the number of cards per pile.
     * @return The number of cards per pile
     */
    public int getCardPerPile() {
        return cardPerPile;
    }

    /**
     * Gets the level of the primary card.
     * @return The level of the primary card
     */
    public CardLevel getPrimaryCardLevel() {
        return primaryCardLevel;
    }

    /**
     * Returns the card distribution of the piles (how many cards for each game level).
     * @return A map that associates to each card level the corresponding number of cards per pile
     */
    public Map<CardLevel, Integer> getCardDistributionForPile() {
        return null;
    }
}
