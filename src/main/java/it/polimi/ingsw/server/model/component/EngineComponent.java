package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.Rotation;


/**
 * An engine welded to a ship.
 *
 * <p>Unlike a cannon, an engine has only one legal facing: its exhaust must point at
 * the stern, and nothing may sit in the cell behind it (manual p.6). Both checks live
 * in the ship validator; this type just reports where the exhaust points.
 *
 * @param tile     the printed piece
 * @param rotation how far it is turned
 */
public record EngineComponent(Tile tile, Rotation rotation) implements ShipComponent {

    /**
     * Validates the kind.
     *
     * @throws IllegalArgumentException if the tile is not an engine
     */
    public EngineComponent {
        Components.require(tile, ComponentKind.SINGLE_ENGINE, ComponentKind.DOUBLE_ENGINE);
    }

    /**
     * Returns where this engine's exhaust points.
     *
     * @return the direction the exhaust faces
     */
    public Direction exhaustDirection() {
        return tile.exhaustDirection(rotation);
    }

    /**
     * Tells whether firing this engine costs a battery charge.
     *
     * @return {@code true} for a double engine
     */
    public boolean needsCharge() {
        return kind() == ComponentKind.DOUBLE_ENGINE;
    }

    /**
     * Returns what this engine contributes to engine power.
     *
     * <p>A single engine is worth one and is always counted. A double is worth two, but
     * only when a battery is spent on it (manual p.11).
     *
     * @param powered whether a battery charge is being spent on this engine; ignored
     *                for single engines, which never need one
     * @return the contribution to engine power
     */
    public int power(boolean powered) {
        if (needsCharge()) {
            return powered ? 2 : 0;
        }
        return 1;
    }
}
