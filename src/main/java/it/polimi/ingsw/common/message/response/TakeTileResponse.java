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
        System.out.println("[SERVER DEBUG] TakeTileResponse constructor - Player " + updatedPlayer.getId() + " has " + updatedPlayer.getHeldComponents().size() + " held components");
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
        System.out.println("[TAKETILE DEBUG] TakeTileResponse.handleOnClient() called - success: " + isSuccess());
        if (isSuccess() && component != null) {
            System.out.println("[TAKETILE DEBUG] SIMPLIFIED - Directly setting component in local player");
            
            // SIMPLIFIED: Instead of complex player object updates, directly set the component
            if (context.getClientState() != null) {
                var localPlayer = context.getClientState().getLocalPlayer();
                if (localPlayer != null) {
                    System.out.println("[TAKETILE DEBUG] Found local player: " + localPlayer.getId());
                    localPlayer.addComponent(component);
                    System.out.println("[TAKETILE DEBUG] Added component directly - player now has: " + localPlayer.getHeldComponents().size() + " components");
                    
                    // Update deck if provided
                    if (updatedDeck != null) {
                        context.getClientState().updateComponentDeck(updatedDeck);
                    }
                    
                    // Trigger UI refresh
                    context.getClientState().refreshCurrentViewOnly();
                    System.out.println("[TAKETILE DEBUG] Triggered UI refresh");
                } else {
                    System.out.println("[TAKETILE DEBUG] ERROR: Local player not found!");
                }
            } else {
                System.out.println("[TAKETILE DEBUG] ERROR: ClientState is null!");
            }
            
            context.showNotification("Component Taken", "You drew a " + component.getType().name(),
                    it.polimi.ingsw.client.ui.NotificationType.INFO);
        } else {
            System.out.println("[TAKETILE DEBUG] TakeTileResponse failed or missing data - success: " + isSuccess() + 
                ", component: " + (component != null));
        }
    }
}