package it.polimi.ingsw.common.message.response;

import java.util.UUID;

/**
 * Response to a successful DeclareStrengthRequest (following a CombatZoneCard).
 */
public class CombatZoneResponse extends AbstractResponse {
    private final int usedBatteries; // Necessario?
    private final int lostFlightDays;
    private final int lostCrew;

    // TODO: IL GIOCATORE CON MENO CANNONI VIENE SPARATO (ALTRO MESSAGGIO?)

    public CombatZoneResponse(UUID correlationId, int usedBatteries, int lostFlightDays, int lostCrew) {
        super(correlationId);
        this.usedBatteries = usedBatteries;
        this.lostFlightDays = lostFlightDays;
        this.lostCrew = lostCrew;
    }
    
    @Override
    public void handleOnClient(ClientContext context) {
        // TODO
    }
}