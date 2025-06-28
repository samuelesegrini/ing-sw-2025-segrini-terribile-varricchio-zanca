package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Lightweight response to a LeaveGameRequest.
 * Acknowledges that the request was processed successfully.
 * The PlayerLeftGameEvent is the single source of truth for state updates.
 */
public class LeaveGameResponse extends AbstractResponse {

    public LeaveGameResponse(UUID correlationId) {
        super(correlationId);
    }

    @Override
    public void handleOnClient(ClientContext context) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(LeaveGameResponse.class.getName());
        
        if (isSuccess()) {
            logger.info("🎯 LEAVE GAME RESPONSE - Received confirmation that the leave game request was successful.");
            
            // Show a simple acknowledgment notification
            context.showNotification("Request Acknowledged", 
                "Leave game request processed successfully.", 
                NotificationType.SUCCESS);
                
            // DO NOT update the client state or navigate here.
            // The PlayerLeftGameEvent handler is responsible for all state updates and navigation.
        } else {
            // Show error notification for failed leave game
            context.showNotification(
                    "Failed to Leave",
                    getErrorMessage() != null ? getErrorMessage() : "Failed to leave game",
                    NotificationType.ERROR
            );
        }
    }

}