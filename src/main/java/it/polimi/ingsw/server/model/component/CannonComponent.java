package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.server.model.ship.Direction;
import it.polimi.ingsw.server.model.ship.Rotation;

/**
 * A cannon welded to a ship.
 *
 * <p>Firepower is counted in halves rather than in decimals. Manual p.11 is explicit
 * that 5½ beats 5 and loses to 6, and a side-facing single cannon is worth exactly a
 * half, so rounding anywhere would change who takes the cannon fire.
 *
 * @param tile     the printed piece
 * @param rotation how far it is turned
 */
public record CannonComponent(Tile tile, Rotation rotation) implements ShipComponent {

    /**
     * Validates the kind.
     *
     * @throws IllegalArgumentException if the tile is not a cannon
     */
    public CannonComponent {
        Components.require(tile, ComponentKind.SINGLE_CANNON, ComponentKind.DOUBLE_CANNON);
    }

    /**
     * Returns where this cannon points.
     *
     * @return the direction the muzzle faces
     */
    public Direction muzzleDirection() {
        return tile.muzzleDirection(rotation);
    }

    /**
     * Tells whether firing this cannon costs a battery charge.
     *
     * @return {@code true} for a double cannon
     */
    public boolean needsCharge() {
        return kind() == ComponentKind.DOUBLE_CANNON;
    }

    /**
     * Tells whether this cannon counts at full value.
     *
     * <p>Only cannons pointing at the bow do; every other facing is halved (manual
     * p.11).
     *
     * @return {@code true} when the muzzle faces north
     */
    public boolean facesForward() {
        return muzzleDirection() == Direction.NORTH;
    }

    /**
     * Returns what this cannon contributes to firepower, counted in halves.
     *
     * <p>A single cannon is worth 2 halves forward and 1 half otherwise. A double is
     * worth 4 and 2, but only when a battery is spent on it; unpowered it contributes
     * nothing. A single cannon is always counted — a player cannot choose to declare
     * less than they have (manual p.19).
     *
     * @param powered whether a battery charge is being spent on this cannon; ignored
     *                for single cannons, which never need one
     * @return the contribution in halves of a firepower point
     */
    public int firepowerHalves(boolean powered) {
        if (needsCharge() && !powered) {
            return 0;
        }
        int base = needsCharge() ? 4 : 2;
        return facesForward() ? base : base / 2;
    }
}
