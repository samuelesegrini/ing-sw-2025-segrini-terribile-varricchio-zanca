package it.polimi.ingsw.common.message.response;

import java.util.UUID;

/**
 * Response confirming that a combat strength declaration was received and processed.
 */
public class CombatStrengthResponse extends AbstractResponse {
    private final int batteriesToUse;

    /**
     *
     * @param correlationId The correlation ID
     * @param batteriesToUse The number of batteries to use
     */

    public CombatStrengthResponse(UUID correlationId, int batteriesToUse) {
        super(correlationId);
        this.batteriesToUse = batteriesToUse;
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
     * @param context The client context
     */

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

    /**
     *
     * @return the combat strength response as a string: "CombatStrengthResponse{correlationId= ID, success= true/false, batteriesToUse= int}
     */

    @Override
    public String toString() {
        return "CombatStrengthResponse{" +
                "correlationId=" + getCorrelationId() +
                ", success=" + isSuccess() +
                ", batteriesToUse=" + batteriesToUse +
                '}';
    }
}