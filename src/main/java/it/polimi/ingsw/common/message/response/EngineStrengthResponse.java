package it.polimi.ingsw.common.message.response;

import java.util.UUID;

/**
 * Response confirming that an engine strength declaration was received and processed.
 */
public class EngineStrengthResponse extends AbstractResponse {
    private final int engineStrength;

    public EngineStrengthResponse(UUID correlationId, int engineStrength) {
        super(correlationId);
        this.engineStrength = engineStrength;
    }

    public int getEngineStrength() {
        return engineStrength;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess()) {
            context.showNotification(
                "Engine Strength",
                "Engine strength of " + engineStrength + " declared",
                it.polimi.ingsw.client.ui.NotificationType.SUCCESS
            );
        } else {
            context.showError("Engine Strength Error", getErrorMessage());
        }
    }

    @Override
    public String toString() {
        return "EngineStrengthResponse{" +
                "correlationId=" + getCorrelationId() +
                ", success=" + isSuccess() +
                ", engineStrength=" + engineStrength +
                '}';
    }
}