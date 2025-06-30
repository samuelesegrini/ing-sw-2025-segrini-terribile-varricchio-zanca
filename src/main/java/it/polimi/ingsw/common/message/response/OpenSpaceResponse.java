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

    /**
     *
     * @param correlationId The correlation ID
     * @param newFlightPosition The new flight position
     * @param usedBatteries The number of used batteries
     */

    public OpenSpaceResponse(UUID correlationId, int newFlightPosition, int usedBatteries) {
        super(correlationId);
        this.newFlightPosition = newFlightPosition;
        this.usedBatteries = usedBatteries;
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