package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Response to SetPlayerReadyRequest.
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
        // Response provides immediate feedback with ready state
        if (isSuccess()) {
            context.showNotification("Ready Status Updated", 
                ready ? "You are ready!" : "You are not ready.", 
                NotificationType.SUCCESS);
        } else {
            context.showNotification("Operation Failed",
                getErrorMessage() != null ? getErrorMessage() : "Failed to update ready status",
                NotificationType.ERROR);
        }

        context.getController().getUI().onSetPlayerReadyResponse(this);
    }
}