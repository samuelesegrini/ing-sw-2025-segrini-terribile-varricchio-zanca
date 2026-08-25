package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.server.model.crew.AlienColor;
import it.polimi.ingsw.server.model.ship.Rotation;

/**
 * A life support module welded to a ship.
 *
 * <p>It does nothing on its own. Attached to a cabin, it makes that cabin habitable by
 * an alien of its colour — and attachment means a real connector joint, not merely
 * sitting next door (manual p.18). Lose the module and the alien leaves.
 *
 * @param tile     the printed piece
 * @param rotation how far it is turned
 */
public record LifeSupportComponent(Tile tile, Rotation rotation) implements ShipComponent {

    /**
     * Validates the kind.
     *
     * @throws IllegalArgumentException if the tile is not a life support module
     */
    public LifeSupportComponent {
        Components.require(tile, ComponentKind.PURPLE_LIFE_SUPPORT, ComponentKind.BROWN_LIFE_SUPPORT);
    }

    /**
     * Returns the species this module keeps alive.
     *
     * @return the supported alien colour
     */
    public AlienColor supports() {
        return AlienColor.supportedBy(kind());
    }
}
