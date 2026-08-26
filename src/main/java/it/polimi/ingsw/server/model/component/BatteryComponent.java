package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Rotation;


/**
 * A battery compartment welded to a ship.
 *
 * <p>Holds the two or three charges printed on the tile. A charge is spent every time
 * a double cannon, a double engine or a shield is used, and once spent it is gone for
 * the rest of the flight — there is no recharging (manual p.7).
 *
 * <p>Compartments are filled to capacity during launch preparation, not at placement:
 * a ship under construction has no charges in it.
 */
public final class BatteryComponent implements ShipComponent {

    private final Tile tile;
    private final Rotation rotation;
    private int charges;

    /**
     * Wraps a battery tile, empty until the ship is prepared for launch.
     *
     * @param tile     the printed piece
     * @param rotation how far it is turned
     * @throws IllegalArgumentException if the tile is not a battery
     */
    public BatteryComponent(Tile tile, Rotation rotation) {
        Components.require(tile, ComponentKind.BATTERY);
        this.tile = tile;
        this.rotation = rotation;
    }

    @Override
    public Tile tile() {
        return tile;
    }

    @Override
    public Rotation rotation() {
        return rotation;
    }

    /**
     * Returns how many charges this compartment holds when full.
     *
     * @return two or three, as printed on the tile
     */
    public int capacity() {
        return tile.capacity();
    }

    /**
     * Returns how many charges are left.
     *
     * @return the remaining charges
     */
    public int charges() {
        return charges;
    }

    /**
     * Tells whether this compartment has anything left to give.
     *
     * @return {@code true} when no charges remain
     */
    public boolean isEmpty() {
        return charges == 0;
    }

    /**
     * Fills the compartment to its printed capacity.
     *
     * <p>Done once, during launch preparation (manual p.9).
     */
    public void fill() {
        charges = capacity();
    }

    /**
     * Spends one charge.
     *
     * @throws IllegalStateException if the compartment is already empty
     */
    public void spend() {
        if (isEmpty()) {
            throw new IllegalStateException(id() + " has no charges left to spend");
        }
        charges--;
    }

    /**
     * Empties the compartment.
     *
     * <p>Used when the component is destroyed: tokens on a lost piece go straight back
     * to the bank (manual p.10).
     */
    public void drain() {
        charges = 0;
    }
}
