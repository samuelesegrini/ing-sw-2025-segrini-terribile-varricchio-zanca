package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.state.LocalGameState;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import java.util.Map;

/**
 * Event when a tile is placed on a ship.
 */
public class TilePlacedEvent extends AbstractEvent {
    private final String playerId;
    private final String playerNickname;
    private final String tileId;
    private final int row, col, rotation;
    private final ComponentType componentType;

    public TilePlacedEvent(String gameId, String playerId, String playerNickname,
                           String tileId, int row, int col, int rotation,
                           ComponentType componentType) {
        super(EventType.TILE_PLACED, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.tileId = tileId;
        this.row = row;
        this.col = col;
        this.rotation = rotation;
        this.componentType = componentType;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        LocalGameState gameState = context.getGameState();

        // Update local ship state only for local player
        if (context.isLocalPlayer(playerId)) {
            gameState.placeTile(playerId, tileId, row, col, rotation);
        }

        context.runOnUIThread(() -> {
            if (context.isLocalPlayer(playerId)) {
                // Our own action confirmed
                context.getController().getModel().firePropertyChange("tileConfirmed", false, true);
                context.getController().getModel().firePropertyChange("shipGridUpdated", null, gameState.getShipGrid());
                
                // Show success notification
                if (context.getNotificationService() != null) {
                    context.getNotificationService().showNotification(
                        new it.polimi.ingsw.client.ui.Notification(
                            "Tile Placed",
                            "Component successfully placed at (" + row + "," + col + ")",
                            it.polimi.ingsw.client.ui.NotificationType.SUCCESS
                        )
                    );
                }
            } else {
                // Opponent's action - show info notification
                if (context.getNotificationService() != null) {
                    context.getNotificationService().showNotification(
                        new it.polimi.ingsw.client.ui.Notification(
                            "Opponent Move",
                            playerNickname + " placed a " + componentType.name().toLowerCase().replace("_", " "),
                            it.polimi.ingsw.client.ui.NotificationType.INFO
                        )
                    );
                }
            }

            // Notify UI to refresh ship display
            context.getController().getModel().firePropertyChange("opponentShipUpdated", null, Map.of(
                "playerId", playerId,
                "component", componentType,
                "row", row,
                "col", col
            ));
        });
    }
}