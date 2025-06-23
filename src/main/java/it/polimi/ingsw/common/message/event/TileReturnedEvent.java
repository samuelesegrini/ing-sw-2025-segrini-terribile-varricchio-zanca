// in package it.polimi.ingsw.common.message.event
package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.enums.ship.ComponentType;

/**
 * Broadcast when a player returns a tile to the communal pile, face-up.
 */
public class TileReturnedEvent extends AbstractEvent {
    private final String tileId;
    private final ComponentType tileType;

    public TileReturnedEvent(String gameId, String tileId, ComponentType tileType) {
        super(EventType.TILE_RETURNED, gameId, null);
        this.tileId = tileId;
        this.tileType = tileType;
    }

    public String getTileId() {
        return tileId;
    }

    public ComponentType getTileType() {
        return tileType;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        // Client UI can now show this tile as available in the face-up pile.
    }
}