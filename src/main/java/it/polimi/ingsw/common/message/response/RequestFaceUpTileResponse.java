package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Response to request face-up tile request.
 * Contains the tile that was taken or error information if the tile was not available.
 */
public class RequestFaceUpTileResponse extends AbstractResponse {

    private final Tile tile;

    public RequestFaceUpTileResponse(UUID correlationId, Tile tile) {
        super(correlationId);
        this.tile = tile;
    }

    public Tile getTile() {
        return tile;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess() && tile != null) {
            // Update model state - player now holds this tile
            if (context.getClientState() != null) {
                // context.getClientState().setHeldTile(tile);
            }
            
            // Show success notification
            context.showNotification(
                    "Tile Taken",
                    "You took a " + tile.getType() + " tile",
                    NotificationType.INFO
            );
        } else {
            // Show error notification
            context.showNotification(
                    "Tile Unavailable",
                    getErrorMessage() != null ? getErrorMessage() : "The requested tile is no longer available",
                    NotificationType.WARNING
            );
        }

        context.getController().getUI().onRequestFaceUpTileResponse(this);
    }

    /**
     * Represents a component tile in the game.
     * This is a simplified version - the actual implementation may be more complex.
     */
    public static class Tile {
        private final String tileId;
        private final String type;
        private final int connectors;
        private final int rotation;

        public Tile(String tileId, String type, int connectors, int rotation) {
            this.tileId = tileId;
            this.type = type;
            this.connectors = connectors;
            this.rotation = rotation;
        }

        public String getTileId() {
            return tileId;
        }

        public String getType() {
            return type;
        }

        public int getConnectors() {
            return connectors;
        }

        public int getRotation() {
            return rotation;
        }
    }
}