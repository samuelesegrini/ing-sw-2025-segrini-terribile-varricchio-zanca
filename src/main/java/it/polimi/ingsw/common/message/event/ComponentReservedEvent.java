package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.client.core.state.ComponentInstance;
import it.polimi.ingsw.common.ComponentData;

/**
 * Event broadcast when a component tile is reserved by a player.
 * This happens in advanced building rules where players can reserve components for later use.
 */
public class ComponentReservedEvent extends AbstractEvent {
    private final ComponentData componentData;
    private final String playerId;
    private final String playerNickname;
    private final long reservationExpiresAt;

    public ComponentReservedEvent(String gameId, ComponentData componentData, 
                                 String playerId, String playerNickname, long reservationExpiresAt) {
        super(EventType.COMPONENT_RESERVED, gameId, playerId);
        this.componentData = componentData;
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.reservationExpiresAt = reservationExpiresAt;
    }
    

    public ComponentData getComponentData() {
        return componentData;
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
                if (componentData != null && componentData.getConnectors() != null) {
                    // Create ComponentInstance from complete server data
                    ComponentInstance component = new ComponentInstance(
                        componentData.getId(),
                        componentData.getType(),
                        componentData.getConnectors()
                    );
                    component.setDirection(componentData.getDefaultDirection());
                    
                    it.polimi.ingsw.client.core.state.LocalGameState.getInstance().addHeldTile(component);
                    context.getController().getModel().firePropertyChange("heldTiles", null, null);
                } else {
                    System.err.println("ComponentReservedEvent: Invalid componentData or null connectors");
                }
            }

            // Show notification
            if (context.getNotificationService() != null) {
                String message;
                if (context.isLocalPlayer(playerId)) {
                    long timeLeft = (reservationExpiresAt - System.currentTimeMillis()) / 1000;
                    message = String.format("You reserved a %s tile (%d seconds to use)", componentData.getType().name(), timeLeft);
                } else {
                    message = String.format("%s reserved a %s tile", playerNickname, componentData.getType().name());
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
                        "tileId", componentData.getId(),
                        "tileType", componentData.getType().name(),
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