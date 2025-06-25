package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import java.util.UUID;

/**
 * Response to tile placement request.
 * ENHANCED VERSION: Carries full server models instead of just confirmation.
 */
public class PlaceTileResponse extends AbstractResponse {
    private final Ship updatedShip;        // Full ship state
    private final ComponentDeck updatedDeck; // Full deck state

    // Legacy constructor for backward compatibility
    public PlaceTileResponse(UUID correlationId) {
        super(correlationId);
        this.updatedShip = null;
        this.updatedDeck = null;
    }

    // NEW: Enhanced constructor with server models
    public PlaceTileResponse(UUID correlationId, boolean success, String message, 
                            Ship updatedShip, ComponentDeck updatedDeck) {
        super(correlationId, success, message);
        this.updatedShip = updatedShip;
        this.updatedDeck = updatedDeck;
    }

    public Ship getUpdatedShip() {
        return updatedShip;
    }

    public ComponentDeck getUpdatedDeck() {
        return updatedDeck;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess() && updatedShip != null && updatedDeck != null) {
            // NEW: Simple model replacement via ClientState
            if (context instanceof ClientContextEnhanced enhancedContext) {
                if (enhancedContext.getClientState() != null) {
                    enhancedContext.getClientState().updateLocalPlayerShip(updatedShip);
                    enhancedContext.getClientState().updateComponentDeck(updatedDeck);
                    // UI refreshes automatically
                }
            }
            // LEGACY: If ClientState not available, confirmation handled by event
        }
    }
    
    // Interface to detect enhanced ClientContext with ClientState
    public interface ClientContextEnhanced extends ClientContext {
        it.polimi.ingsw.client.core.ClientState getClientState();
    }
}