package it.polimi.ingsw.common.message.event;

// REMOVED: LocalGameState no longer needed
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;

/**
 * Event broadcast when a ship component is damaged or destroyed.
 * Updates the game state and provides visual/audio feedback to players.
 */
public class ShipDamagedEvent extends AbstractEvent {
    private final String playerId;
    private final String playerNickname;
    private final int row, col;
    private final String damageSource;
    private final ComponentType componentLost;

    public ShipDamagedEvent(String gameId, String playerId, String playerNickname,
                            int row, int col, String damageSource, ComponentType componentLost) {
        super(EventType.SHIP_DAMAGED, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.row = row;
        this.col = col;
        this.damageSource = damageSource;
        this.componentLost = componentLost;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public String getDamageSource() {
        return damageSource;
    }

    public ComponentType getComponentLost() {
        return componentLost;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        // Update game state with damage
        var clientState = context.getClientState();
        if (clientState != null) {
            // Fire property change for ship damage
            clientState.firePropertyChange("shipDamaged", null, 
                java.util.Map.of("playerId", playerId, "row", row, "col", col, 
                               "damageSource", damageSource, "componentLost", componentLost));
        }

        context.runOnUIThread(() -> {
            // Show notification about the damage
            if (context.getNotificationService() != null) {
                String message = context.isLocalPlayer(playerId)
                        ? "Your " + (componentLost != null ? componentLost.name().toLowerCase() : "component") + " was destroyed by " + damageSource
                        : playerNickname + " lost a " + (componentLost != null ? componentLost.name().toLowerCase() : "component");

                context.getNotificationService().showNotification(new Notification(
                        "Ship Damaged!",
                        message,
                        context.isLocalPlayer(playerId) ? NotificationType.WARNING : NotificationType.INFO
                ));
            }

            // Update UI if available
            // if (context.getGameUI() != null) {
            //     // Note: These would need to be implemented based on the actual UI interface
            //     // context.getGameUI().showDamageAnimation(playerId, row, col, damageSource);
            //     // context.getGameUI().refreshShipDisplay(playerId);
            //     // context.getGameUI().updateShipStats(playerId);
            // }
        });
    }
}