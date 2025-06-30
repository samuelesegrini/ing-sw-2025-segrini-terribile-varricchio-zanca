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

    /**
     *
     * @param correlationId The correlation ID
     * @param usedBatteries The number of used batteries
     * @param lostFlightDays The number of days the player loses
     * @param lostCrew The number of crew members the player loses
     */

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