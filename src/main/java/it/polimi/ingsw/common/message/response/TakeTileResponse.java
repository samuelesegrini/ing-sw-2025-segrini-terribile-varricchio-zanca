package it.polimi.ingsw.common.message.response;

// REMOVED: LocalGameState no longer needed
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import java.util.UUID;

/**
 * Response to a TakeTileRequest, containing the complete information about the drawn tile.
 * ENHANCED VERSION: Carries full server models instead of just ComponentData.
 */
public class TakeTileResponse extends AbstractResponse {
    private final Player updatedPlayer;    // Full Player model
    private final Component component;     // Full Component model
    private final ComponentDeck updatedDeck; // Full ComponentDeck model

    // NEW: Enhanced constructor with server models
    public TakeTileResponse(UUID correlationId, boolean success, String message,
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
            // Simple model replacement via ClientState
            if (context.getClientState() != null) {
                context.getClientState().updatePlayer(updatedPlayer);
                context.getClientState().updateComponentDeck(updatedDeck);
                // UI refreshes automatically
            }
            
            context.showNotification("Component Taken", "You drew a " + component.getType().name(),
                    it.polimi.ingsw.client.ui.NotificationType.INFO);
        }
    }
}