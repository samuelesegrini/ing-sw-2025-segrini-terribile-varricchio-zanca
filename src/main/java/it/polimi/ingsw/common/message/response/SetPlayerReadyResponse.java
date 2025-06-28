package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.beans.PropertyChangeEvent;
import java.util.UUID;

/**
 * Response to set player ready request.
 * Confirms whether the player's ready status was successfully updated.
 */
public class SetPlayerReadyResponse extends AbstractResponse {

    private final boolean ready;

    public SetPlayerReadyResponse(UUID correlationId, boolean ready) {
        super(correlationId);
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SetPlayerReadyResponse.class.getName());

        if (isSuccess()) {
            logger.info("🎯 SET PLAYER READY RESPONSE - Received confirmation that the ready status request was successful.");
            
            // Show a simple acknowledgment notification
            context.showNotification("Request Acknowledged", 
                "Ready status request processed successfully.", 
                NotificationType.SUCCESS);
                
            // DO NOT update the player ready status here.
            // The PlayerReadyChangedEvent and GameLobbyUpdateEvent handlers are responsible for state updates.
        } else {
            // Show error notification for failed ready status change
            context.showNotification(
                    "Ready Status Failed",
                    getErrorMessage() != null ? getErrorMessage() : "Failed to update ready status",
                    NotificationType.ERROR
            );
        }
    }
}