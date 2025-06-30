package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Response to SetPlayerReadyRequest.
 */
public class SetPlayerReadyResponse extends AbstractResponse {
    private final boolean ready;

    /**
     * constructor
     *
     * @param correlationId The correlation ID
     * @param ready whether the player is ready or not
     */

    public SetPlayerReadyResponse(UUID correlationId, boolean ready) {
        super(correlationId);
        this.ready = ready;
    }

    /**
     *
     * @return true if the player is ready, false otherwise
     */

    public boolean isReady() {
        return ready;
    }

    /**
     *
     * @param context The client context
     */

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