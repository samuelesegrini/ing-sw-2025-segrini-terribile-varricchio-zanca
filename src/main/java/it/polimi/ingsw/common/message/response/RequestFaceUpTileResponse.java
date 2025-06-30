package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.io.Serializable;
import java.util.UUID;

/**
 * Response to request face-up tile request.
 * Contains the tile that was taken or error information if the tile was not available.
 */
public class RequestFaceUpTileResponse extends AbstractResponse {

    private final Tile tile;

    /**
     * constructor
     *
     * @param correlationId The correlation ID
     * @param tile The tile requested
     */

    public RequestFaceUpTileResponse(UUID correlationId, Tile tile) {
        super(correlationId);
        this.tile = tile;
    }

    /**
     *
     * @return The tile requested
     */

    public Tile getTile() {
        return tile;
    }

    /**
     *
     * @param context The client context
     */

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
    public static class Tile implements Serializable {
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

        /**
         *
         * @return The requested tile ID
         */

        public String getTileId() {
            return tileId;
        }

        /**
         *
         * @return the type of the requested tile
         */

        public String getType() {
            return type;
        }

        /**
         *
         * @return the number of connectors of the requested tile
         */

        public int getConnectors() {
            return connectors;
        }

        /**
         *
         * @return the rotation of the requested tile
         */

        public int getRotation() {
            return rotation;
        }
    }
}