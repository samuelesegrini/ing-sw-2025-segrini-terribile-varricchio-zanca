package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;

import java.util.logging.Logger;

/**
 * Event broadcast when a component tile is reserved by a player.
 * This moves the component to the player's reservation area.
 * 
 * ENHANCED VERSION: Carries full server models instead of just basic data.
 */
public class ComponentReservedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(ComponentReservedEvent.class.getName());
    private final Component component;      // Full Component model
    private final Player player;           // Full Player model  
    private final ComponentDeck updatedDeck; // Full deck state

    public ComponentReservedEvent(String gameId, Component component, 
                                 Player player, ComponentDeck updatedDeck) {
        super(EventType.COMPONENT_RESERVED, gameId, player.getId());
        this.component = component;
        this.player = player;
        this.updatedDeck = updatedDeck;
        LOGGER.fine("ComponentReservedEvent instantiated for game: " + gameId + ", player: " + player.getNickname() + ", component: " + component.getType());
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
        LOGGER.finer("EVENT FILTERING - ComponentReservedEvent shouldSendTo clientId: " + clientId + " = " + shouldSend + " (including requester)");
        return shouldSend;
    }

    @Override
    public void updateClientState(it.polimi.ingsw.client.core.ClientState clientState) {
        // Event updates client state with complete models
        clientState.updatePlayer(player);
        clientState.updateComponentDeck(updatedDeck);
        clientState.addReservedComponent(player.getId(), component);
        clientState.incrementStateVersion();
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        // First update client state
        updateClientState(context.getClientState());
        
        // Then handle UI updates
        context.runOnUIThread(() -> {
            boolean isLocalPlayer = context.isLocalPlayer(getPlayerId());
            
            // Direct UI notification via newUI
            context.getNewUI().onComponentReservedEvent(this);
            
            // Show notification
            if (context.getNotificationService() != null) {
                String message;
                if (isLocalPlayer) {
                    message = String.format("You reserved a %s component", component.getType().name());
                } else {
                    message = String.format("%s reserved a component", getPlayerNickname());
                }
                context.getNotificationService().showNotification(
                    new it.polimi.ingsw.client.ui.Notification(
                        "Component Reserved",
                        message,
                        it.polimi.ingsw.client.ui.NotificationType.INFO
                    )
                );
            }
        });
    }
}