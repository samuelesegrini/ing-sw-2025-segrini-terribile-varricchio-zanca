package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Lightweight response to a ReserveTileRequest.
 * Acknowledges that the request was processed successfully.
 * The ComponentReservedEvent is the single source of truth for state updates.
 */
public class ReserveTileResponse extends AbstractResponse {

    public ReserveTileResponse(UUID correlationId) {
        super(correlationId);
    }

    @Override
    public void handleOnClient(ClientContext context) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ReserveTileResponse.class.getName());
        
        if (isSuccess()) {
            logger.info("🎯 RESERVE TILE RESPONSE - Received confirmation that the reserve tile request was successful.");
            
            // Show a simple acknowledgment notification
            context.showNotification("Request Acknowledged", 
                "Reserve tile request processed successfully.", 
                NotificationType.SUCCESS);
                
            // DO NOT update the player or deck state here.
            // The ComponentReservedEvent handler is responsible for all state updates.
        } else {
            // Show error notification for failed tile reservation
            context.showNotification(
                    "Reserve Tile Failed",
                    getErrorMessage() != null ? getErrorMessage() : "Failed to reserve tile",
                    NotificationType.ERROR
            );
        }
    }
}