package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.Map;
import java.util.UUID;

/**
 * Response to a successful DeclareStrengthRequest (following an OpenSpaceCard).
 */
public class OpenSpaceResponse extends AbstractResponse {
    private final int usedBatteries; // Necessario?
    private final int newFlightPosition;

    public OpenSpaceResponse(UUID correlationId, int newFlightPosition, int usedBatteries) {
        super(correlationId);
        this.newFlightPosition = newFlightPosition;
        this.usedBatteries = usedBatteries;
    }
    
    @Override
    public void handleOnClient(ClientContext context) {
        // TODO
    }
}