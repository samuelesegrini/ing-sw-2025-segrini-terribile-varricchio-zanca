package it.polimi.ingsw.server.model.adventure;

import it.polimi.ingsw.server.model.board.DeckComposition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * The adventure cards dealt out for one flight, still in their piles.
 *
 * <p>A level II flight builds four piles of three cards, two level II cards and one
 * level I card in each. Three of them lie face down at the bottom of the flight board
 * where players may look at them while building; the fourth sits at the top and nobody
 * sees it before the flight (manual p.16).
 *
 * <p>That asymmetry is the point of the whole mechanism. Three quarters of the flight can
 * be scouted, so a player can build for the dangers they know are coming — and the last
 * quarter cannot, so no ship is ever safe.
 */
public final class AdventureDeck {

    private final List<List<AdventureCardIdentity>> piles;

    private AdventureDeck(List<List<AdventureCardIdentity>> piles) {
        this.piles = piles;
    }

    /**
     * Deals the piles for a flight.
     *
     * <p>Each level's pool is shuffled on its own before any pile is built, as the manual
     * describes, so a pile's level mix is fixed but its contents are not.
     *
     * @param composition how many piles to build and what goes in each
     * @param pools       the cards available at each level, already filtered to those the
     *                    flight may draw
     * @param random      the source of randomness
     * @return the dealt piles
     * @throws IllegalArgumentException if a pool is too small for what the composition asks
     */
    public static AdventureDeck deal(DeckComposition composition,
                                     Map<CardLevel, List<AdventureCardIdentity>> pools,
                                     RandomGenerator random) {
        Map<CardLevel, List<AdventureCardIdentity>> shuffled = new java.util.EnumMap<>(CardLevel.class);
        for (CardLevel level : CardLevel.values()) {
            int needed = composition.totalCardsOfLevel(level);
            if (needed == 0) {
                continue;
            }
            List<AdventureCardIdentity> pool = new ArrayList<>(pools.getOrDefault(level, List.of()));
            if (pool.size() < needed) {
                throw new IllegalArgumentException(
                        "the flight needs " + needed + " " + level + " cards but only "
                                + pool.size() + " are available");
            }
            java.util.Collections.shuffle(pool, new java.util.Random(random.nextLong()));
            shuffled.put(level, pool);
        }

        List<List<AdventureCardIdentity>> piles = new ArrayList<>();
        for (int pile = 0; pile < composition.piles(); pile++) {
            List<AdventureCardIdentity> cards = new ArrayList<>();
            composition.cardsPerPile().forEach((level, count) -> {
                List<AdventureCardIdentity> pool = shuffled.get(level);
                for (int i = 0; i < count; i++) {
                    cards.add(pool.removeLast());
                }
            });
            piles.add(List.copyOf(cards));
        }
        return new AdventureDeck(List.copyOf(piles));
    }

    /**
     * Returns how many piles the deck was dealt into.
     *
     * @return the number of piles
     */
    public int pileCount() {
        return piles.size();
    }

    /**
     * Tells whether a pile may be looked at during building.
     *
     * <p>Every pile but the last: the one at the top of the flight board stays unknown to
     * everyone until it is turned over in flight.
     *
     * @param pile the pile index, counting from zero
     * @return {@code true} when players may scout that pile
     */
    public boolean isPeekable(int pile) {
        return pile >= 0 && pile < piles.size() - 1;
    }

    /**
     * Returns the cards in a pile.
     *
     * @param pile the pile index, counting from zero
     * @return the cards it holds, in the order they were dealt
     * @throws IllegalArgumentException if there is no such pile
     */
    public List<AdventureCardIdentity> pile(int pile) {
        if (pile < 0 || pile >= piles.size()) {
            throw new IllegalArgumentException("there is no pile " + pile + " in a deck of " + piles.size());
        }
        return piles.get(pile);
    }

    /**
     * Returns every card dealt for this flight, pile by pile.
     *
     * <p>Server-side only until the piles are merged for the flight: handing this to a
     * client would give away the pile nobody is allowed to see.
     *
     * @return all the cards in the deck
     */
    public List<AdventureCardIdentity> cards() {
        return piles.stream().flatMap(List::stream).toList();
    }
}
