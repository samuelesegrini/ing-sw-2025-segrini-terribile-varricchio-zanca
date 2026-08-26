package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Rotation;


/**
 * A component that does nothing but hold the ship together.
 *
 * <p>Structural modules carry plenty of connectors, many of them universal, which is
 * what makes them useful: they keep a ship from falling apart the first time it is hit
 * (manual p.7).
 *
 * @param tile     the printed piece
 * @param rotation how far it is turned
 */
public record StructuralComponent(Tile tile, Rotation rotation) implements ShipComponent {

    /**
     * Validates the kind.
     *
     * @throws IllegalArgumentException if the tile is not a structural module
     */
    public StructuralComponent {
        Components.require(tile, ComponentKind.STRUCTURAL_MODULE);
    }
}
