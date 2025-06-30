package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.server.model.domain.ship.Position;

import java.util.Set;
import java.util.UUID;

/**
 * Response to a successful DeclareStrengthRequest (following the loss to a PiratesCard).
 */
public class PiratesLossResponse extends AbstractResponse {
    private final int usedBatteries; // Necessario?
    private final Set<Position> destroyedComponents;

    /**
     * constructor
     *
     * @param correlationId The
     * @param usedBatteries The number of used batteries
     * @param destroyedComponents The number of destroyed components
     */

    public PiratesLossResponse(UUID correlationId, int usedBatteries, Set<Position> destroyedComponents) {
        super(correlationId);
        this.usedBatteries = usedBatteries;
        this.destroyedComponents = Set.copyOf(destroyedComponents);
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