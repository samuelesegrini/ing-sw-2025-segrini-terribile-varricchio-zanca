package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.Map;
import java.util.UUID;

/**
 * Response to a successful DeclareStrengthRequest (following a SlaversCard).
 */
public class SlaversResponse extends AbstractResponse {
    private final int usedBatteries; // Necessario?
    private final boolean hasWon;
    private final int lostCrew;
    private final int lostFlightDays;
    private final int collectedCredits;

    /**
     * constructors
     *
     * @param correlationId The correlation ID
     * @param usedBatteries The number of  used batteries
     * @param hasWon whether the player has won
     * @param lostCrew The number crew members lost
     * @param lostFlightDays The number of lost flight days
     * @param collectedCredits The number of credits collected
     */

    public SlaversResponse(UUID correlationId, int usedBatteries, boolean hasWon, int lostCrew, int lostFlightDays, int collectedCredits) {
        super(correlationId);
        this.usedBatteries = usedBatteries;
        this.hasWon = hasWon;
        this.lostCrew = lostCrew;
        this.lostFlightDays = lostFlightDays;
        this.collectedCredits = collectedCredits;
    }

    /**
     *
     * @param context The client context
     */

    @Override
    public void handleOnClient(ClientContext context) {
        // TODO
    }
}