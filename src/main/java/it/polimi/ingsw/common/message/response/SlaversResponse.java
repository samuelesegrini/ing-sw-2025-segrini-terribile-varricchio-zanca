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

    public SlaversResponse(UUID correlationId, int usedBatteries, boolean hasWon, int lostCrew, int lostFlightDays, int collectedCredits) {
        super(correlationId);
        this.usedBatteries = usedBatteries;
        this.hasWon = hasWon;
        this.lostCrew = lostCrew;
        this.lostFlightDays = lostFlightDays;
        this.collectedCredits = collectedCredits;
    }
    
    @Override
    public void handleOnClient(ClientContext context) {
        // TODO
    }
}