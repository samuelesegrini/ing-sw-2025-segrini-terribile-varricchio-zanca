package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;

import java.util.logging.Logger;

/**
 * Event when a component is placed on a ship.
 * ENHANCED VERSION: Carries full server models instead of basic data.
 */
public class ComponentPlacedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(ComponentPlacedEvent.class.getName());
    private final Player player;           // Full Player model
    private final Component component;     // Full Component model
    private final Ship updatedShip;       // Updated Ship model
    private final ComponentDeck updatedDeck; // Updated ComponentDeck model

    public ComponentPlacedEvent(String gameId, Player player, Component component, 
                               Ship updatedShip, ComponentDeck updatedDeck) {
        super(EventType.COMPONENT_PLACED, gameId, player.getId());
        this.player = player;
        this.component = component;
        this.updatedShip = updatedShip;
        this.updatedDeck = updatedDeck;
        LOGGER.fine("ComponentPlacedEvent instantiated for game: " + gameId + ", player: " + player.getNickname() + ", component: " + component.getType());
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
        return player.getId().toString();
    }

    public String getPlayerNickname() {
        return player.getNickname();
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Don't send to the requesting client (they get the response instead)
        PlayerId clientPlayerId = context.getPlayerIdForClient(clientId);
        if (player.getId().equals(clientPlayerId)) {
            LOGGER.finer("EVENT FILTERING - ComponentPlacedEvent NOT sent to requesting client: " + clientId);
            return false; // Exclude the requesting client
        }
        
        // Use default game filtering for other clients
        return super.shouldSendTo(clientId, context);
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // SIMPLIFIED: Direct model updates instead of complex conversion
            
            // NEW: Simple direct model update via ClientState
            // Update the player in game model
            context.getClientState().updatePlayer(player);
            // Update the component deck state
            context.getClientState().updateComponentDeck(updatedDeck);
            // UI refreshes automatically via ClientState.refreshCurrentView()


            // Show appropriate notification
            if (context.getNotificationService() != null) {
                if (context.isLocalPlayer(getPlayerId())) {
                    LOGGER.fine("Displaying 'Component Placed' notification for local player.");
                    context.getNotificationService().showNotification(
                        new it.polimi.ingsw.client.ui.Notification(
                            "Component Placed",
                            "Component successfully placed on your ship",
                            it.polimi.ingsw.client.ui.NotificationType.SUCCESS
                        )
                    );
                } else {
                    LOGGER.fine("Displaying 'Opponent Move' notification for component placed by " + getPlayerNickname());
                    context.getNotificationService().showNotification(
                        new it.polimi.ingsw.client.ui.Notification(
                            "Opponent Move",
                            getPlayerNickname() + " placed a " + component.getType().name().toLowerCase().replace("_", " "),
                            it.polimi.ingsw.client.ui.NotificationType.INFO
                        )
                    );
                }
            }

        });
    }
}