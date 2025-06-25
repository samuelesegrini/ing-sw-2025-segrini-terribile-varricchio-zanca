package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import java.util.UUID;

/**
 * Response to a reserve tile request.
 * ENHANCED VERSION: Carries full server models instead of just confirmation.
 */
public class ReserveTileResponse extends AbstractResponse {
    private final Player updatedPlayer;    // Full Player model
    private final Component component;     // Full Component model
    private final ComponentDeck updatedDeck; // Full ComponentDeck model
    
    // Legacy constructor for backward compatibility
    public ReserveTileResponse(UUID correlationId) {
        super(correlationId);
        this.updatedPlayer = null;
        this.component = null;
        this.updatedDeck = null;
    }

    // NEW: Enhanced constructor with server models
    public ReserveTileResponse(UUID correlationId, boolean success, String message,
                              Player updatedPlayer, Component component, ComponentDeck updatedDeck) {
        super(correlationId, success, message);
        this.updatedPlayer = updatedPlayer;
        this.component = component;
        this.updatedDeck = updatedDeck;
    }

    public Player getUpdatedPlayer() {
        return updatedPlayer;
    }

    public Component getComponent() {
        return component;
    }

    public ComponentDeck getUpdatedDeck() {
        return updatedDeck;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess() && updatedPlayer != null && component != null && updatedDeck != null) {
            // NEW: Simple model replacement via ClientState
            if (context.getClientState() != null) {
                // Update client state using existing methods
                context.getClientState().updatePlayer(updatedPlayer);
                context.getClientState().updateComponentDeck(updatedDeck);
                // UI refreshes automatically
            }
            
            context.showNotification("Component Reserved", "Component reserved successfully",
                    it.polimi.ingsw.client.ui.NotificationType.INFO);
        } else {
            // LEGACY: The actual reservation logic is handled by the ComponentReservedEvent
            context.showNotification("Component Reserved", "Component reserved successfully",
                    it.polimi.ingsw.client.ui.NotificationType.INFO);
        }
    }
}