package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Response to flip building timer request.
 * Confirms whether the timer was successfully flipped.
 */
public class FlipBuildingTimerResponse extends AbstractResponse {

    private final long newTimeRemaining;

    public FlipBuildingTimerResponse(UUID correlationId, long newTimeRemaining) {
        super(correlationId);
        this.newTimeRemaining = newTimeRemaining;
    }

    public long getNewTimeRemaining() {
        return newTimeRemaining;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess()) {
            // Show confirmation notification
            context.showNotification(
                    "Timer Flipped",
                    "Building timer flipped! " + (newTimeRemaining / 1000) + " seconds remaining",
                    NotificationType.INFO
            );
        } else {
            // Show error notification
            context.showNotification(
                    "Timer Flip Failed",
                    getErrorMessage() != null ? getErrorMessage() : "Failed to flip building timer",
                    NotificationType.ERROR
            );
        }
    }
}