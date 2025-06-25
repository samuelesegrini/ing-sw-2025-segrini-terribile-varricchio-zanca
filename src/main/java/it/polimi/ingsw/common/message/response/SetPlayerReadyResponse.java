package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

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
        if (isSuccess()) {
            // Update the model with the new ready status
            String playerId = context.getPlayerId();
            if (playerId != null) {
                context.getClientState().setPlayerReadyStatus(playerId, ready);
            }
            
            // Show confirmation notification
            context.showNotification(
                    "Ready Status",
                    ready ? "You are now ready to start the game" : "You are no longer ready",
                    NotificationType.INFO
            );
        } else {
            // Show error notification
            context.showNotification(
                    "Ready Status Failed",
                    getErrorMessage() != null ? getErrorMessage() : "Failed to update ready status",
                    NotificationType.ERROR
            );
        }
    }
}