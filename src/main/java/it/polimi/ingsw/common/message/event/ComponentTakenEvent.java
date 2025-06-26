package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;

import java.util.logging.Logger;

/**
 * Event broadcast when a component tile is taken from the face-down pile by a player.
 * This moves the component directly to the player's hand, not to a reservation state.
 * 
 * ENHANCED VERSION: Carries full server models instead of just basic data.
 */
public class ComponentTakenEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(ComponentTakenEvent.class.getName());
    private final Component component;      // Full Component model
    private final Player player;           // Full Player model  
    private final ComponentDeck updatedDeck; // Full deck state

    public ComponentTakenEvent(String gameId, Component component, 
                              Player player, ComponentDeck updatedDeck) {
        super(EventType.COMPONENT_TAKEN, gameId, player.getId());
        this.component = component;
        this.player = player;
        this.updatedDeck = updatedDeck;
        LOGGER.fine("ComponentTakenEvent instantiated for game: " + gameId + ", player: " + player.getNickname() + ", component: " + component.getType());
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

    /**
     * Gets the player ID.
     */
    public PlayerId getPlayerId() {
        return player.getId();
    }
    

    public String getPlayerNickname() {
        return player.getNickname();
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Don't send to the requesting client (they get the response instead)
        PlayerId clientPlayerId = context.getPlayerIdForClient(clientId);
        if (player.getId().equals(clientPlayerId)) {
            LOGGER.finer("EVENT FILTERING - ComponentTakenEvent NOT sent to requesting client: " + clientId);
            return false; // Exclude the requesting client
        }
        
        // Use default game filtering for other clients
        return super.shouldSendTo(clientId, context);
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
                    LOGGER.fine("Displaying 'Component Taken' notification for local player: " + message);
                } else {
                    message = String.format("%s took a tile from the pile", getPlayerNickname());
                    LOGGER.fine("Displaying 'Component Taken' notification for other player: " + message);
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