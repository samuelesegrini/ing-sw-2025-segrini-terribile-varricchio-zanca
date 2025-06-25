package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;

/**
 * Event broadcast when a component tile is taken from the face-down pile by a player.
 * This moves the component directly to the player's hand, not to a reservation state.
 * 
 * ENHANCED VERSION: Carries full server models instead of just basic data.
 */
public class ComponentTakenEvent extends AbstractEvent {
    private final Component component;      // Full Component model
    private final Player player;           // Full Player model  
    private final ComponentDeck updatedDeck; // Full deck state

    public ComponentTakenEvent(String gameId, Component component, 
                              Player player, ComponentDeck updatedDeck) {
        super(EventType.COMPONENT_TAKEN, gameId, player.getId().toString());
        this.component = component;
        this.player = player;
        this.updatedDeck = updatedDeck;
    }

    public Component getComponent() {
        return component;
    }

    public Player getPlayer() {
        return player;
    }
    
    public ComponentDeck getUpdatedDeck() {
        return updatedDeck;
    }

    // Legacy getters for backward compatibility
    public String getPlayerId() {
        return player.getId().toString();
    }

    public String getPlayerNickname() {
        return player.getNickname();
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // SIMPLIFIED: Direct model updates via ClientState
            context.getClientState().updatePlayer(player);
            context.getClientState().updateComponentDeck(updatedDeck);
            // UI refreshes automatically via ClientState.refreshCurrentView()

            // Show notification - different message than reservation
            if (context.getNotificationService() != null) {
                String message;
                if (context.isLocalPlayer(getPlayerId())) {
                    message = String.format("You took a %s tile from the pile", component.getType().name());
                } else {
                    message = String.format("%s took a tile from the pile", getPlayerNickname());
                }
                context.getNotificationService().showNotification(
                    new it.polimi.ingsw.client.ui.Notification(
                        "Component Taken",
                        message,
                        it.polimi.ingsw.client.ui.NotificationType.INFO
                    )
                );
            }

        });
    }
}