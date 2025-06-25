// in package it.polimi.ingsw.common.message.event
package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.common.ComponentData;

/**
 * Broadcast when a player returns a tile to the communal pile, face-up.
 */
public class TileReturnedEvent extends AbstractEvent {
    private final ComponentData componentData;

    public TileReturnedEvent(String gameId, ComponentData componentData) {
        super(EventType.TILE_RETURNED, gameId, null);
        this.componentData = componentData;
    }
    

    public ComponentData getComponentData() {
        return componentData;
    }


    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update local game state to reflect returned tile
            if (context.getGameState() != null) {
                // Create ComponentInstance from complete server data
                if (componentData.getConnectors() != null) {
                    context.getGameState().addFaceUpJunkyardTile(
                        componentData.getId(),
                        componentData.getType(),
                        componentData.getConnectors()
                    );
                }
                
                // Remove from held tiles if this was held by local player
                context.getGameState().removeHeldTileById(componentData.getId());
            }

            // Show notification about returned tile
            if (context.getNotificationService() != null) {
                String message = String.format("A %s tile was returned to the face-up pile", componentData.getType().toString().toLowerCase());
                context.getNotificationService().showNotification(
                    new it.polimi.ingsw.client.ui.Notification(
                        "Tile Returned",
                        message,
                        it.polimi.ingsw.client.ui.NotificationType.INFO
                    )
                );
            }

            // Fire property change events for UI updates
            if (context.getController() != null && context.getController().getModel() != null) {
                // Notify that face-up tiles have been updated
                context.getController().getModel().firePropertyChange("faceUpTilesUpdated", null, 
                    context.getGameState().getAvailableTiles());
                
                // Notify that held tiles may have been updated
                context.getController().getModel().firePropertyChange("heldTilesUpdated", null, 
                    context.getGameState().getHeldTiles());
                
                // General tile availability update
                context.getController().getModel().firePropertyChange("tileReturned", null, 
                    java.util.Map.of(
                        "tileId", componentData.getId(),
                        "tileType", componentData.getType().toString()
                    )
                );
            }
        });
    }
}