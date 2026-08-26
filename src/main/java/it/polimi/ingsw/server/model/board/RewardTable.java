package it.polimi.ingsw.server.model.board;

import it.polimi.ingsw.common.game.GoodColor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The credits printed on a flight board.
 *
 * @param finishOrder          credits for finishing first, second and so on
 * @param prettiestShip        credits for the fewest exposed connectors
 * @param lostComponentPenalty credits charged for each component in the discard pile
 * @param goodsPrices          what a cube of each colour sells for
 */
public record RewardTable(List<Integer> finishOrder,
                          int prettiestShip,
                          int lostComponentPenalty,
                          Map<GoodColor, Integer> goodsPrices) {

    /**
     * Validates the table and takes defensive copies.
     *
     * @throws IllegalArgumentException if a price is missing for any colour
     */
    public RewardTable {
        finishOrder = List.copyOf(finishOrder);
        Map<GoodColor, Integer> prices = new EnumMap<>(GoodColor.class);
        prices.putAll(goodsPrices);
        if (prices.size() != GoodColor.values().length) {
            throw new IllegalArgumentException("every good colour needs a price, got " + prices.keySet());
        }
        goodsPrices = Map.copyOf(prices);
    }

    /**
     * Returns the credits awarded for a finishing placement.
     *
     * <p>Only players who complete the flight collect this; anyone who gave up gets
     * nothing (manual p.20).
     *
     * @param placement the placement, counting from one
     * @return the credits awarded, or zero beyond the printed placements
     * @throws IllegalArgumentException if the placement is not positive
     */
    public int finishReward(int placement) {
        if (placement < 1) {
            throw new IllegalArgumentException("placements count from one, got " + placement);
        }
        return placement <= finishOrder.size() ? finishOrder.get(placement - 1) : 0;
    }

    /**
     * Returns what one cube of the given colour sells for.
     *
     * @param color the colour of the cube
     * @return the credits it fetches at full price
     */
    public int priceOf(GoodColor color) {
        return goodsPrices.get(color);
    }
}
