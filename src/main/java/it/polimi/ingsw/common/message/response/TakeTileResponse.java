package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Lightweight response to a TakeTileRequest.
 * Acknowledges that the request was processed successfully.
 * The ComponentTakenEvent is the single source of truth for state updates.
 */
public class TakeTileResponse extends AbstractResponse {

    public TakeTileResponse(UUID correlationId) {
        super(correlationId);
    }

    @Override
    public void handleOnClient(ClientContext context) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(TakeTileResponse.class.getName());
        
        if (isSuccess()) {
            logger.info("🎯 TAKE TILE RESPONSE - Received confirmation that the take tile request was successful.");
            
            // Show a simple acknowledgment notification
            context.showNotification("Request Acknowledged", 
                "Take tile request processed successfully.", 
                NotificationType.SUCCESS);
                
            // DO NOT update the player's components or deck here.
            // The ComponentTakenEvent handler is responsible for all state updates.
        } else {
            // Show error notification for failed tile take
            context.showNotification(
                    "Take Tile Failed",
                    getErrorMessage() != null ? getErrorMessage() : "Failed to take tile",
                    NotificationType.ERROR
            );
        }
    }
}