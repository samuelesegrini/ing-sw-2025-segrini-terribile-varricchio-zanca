package it.polimi.ingsw.common.message.response;

import java.util.UUID;

/**
 * Response confirming that a combat strength declaration was received and processed.
 */
public class CombatStrengthResponse extends AbstractResponse {
    private final int batteriesToUse;

    public CombatStrengthResponse(UUID correlationId, int batteriesToUse) {
        super(correlationId);
        this.batteriesToUse = batteriesToUse;
    }

    public int getBatteriesToUse() {
        return batteriesToUse;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess()) {
            context.showNotification(
                "Combat Strength",
                "Combat strength declared using " + batteriesToUse + " batteries",
                it.polimi.ingsw.client.ui.NotificationType.SUCCESS
            );
        } else {
            context.showError("Combat Strength Error", getErrorMessage());
        }

        context.getController().getUI().onCombatStrengthResponse(this);
    }

    @Override
    public String toString() {
        return "CombatStrengthResponse{" +
                "correlationId=" + getCorrelationId() +
                ", success=" + isSuccess() +
                ", batteriesToUse=" + batteriesToUse +
                '}';
    }
}