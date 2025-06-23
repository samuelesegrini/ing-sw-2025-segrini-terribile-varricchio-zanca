package it.polimi.ingsw.common.message.event;


/**
 * Event broadcast when a component tile is reserved by a player.
 * This happens in advanced building rules where players can reserve components for later use.
 */
public class ComponentReservedEvent extends AbstractEvent {
    private final String tileId;
    private final String tileType;
    private final String playerId;
    private final String playerNickname;
    private final long reservationExpiresAt;

    public ComponentReservedEvent(String gameId, String tileId, String tileType, 
                                 String playerId, String playerNickname, long reservationExpiresAt) {
        super(EventType.COMPONENT_RESERVED, gameId, playerId);
        this.tileId = tileId;
        this.tileType = tileType;
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.reservationExpiresAt = reservationExpiresAt;
    }

    public String getTileId() {
        return tileId;
    }

    public String getTileType() {
        return tileType;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public long getReservationExpiresAt() {
        return reservationExpiresAt;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update game state with component reservation
            if (context.isLocalPlayer(playerId)) {
                it.polimi.ingsw.client.core.state.LocalGameState.getInstance().addHeldTile(
                    it.polimi.ingsw.server.model.enums.ship.ComponentType.valueOf(tileType)
                );
                context.getController().getModel().firePropertyChange("heldTiles", null, null);
            }

            // Show notification
            if (context.getNotificationService() != null) {
                String message;
                if (context.isLocalPlayer(playerId)) {
                    long timeLeft = (reservationExpiresAt - System.currentTimeMillis()) / 1000;
                    message = String.format("You reserved a %s tile (%d seconds to use)", tileType, timeLeft);
                } else {
                    message = String.format("%s reserved a %s tile", playerNickname, tileType);
                }
                context.getNotificationService().showNotification(
                    new it.polimi.ingsw.client.ui.Notification(
                        "Component Reserved",
                        message,
                        it.polimi.ingsw.client.ui.NotificationType.INFO
                    )
                );
            }

            // Fire property change events for UI updates
            if (context.getController() != null && context.getController().getModel() != null) {
                context.getController().getModel().firePropertyChange("componentReserved", null, 
                    java.util.Map.of(
                        "tileId", tileId,
                        "tileType", tileType,
                        "playerId", playerId,
                        "expiresAt", reservationExpiresAt
                    )
                );
                
                if (context.isLocalPlayer(playerId)) {
                    context.getController().getModel().firePropertyChange("heldTilesUpdated", null, 
                        it.polimi.ingsw.client.core.state.LocalGameState.getInstance().getHeldTiles());
                }
            }
        });
    }
}