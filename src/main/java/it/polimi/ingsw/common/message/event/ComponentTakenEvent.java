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
        // Send to ALL players in the game, including the requester (single source of truth)
        boolean shouldSend = super.shouldSendTo(clientId, context);
        LOGGER.finer("EVENT FILTERING - ComponentTakenEvent shouldSendTo clientId: " + clientId + " = " + shouldSend + " (including requester)");
        return shouldSend;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update state for ALL players - this is the single source of truth
            boolean isLocalPlayer = context.isLocalPlayer(getPlayerId());
            
            LOGGER.fine("ComponentTakenEvent: Updating state for player: " + getPlayerNickname() + 
                       " (local: " + isLocalPlayer + ")");
            context.getClientState().updatePlayer(player);
            context.getClientState().updateComponentDeck(updatedDeck);
            
            // Trigger UI refresh to show the updated state
            context.getClientState().refreshCurrentViewOnly();

            // Show notification for all players
            if (context.getNotificationService() != null) {
                String message;
                if (isLocalPlayer) {
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