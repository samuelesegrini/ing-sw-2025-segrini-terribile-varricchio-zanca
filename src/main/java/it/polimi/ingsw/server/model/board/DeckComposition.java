package it.polimi.ingsw.server.model.board;

import it.polimi.ingsw.common.game.CardLevel;

import java.util.EnumMap;
import java.util.Map;

/**
 * How a flight's adventure deck is put together.
 *
 * <p>A level II flight builds four piles of three cards, each pile holding two level II
 * cards and one level I card; three piles sit face down where players may peek at them
 * during building and the fourth stays unknown (manual p.16). A test flight uses a
 * single pile of the eight cards bearing the L mark (manual p.9).
 *
 * @param piles               how many piles the deck is dealt into
 * @param cardsPerPile        how many cards of each level go into every pile
 * @param testFlightCardsOnly whether the draw is restricted to cards bearing the L mark
 */
public record DeckComposition(int piles, Map<CardLevel, Integer> cardsPerPile, boolean testFlightCardsOnly) {

    /**
     * Validates the composition and takes a defensive copy.
     *
     * @throws IllegalArgumentException if there are no piles, or a pile would be empty
     */
    public DeckComposition {
        if (piles < 1) {
            throw new IllegalArgumentException("a deck needs at least one pile, got " + piles);
        }
        Map<CardLevel, Integer> counts = new EnumMap<>(CardLevel.class);
        cardsPerPile.forEach((level, count) -> {
            if (count > 0) {
                counts.put(level, count);
            }
        });
        if (counts.isEmpty()) {
            throw new IllegalArgumentException("a pile needs at least one card");
        }
        cardsPerPile = Map.copyOf(counts);
    }

    /**
     * Returns how many cards the finished deck holds.
     *
     * @return the total number of cards drawn for the flight
     */
    public int totalCards() {
        return piles * cardsPerPile.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Returns how many cards of the given level the whole deck needs.
     *
     * @param level the card level
     * @return the number of cards to draw from that level's pool
     */
    public int totalCardsOfLevel(CardLevel level) {
        return piles * cardsPerPile.getOrDefault(level, 0);
    }

    /**
     * Returns how many piles players may look at while building.
     *
     * <p>The pile at the top of the flight board is never visible before the flight, so
     * it is always one fewer than the total (manual p.16).
     *
     * @return the number of peekable piles
     */
    public int peekablePiles() {
        return piles - 1;
    }
}
