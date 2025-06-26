package it.polimi.ingsw.common.message.response;

import java.util.UUID;

/**
 * Response confirming that a planet choice was received and processed.
 */
public class PlanetChoiceResponse extends AbstractResponse {
    private final int planetIndex;

    public PlanetChoiceResponse(UUID correlationId, int planetIndex) {
        super(correlationId);
        this.planetIndex = planetIndex;
    }

    public int getPlanetIndex() {
        return planetIndex;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess()) {
            String message = planetIndex == 0 ? 
                "Skipped planet landing" : 
                "Landed on planet " + planetIndex;
            context.showNotification(
                "Planet Choice",
                message,
                it.polimi.ingsw.client.ui.NotificationType.SUCCESS
            );
        } else {
            context.showError("Planet Choice Error", getErrorMessage());
        }
    }

    @Override
    public String toString() {
        return "PlanetChoiceResponse{" +
                "correlationId=" + getCorrelationId() +
                ", success=" + isSuccess() +
                ", planetIndex=" + planetIndex +
                '}';
    }
}