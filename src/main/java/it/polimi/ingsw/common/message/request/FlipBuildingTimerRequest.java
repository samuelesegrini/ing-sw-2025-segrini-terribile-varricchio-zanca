package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;

/**
 * Request sent by a player to flip the building timer during the ship building phase.
 * In Galaxy Trucker, players can flip the sand timer to give themselves and others more time.
 */
public class FlipBuildingTimerRequest extends AbstractRequest {

    public FlipBuildingTimerRequest() {
        super();
    }

    @Override
    public ValidationResult validate() {
        // No validation needed for timer flip - always valid during building phase
        return ValidationResult.success();
    }

    @Override
    public Response execute(RequestContext context) {
        // Server-side logic will handle flipping the timer and notifying all players.
        // This is just a placeholder on the client side.
        // The actual implementation is in the server's command dispatcher.
        return null;
    }
}