package it.polimi.ingsw.server.model.goods;

import it.polimi.ingsw.common.game.GoodColor;
import java.util.EnumMap;
import java.util.Map;

/**
 * The pile of trade goods in the middle of the table.
 *
 * <p>Finite, which is the whole reason the shortage rule exists: on a busy flight there
 * may not be enough cubes to go round, and players then load in route order, first come
 * first served (manual p.19). A cube thrown overboard by a ship ahead goes back here and
 * becomes available to one behind — which is why jettisoning is a decision that affects
 * other people.
 *
 * <p>How many cubes the physical game ships with is not stated in either rulebook; the
 * stock is data rather than a constant, and the shipped numbers are an assumption
 * recorded as decision D8 in the specification.
 */
public final class GoodsBank {

    private final Map<GoodColor, Integer> stock = new EnumMap<>(GoodColor.class);

    /**
     * Creates a bank holding the given cubes.
     *
     * @param stock how many cubes of each colour are available
     * @throws IllegalArgumentException if any count is negative
     */
    public GoodsBank(Map<GoodColor, Integer> stock) {
        stock.forEach((color, count) -> {
            if (count < 0) {
                throw new IllegalArgumentException("the bank cannot hold " + count + " " + color + " cubes");
            }
            this.stock.put(color, count);
        });
        for (GoodColor color : GoodColor.values()) {
            this.stock.putIfAbsent(color, 0);
        }
    }

    /**
     * Returns how many cubes of a colour are left.
     *
     * @param color the colour to count
     * @return the cubes available
     */
    public int available(GoodColor color) {
        return stock.get(color);
    }

    /**
     * Tells whether any cube of a colour is left.
     *
     * @param color the colour to look for
     * @return {@code true} when at least one is available
     */
    public boolean has(GoodColor color) {
        return available(color) > 0;
    }

    /**
     * Takes one cube out.
     *
     * <p>Returns whether it worked rather than failing. Running out is a rule, not an
     * error: a player who finds nothing left to load still pays the flight days (manual
     * p.19), so the caller has to carry on either way.
     *
     * @param color the colour wanted
     * @return {@code true} when a cube was handed over
     */
    public boolean take(GoodColor color) {
        if (!has(color)) {
            return false;
        }
        stock.merge(color, -1, Integer::sum);
        return true;
    }

    /**
     * Puts a cube back.
     *
     * <p>Where every cube goes when it is sold, jettisoned, taken by an enemy, or lost
     * with the hold carrying it.
     *
     * @param color the colour being returned
     */
    public void giveBack(GoodColor color) {
        stock.merge(color, 1, Integer::sum);
    }

    /**
     * Puts several cubes back.
     *
     * @param colors the cubes being returned
     */
    public void giveBackAll(Iterable<GoodColor> colors) {
        colors.forEach(this::giveBack);
    }

    /**
     * Returns what the bank currently holds.
     *
     * @return an immutable snapshot of the stock
     */
    public Map<GoodColor, Integer> stock() {
        return Map.copyOf(stock);
    }
}
