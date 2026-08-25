package it.polimi.ingsw.server.model.goods;

import java.util.List;

/**
 * What a ship actually gave up when a card took its valuables.
 *
 * <p>The manual's cascade, in one place: goods first and the most valuable first, then
 * battery charges once the holds are empty, and then nothing at all — "if you are out of
 * goods and out of batteries, there is nothing more they can take from you" (p.11).
 *
 * <p>Reported rather than merely applied, because the amount demanded and the amount
 * surrendered are often different numbers, and a player watching a smuggler take two
 * goods from a ship carrying one deserves to see what happened.
 *
 * @param goods     the cubes handed over, most valuable first
 * @param batteries the charges handed over once the holds ran dry
 * @param unpaid    what could not be taken because the ship had nothing left
 */
public record Forfeit(List<GoodColor> goods, int batteries, int unpaid) {

    /**
     * Takes a defensive copy of the goods.
     *
     * @throws IllegalArgumentException if a count is negative
     */
    public Forfeit {
        goods = List.copyOf(goods);
        if (batteries < 0 || unpaid < 0) {
            throw new IllegalArgumentException("a forfeit cannot be negative");
        }
    }

    /**
     * Returns how much was actually taken.
     *
     * @return the number of goods plus charges surrendered
     */
    public int total() {
        return goods.size() + batteries;
    }

    /**
     * Tells whether the ship got off lightly because it had nothing left to lose.
     *
     * @return {@code true} when part of the demand could not be met
     */
    public boolean wasCappedByPoverty() {
        return unpaid > 0;
    }
}
