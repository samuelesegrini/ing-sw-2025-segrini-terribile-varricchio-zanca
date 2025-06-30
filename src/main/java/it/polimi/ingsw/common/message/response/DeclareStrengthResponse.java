package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.common.message.request.flight.DeclareStrengthRequest;

import java.util.UUID;

/**
 * Response indicating that strength has been successfully declared and batteries committed.
 */
public class DeclareStrengthResponse extends AbstractResponse {
    private final int batteriesToUse;
    private final DeclareStrengthRequest.DecisionType decisionType;

    /**
     * constructor
     *
     * @param correlationId The correlation ID
     * @param batteriesToUse The number of batteries to use
     * @param decisionType The decision Type
     */

    public DeclareStrengthResponse(UUID correlationId, int batteriesToUse, DeclareStrengthRequest.DecisionType decisionType) {
        super(correlationId);
        this.batteriesToUse = batteriesToUse;
        this.decisionType = decisionType;
    }

    /**
     *
     * @return The number of batteries to use
     */

    public int getBatteriesToUse() {
        return batteriesToUse;
    }

    /**
     *
     * @return The decision Type
     */

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

        context.getController().getUI().onDeclareStrengthResponse(this);
    }

    /**
     *
     * @return the declare strength response as a string: "DeclareStrengthResponse{correlationId= ID, success= true/false, batteriesToUse= int, decisionType= ..}
     */

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