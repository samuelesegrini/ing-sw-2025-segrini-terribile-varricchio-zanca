package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Lightweight response to a DrawAdventureCardRequest.
 * Acknowledges that the request was processed successfully.
 * The AdventureCardDrawnEvent is the single source of truth for state updates.
 */
public class DrawAdventureCardResponse extends AbstractResponse {

    /**
     *
     * @param correlationId The correlation ID
     */

    public DrawAdventureCardResponse(UUID correlationId) {
        super(correlationId);
    }

    /**
     *
     * @param context The client context
     */

    @Override
    public void handleOnClient(ClientContext context) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(DrawAdventureCardResponse.class.getName());
        
        if (isSuccess()) {
            logger.info("🎯 DRAW ADVENTURE CARD RESPONSE - Received confirmation that the draw adventure card request was successful.");
            
            // Show a simple acknowledgment notification
            context.showNotification("Request Acknowledged", 
                "Draw adventure card request processed successfully.", 
                NotificationType.SUCCESS);
                
            // DO NOT update the adventure card state here.
            // The AdventureCardDrawnEvent handler is responsible for all state updates.
        } else {
            // Show error notification for failed adventure card draw
            context.showNotification(
                    "Draw Card Failed",
                    getErrorMessage() != null ? getErrorMessage() : "Failed to draw adventure card",
                    NotificationType.ERROR
            );
        }
    }

    /**
     *
     * @return the draw adventure card response as a string
     */

    @Override
    public String toString() {
        return "DrawAdventureCardResponse{" +
                "correlationId=" + getCorrelationId() +
                ", success=" + isSuccess() +
                '}';
    }
}