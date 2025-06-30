package it.polimi.ingsw.common.message.event;

// REMOVED: LocalGameState no longer needed
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Event when a tile is placed on a ship.
 */
public class TilePlacedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(TilePlacedEvent.class.getName());
    private final String playerId;
    private final String playerNickname;
    private final String tileId;
    private final int row, col, rotation;
    private final ComponentType componentType;

    /**
     * constructor
     *
     * @param gameId the game ID
     * @param playerId the player ID
     * @param playerNickname the player nickname
     * @param tileId the tile ID
     * @param row the row where to place the tile
     * @param col the column where to place the tile
     * @param rotation  the rotation how to place the tile
     * @param componentType the type of the component to place
     */

    public TilePlacedEvent(String gameId, String playerId, String playerNickname,
                           String tileId, int row, int col, int rotation,
                           ComponentType componentType) {
        super(EventType.TILE_PLACED, gameId, PlayerId.fromString(playerId));
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.tileId = tileId;
        this.row = row;
        this.col = col;
        this.rotation = rotation;
        this.componentType = componentType;
        LOGGER.fine("TilePlacedEvent instantiated for game: " + gameId + ", player: " + playerNickname + ", tile: " + tileId + " at (" + row + "," + col + ")");
    }

    /**
     *
     * @param clientState The client state to update
     */

    @Override
    public void updateClientState(it.polimi.ingsw.client.core.ClientState clientState) {
        // This is a legacy event - model updates are handled by ComponentPlacedEvent
        // Only increment version for consistency
        clientState.incrementStateVersion();
    }

    /**
     *
     * @param context The client event context
     */

    @Override
    public void handleOnClient(ClientEventContext context) {
        // First update client state
        updateClientState(context.getClientState());
        context.runOnUIThread(() -> {
            LOGGER.fine("Handling TilePlacedEvent for player: " + playerId + ", tile: " + tileId);
            // Show appropriate notifications
            if (context.isLocalPlayer(playerId)) {
                // Our own action confirmed - show success notification
                if (context.getNotificationService() != null) {
                    LOGGER.fine("Displaying 'Tile Placed' notification for local player.");
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
                    LOGGER.fine("Displaying 'Opponent Move' notification for tile placed by " + playerNickname);
                    context.getNotificationService().showNotification(
                        new it.polimi.ingsw.client.ui.Notification(
                            "Opponent Move",
                            playerNickname + " placed a " + componentType.name().toLowerCase().replace("_", " "),
                            it.polimi.ingsw.client.ui.NotificationType.INFO
                        )
                    );
                }
            }
            
            // NOTE: Model updates and UI refresh should be handled by newer events like ComponentPlacedEvent
            // that carry full server models. This legacy event only handles notifications.
        });
    }
}