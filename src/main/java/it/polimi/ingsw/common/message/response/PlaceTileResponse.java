package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Lightweight response to a PlaceTileRequest.
 * Acknowledges that the request was processed successfully.
 * The ComponentPlacedEvent is the single source of truth for state updates.
 */
public class PlaceTileResponse extends AbstractResponse {

    public PlaceTileResponse(UUID correlationId) {
        super(correlationId);
    }

    @Override
    public void handleOnClient(ClientContext context) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(PlaceTileResponse.class.getName());
        
        if (isSuccess()) {
            logger.info("🎯 PLACE TILE RESPONSE - Received confirmation that the place tile request was successful.");
            
            // Show a simple acknowledgment notification
            context.showNotification("Request Acknowledged", 
                "Place tile request processed successfully.", 
                NotificationType.SUCCESS);
                
            // DO NOT update the ship or deck state here.
            // The ComponentPlacedEvent handler is responsible for all state updates.
        } else {
            // Show error notification for failed tile placement
            context.showNotification(
                    "Place Tile Failed",
                    getErrorMessage() != null ? getErrorMessage() : "Failed to place tile",
                    NotificationType.ERROR
            );
        }
    }
}