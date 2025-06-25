package it.polimi.ingsw.common.message.response;

import java.util.UUID;

/**
 * Response to a successful DeclareStrengthRequest (following a PiratesCard).
 */
public class PiratesResponse extends AbstractResponse {
    private final int usedBatteries; // Necessario?
    private final boolean hasWon;
    private final int collectedCredits;

    // TODO: SE I PIRATI VINCONO VIENI SPARATO DI NUOVO (ALTRO MESSAGGIO?)

    public PiratesResponse(UUID correlationId, int usedBatteries, boolean hasWon, int collectedCredits) {
        super(correlationId);
        this.usedBatteries = usedBatteries;
        this.hasWon = hasWon;
        this.collectedCredits = collectedCredits;
    }
    
    @Override
    public void handleOnClient(ClientContext context) {
        // TODO
    }
}