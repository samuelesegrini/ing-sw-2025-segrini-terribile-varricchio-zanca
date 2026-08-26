package it.polimi.ingsw.common.protocol.view;

import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.Rotation;

import java.io.Serializable;
import java.util.Map;

/**
 * One component tile, as a view needs it.
 *
 * <p>The connectors are given <em>as they face</em>, with the rotation already applied, so
 * that a view can draw the ship without knowing how a tile turns. The rotation is sent as
 * well, because the artwork still has to be turned by that much.
 *
 * <p>Sending both the kind and the connectors makes the message self-describing: a client
 * with none of the game's data files could still draw a readable schematic of a ship. That
 * costs about sixty bytes a tile and at most twenty-four tiles a ship, which is not a
 * number worth optimising.
 *
 * @param tileId     what the tile is called, and the key to its artwork
 * @param kind       what it does
 * @param rotation   how far it has been turned from the printed orientation
 * @param connectors what each side offers, after the rotation
 */
public record TileView(String tileId, ComponentKind kind, Rotation rotation,
                       Map<Direction, Connector> connectors) implements Serializable {

    /**
     * Takes a defensive copy of the connectors.
     *
     * @throws NullPointerException if any part is {@code null}
     */
    public TileView {
        if (tileId == null || kind == null || rotation == null) {
            throw new NullPointerException("a tile view needs an id, a kind and a rotation");
        }
        connectors = Map.copyOf(connectors);
    }
}
