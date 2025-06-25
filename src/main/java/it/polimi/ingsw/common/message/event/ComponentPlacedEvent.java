package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;

/**
 * Event when a component is placed on a ship.
 * ENHANCED VERSION: Carries full server models instead of basic data.
 */
public class ComponentPlacedEvent extends AbstractEvent {
    private final Player player;           // Full Player model
    private final Component component;     // Full Component model
    private final Ship updatedShip;       // Updated Ship model
    private final ComponentDeck updatedDeck; // Updated ComponentDeck model

    public ComponentPlacedEvent(String gameId, Player player, Component component, 
                               Ship updatedShip, ComponentDeck updatedDeck) {
        super(EventType.TILE_PLACED, gameId, player.getId());
        this.player = player;
        this.component = component;
        this.updatedShip = updatedShip;
        this.updatedDeck = updatedDeck;
    }

    public Player getPlayer() {
        return player;
    }

    public Component getComponent() {
        return component;
    }

    public Ship getUpdatedShip() {
        return updatedShip;
    }

    public ComponentDeck getUpdatedDeck() {
        return updatedDeck;
    }

    // Legacy getters for backward compatibility
    public String getPlayerId() {
        return player.getId();
    }

    public String getPlayerNickname() {
        return player.getNickname();
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // SIMPLIFIED: Direct model updates instead of complex conversion
            
            // NEW: Simple direct model update via ClientState
            if (context.getClientState() != null) {
                // Update the player in game model
                context.getClientState().updatePlayer(player);
                // Update the component deck state  
                context.getClientState().updateComponentDeck(updatedDeck);
                // UI refreshes automatically via ClientState.refreshCurrentView()
            } else {
                // LEGACY: Fallback to LocalGameState for backward compatibility
                if (context.isLocalPlayer(getPlayerId())) {
                    // Note: This relies on the old placement method which should be replaced
                    context.getGameState().placeTile(getPlayerId(), component.getId(), 
                        component.getPosition().getRow(), component.getPosition().getCol(), 0);
                }
            }

            // Show appropriate notification
            if (context.getNotificationService() != null) {
                if (context.isLocalPlayer(getPlayerId())) {
                    context.getNotificationService().showNotification(
                        new it.polimi.ingsw.client.ui.Notification(
                            "Component Placed",
                            "Component successfully placed on your ship",
                            it.polimi.ingsw.client.ui.NotificationType.SUCCESS
                        )
                    );
                } else {
                    context.getNotificationService().showNotification(
                        new it.polimi.ingsw.client.ui.Notification(
                            "Opponent Move",
                            getPlayerNickname() + " placed a " + component.getType().name().toLowerCase().replace("_", " "),
                            it.polimi.ingsw.client.ui.NotificationType.INFO
                        )
                    );
                }
            }

            // SIMPLIFIED: Single property change event instead of multiple
            if (context.getController() != null && context.getController().getModel() != null) {
                context.getController().getModel().firePropertyChange("componentPlaced", null, 
                    java.util.Map.of(
                        "playerId", getPlayerId(),
                        "component", component.getType(),
                        "position", component.getPosition()
                    )
                );
            }
        });
    }
}