package it.polimi.ingsw.server.model.component;


import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.Rotation;
import java.util.Set;

/**
 * A shield generator welded to a ship.
 *
 * <p>A shield covers two adjacent sides and stops small meteors and light cannon fire
 * arriving from them, at the cost of one battery charge each time. It is useless
 * against big meteors and heavy fire (manual p.7). Where it sits on the ship makes no
 * difference; only which way it faces does, which is why two shields turned to cover
 * all four sides is all anyone needs.
 *
 * @param tile     the printed piece
 * @param rotation how far it is turned
 */
public record ShieldComponent(Tile tile, Rotation rotation) implements ShipComponent {

    /**
     * Validates the kind.
     *
     * @throws IllegalArgumentException if the tile is not a shield
     */
    public ShieldComponent {
        Components.require(tile, ComponentKind.SHIELD);
    }

    /**
     * Returns the two sides this shield protects.
     *
     * @return the covered directions
     */
    public Set<Direction> covered() {
        return tile.shieldedSides(rotation);
    }

    /**
     * Tells whether this shield can stop something arriving from the given side.
     *
     * @param side the direction the threat comes from
     * @return {@code true} when that side is covered
     */
    public boolean covers(Direction side) {
        return covered().contains(side);
    }
}
