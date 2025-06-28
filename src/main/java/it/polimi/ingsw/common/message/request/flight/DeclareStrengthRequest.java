package it.polimi.ingsw.common.message.request.flight;

import it.polimi.ingsw.common.message.request.AbstractRequest;
import it.polimi.ingsw.common.message.request.RequestContext;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.common.message.response.DeclareStrengthResponse;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.player.Player;

/**
 * Request from a player to declare strength for an action,
 * such as powering up engines or cannons, by committing batteries.
 */
public class DeclareStrengthRequest extends AbstractRequest {

    public enum DecisionType {
        DECLARE_ENGINE_STRENGTH,
        DECLARE_CANNON_STRENGTH
    }

    private final DecisionType decisionType;
    private final int batteriesToUse;

    public DeclareStrengthRequest(DecisionType decisionType, int batteriesToUse) {
        this.decisionType = decisionType;
        this.batteriesToUse = batteriesToUse;
    }

    @Override
    public ValidationResult validate() {
        if (batteriesToUse < 0) {
            return ValidationResult.failure("Batteries to use cannot be negative", "batteriesToUse");
        }
        return ValidationResult.success();
    }

    @Override
    public Response execute(RequestContext context) {
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), ErrorResponse.VALIDATION_ERROR);
        }

        GameSession session = context.getGameSession();
        if (session == null) {
            return createErrorResponse("Not in a game", ErrorResponse.INVALID_STATE);
        }

        Player player = session.getPlayer(context.getPlayerId());
        if (player == null) {
            return createErrorResponse("Player not found", ErrorResponse.INTERNAL_ERROR);
        }

        // Check if the player has enough batteries
        if (player.getShip().getBatteries() < batteriesToUse) {
            return createErrorResponse("Not enough batteries", ErrorResponse.INVALID_STATE);
        }

        try {
            // Galaxy Trucker: Consume batteries directly (no charging state)
            int actualBatteriesUsed = player.getShip().consumeBatteries(batteriesToUse);
            
            if (actualBatteriesUsed < batteriesToUse) {
                return createErrorResponse("Only " + actualBatteriesUsed + " batteries available", 
                                         ErrorResponse.INVALID_STATE);
            }
            
            return new DeclareStrengthResponse(getCorrelationId(), actualBatteriesUsed, decisionType);
        } catch (Exception e) {
            return createErrorResponse("Failed to consume batteries: " + e.getMessage(), 
                                     ErrorResponse.INTERNAL_ERROR);
        }
    }
}