package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.server.model.domain.ship.Position;

import java.util.Set;
import java.util.UUID;

/**
 * Response to a successful DeclareStrengthRequest (following a MeteorShowerCard).
 */
public class MeteorSwarmResponse extends AbstractResponse {
    private final int usedBatteries; // Necessario?
    private final Set<Position> destroyedComponents;

    /**
     *
     * @param correlationId The correlation ID
     * @param usedBatteries The number of used batteries
     * @param destroyedComponents  The positions of the destroyed components
     */

    public MeteorSwarmResponse(UUID correlationId, int usedBatteries, Set<Position> destroyedComponents) {
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