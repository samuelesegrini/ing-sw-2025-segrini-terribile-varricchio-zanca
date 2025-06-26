package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.CombatStrengthResponse;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardController;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardState;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.enums.GamePhase;

/**
 * Request from a player to declare their combat strength during enemy encounters.
 * Includes the number of batteries to commit to cannon strength.
 */
public class CombatStrengthRequest extends AbstractRequest {
    private final int batteriesToUse;

    public CombatStrengthRequest(int batteriesToUse) {
        this.batteriesToUse = batteriesToUse;
    }

    public int getBatteriesToUse() {
        return batteriesToUse;
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

        if (session.getCurrentPhase() != GamePhase.FLIGHT) {
            return createErrorResponse("Can only declare combat strength during flight phase", 
                                     ErrorResponse.INVALID_STATE);
        }

        try {
            Player player = session.getPlayer(context.getPlayerId());
            if (player == null) {
                return createErrorResponse("Player not found", ErrorResponse.INTERNAL_ERROR);
            }

            // Check if player has enough batteries
            if (player.getShip().getBatteries() < batteriesToUse) {
                return createErrorResponse("Not enough batteries", ErrorResponse.INVALID_STATE);
            }

            // Get the adventure card controller
            AdventureCardController cardController = session.getAdventureCardController();
            if (cardController == null || !cardController.isProcessingCard()) {
                return createErrorResponse("No adventure card is currently being processed", 
                                         ErrorResponse.INVALID_STATE);
            }

            // Create player choice
            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                context.getPlayerId(), 
                AdventureCardState.AdventureChoiceType.COMBAT_STRENGTH
            );
            choice.setParameter("batteriesToUse", batteriesToUse);

            // Submit choice to controller
            boolean success = cardController.handlePlayerChoice(context.getPlayerId(), choice);
            
            if (success) {
                return new CombatStrengthResponse(getCorrelationId(), batteriesToUse);
            } else {
                return createErrorResponse("Failed to submit combat strength - not your turn or invalid choice", 
                                         ErrorResponse.INVALID_STATE);
            }
        } catch (Exception e) {
            return createErrorResponse("Failed to process combat strength: " + e.getMessage(), 
                                     ErrorResponse.INTERNAL_ERROR);
        }
    }
}