package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.common.message.request.DeclareStrengthRequest;

import java.util.UUID;

/**
 * Response indicating that strength has been successfully declared and batteries committed.
 */
public class DeclareStrengthResponse extends AbstractResponse {
    private final int batteriesToUse;
    private final DeclareStrengthRequest.DecisionType decisionType;

    public DeclareStrengthResponse(UUID correlationId, int batteriesToUse, DeclareStrengthRequest.DecisionType decisionType) {
        super(correlationId);
        this.batteriesToUse = batteriesToUse;
        this.decisionType = decisionType;
    }

    public int getBatteriesToUse() {
        return batteriesToUse;
    }

    public DeclareStrengthRequest.DecisionType getDecisionType() {
        return decisionType;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess()) {
            context.showNotification(
                "Strength Declared",
                decisionType + " strength declared using " + batteriesToUse + " batteries",
                it.polimi.ingsw.client.ui.NotificationType.SUCCESS
            );
        } else {
            context.showError("Declare Strength Error", getErrorMessage());
        }
    }

    @Override
    public String toString() {
        return "DeclareStrengthResponse{" +
                "correlationId=" + getCorrelationId() +
                ", success=" + isSuccess() +
                ", batteriesToUse=" + batteriesToUse +
                ", decisionType=" + decisionType +
                '}';
    }
}