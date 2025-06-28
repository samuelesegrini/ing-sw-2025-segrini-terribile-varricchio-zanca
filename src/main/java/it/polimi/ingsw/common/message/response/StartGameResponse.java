package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Lightweight response to start game request.
 * Acknowledges that the request was processed successfully.
 * The GameStartedEvent is the single source of truth for state updates.
 */
public class StartGameResponse extends AbstractResponse {

    public StartGameResponse(UUID correlationId) {
        super(correlationId);
    }

    @Override
    public void handleOnClient(ClientContext context) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(StartGameResponse.class.getName());
        logger.info("🎯 START GAME RESPONSE - Received confirmation that the game start request was successful.");

        // Show a simple acknowledgment notification
        context.showNotification("Request Acknowledged", 
            "Game start request processed successfully.", 
            NotificationType.SUCCESS);

        // DO NOT navigate or update the game model here.
        // The GameStartedEvent handler is responsible for state updates and navigation.
    }
}