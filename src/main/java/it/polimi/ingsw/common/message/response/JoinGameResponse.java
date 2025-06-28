package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Lightweight response to join game request.
 * Acknowledges that the request was processed successfully.
 * The PlayerJoinedGameEvent is the single source of truth for state updates.
 */
public class JoinGameResponse extends AbstractResponse {

    public JoinGameResponse(UUID correlationId) {
        super(correlationId);
    }

    @Override
    public void handleOnClient(ClientContext context) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(JoinGameResponse.class.getName());
        
        if (isSuccess()) {
            logger.info("🎯 JOIN GAME RESPONSE - Received confirmation that the join game request was successful.");
            
            // Show a simple acknowledgment notification
            context.showNotification("Request Acknowledged", 
                "Join game request processed successfully.", 
                NotificationType.SUCCESS);
                
            // DO NOT navigate or update the game model here.
            // The PlayerJoinedGameEvent handler is responsible for state updates and navigation.
        } else {
            // Show error notification for failed join
            context.showNotification(
                    "Join Failed",
                    getErrorMessage() != null ? getErrorMessage() : "Failed to join game",
                    NotificationType.ERROR
            );
        }

        context.getController().getUI().onJoinGameResponse(this);
    }

}
