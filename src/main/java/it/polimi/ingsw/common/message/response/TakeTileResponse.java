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

    // Legacy constructor for backward compatibility
    public TakeTileResponse(UUID correlationId, it.polimi.ingsw.common.ComponentData componentData) {
        super(correlationId);
        this.updatedPlayer = null;
        this.component = null;
        this.updatedDeck = null;
    }

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

    // Legacy getter for backward compatibility
    public it.polimi.ingsw.common.ComponentData getComponentData() {
        if (component != null) {
            return new it.polimi.ingsw.common.ComponentData(
                component.getId(),
                component.getType(),
                component.getConnectors(),
                component.getCurrentDirection()
            );
        }
        return null;
    }


    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess() && updatedPlayer != null && component != null && updatedDeck != null) {
            // NEW: Simple model replacement via ClientState
            if (context instanceof PlaceTileResponse.ClientContextEnhanced enhancedContext) {
                if (enhancedContext.getClientState() != null) {
                    enhancedContext.getClientState().updatePlayer(updatedPlayer);
                    enhancedContext.getClientState().updateComponentDeck(updatedDeck);
                    // UI refreshes automatically
                }
            } else {
                // LEGACY: Fallback to LocalGameState
                if (component.getConnectors() != null) {
                    ComponentInstance componentInstance = new ComponentInstance(
                        component.getId(), 
                        component.getType(), 
                        component.getConnectors()
                    );
                    componentInstance.setDirection(component.getCurrentDirection());

                    LocalGameState.getInstance().addHeldTile(componentInstance);
                    context.getModel().firePropertyChange("heldTiles", null, null);
                }
            }
            
            context.showNotification("Component Taken", "You drew a " + component.getType().name(),
                    it.polimi.ingsw.client.ui.NotificationType.INFO);
        } else {
            // Handle legacy ComponentData case
            it.polimi.ingsw.common.ComponentData componentData = getComponentData();
            if (componentData != null && componentData.getConnectors() != null) {
                ComponentInstance componentInstance = new ComponentInstance(
                    componentData.getId(), 
                    componentData.getType(), 
                    componentData.getConnectors()
                );
                componentInstance.setDirection(componentData.getDefaultDirection());

                LocalGameState.getInstance().addHeldTile(componentInstance);
                context.getModel().firePropertyChange("heldTiles", null, null);
                
                context.showNotification("Tile Drawn", "You drew a " + componentData.getType().name(),
                        it.polimi.ingsw.client.ui.NotificationType.INFO);
            }
        }
    }
}