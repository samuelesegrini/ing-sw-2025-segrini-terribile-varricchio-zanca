package it.polimi.ingsw.common.message.response;

import java.util.UUID;

/**
 * Response indicating that the flight phase has been successfully started.
 */
public class StartFlightPhaseResponse extends AbstractResponse {

    public StartFlightPhaseResponse(UUID correlationId) {
        super(correlationId);
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess()) {
            context.showNotification(
                "Flight Phase",
                "Flight phase has started - adventure awaits!",
                it.polimi.ingsw.client.ui.NotificationType.SUCCESS
            );
        } else {
            context.showError("Start Flight Phase Error", getErrorMessage());
        }
    }

    @Override
    public String toString() {
        return "StartFlightPhaseResponse{" +
                "correlationId=" + getCorrelationId() +
                ", success=" + isSuccess() +
                '}';
    }
}