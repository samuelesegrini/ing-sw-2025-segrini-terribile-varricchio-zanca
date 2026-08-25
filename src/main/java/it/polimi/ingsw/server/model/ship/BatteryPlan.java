package it.polimi.ingsw.server.model.ship;

import java.util.Set;

/**
 * Which of a ship's double cannons and double engines the player is paying to run.
 *
 * <p>Every time a ship declares firepower or engine power, its owner decides which
 * doubles to power, and each one costs a battery charge (manual p.11). Singles and
 * aliens are not in the plan because they are never optional: a player may not declare
 * less than they have (manual p.19).
 *
 * @param powered the cells holding the components to run
 */
public record BatteryPlan(Set<Position> powered) {

    /**
     * Takes a defensive copy of the chosen cells.
     *
     * @throws NullPointerException if the set is {@code null}
     */
    public BatteryPlan {
        powered = Set.copyOf(powered);
    }

    /**
     * Returns a plan that spends nothing.
     *
     * @return the empty plan
     */
    public static BatteryPlan none() {
        return new BatteryPlan(Set.of());
    }

    /**
     * Returns a plan powering the given cells.
     *
     * @param cells the components to run
     * @return the plan
     */
    public static BatteryPlan powering(Position... cells) {
        return new BatteryPlan(Set.of(cells));
    }

    /**
     * Returns how many charges this plan costs.
     *
     * @return one per component powered
     */
    public int cost() {
        return powered.size();
    }

    /**
     * Tells whether a component is being powered.
     *
     * @param cell the cell to ask about
     * @return {@code true} when the plan pays for that component
     */
    public boolean powers(Position cell) {
        return powered.contains(cell);
    }
}
