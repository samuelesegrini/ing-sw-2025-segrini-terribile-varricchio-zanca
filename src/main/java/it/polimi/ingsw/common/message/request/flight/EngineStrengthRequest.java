package it.polimi.ingsw.common.message.request.flight;

import it.polimi.ingsw.common.message.request.AbstractRequest;
import it.polimi.ingsw.common.message.request.RequestContext;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.EngineStrengthResponse;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardController;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardState;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.enums.GamePhase;

/**
 * Request from a player to declare their engine strength during Open Space cards.
 * Player must declare how much engine strength they want to use.
 */
public class EngineStrengthRequest extends AbstractRequest {
    private final int engineStrength;
    private final int batteriesToUse;

    public EngineStrengthRequest(int engineStrength) {
        this.engineStrength = engineStrength;
        this.batteriesToUse = 0;
    }

    public EngineStrengthRequest(int engineStrength, int batteriesToUse) {
        this.engineStrength = engineStrength;
        this.batteriesToUse = batteriesToUse;
    }

    public int getEngineStrength() {
        return engineStrength;
    }

    public int getBatteriesToUse() {
        return batteriesToUse;
    }

    @Override
    public ValidationResult validate() {
        if (engineStrength < 0) {
            return ValidationResult.failure("Engine strength cannot be negative", "engineStrength");
        }
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
            return createErrorResponse("Can only declare engine strength during flight phase", 
                                     ErrorResponse.INVALID_STATE);
        }

        try {
            Player player = session.getPlayer(context.getPlayerId());
            if (player == null) {
                return createErrorResponse("Player not found", ErrorResponse.INTERNAL_ERROR);
            }

            // Check if player has engine strength > 0
            if (player.getShip().getEngines() <= 0) {
                return createErrorResponse("No engine strength available", ErrorResponse.INVALID_STATE);
            }

            // Check if declared strength is within available range
            double maxEngineStrength = player.getShip().getEngines();
            if (engineStrength > maxEngineStrength) {
                return createErrorResponse("Declared engine strength exceeds available engine power", 
                                         ErrorResponse.INVALID_STATE);
            }

            // Get the adventure card controller
            AdventureCardController cardController = session.getAdventureCardController();
            if (cardController == null || !cardController.isProcessingCard()) {
                return createErrorResponse("No adventure card is currently being processed", 
                                         ErrorResponse.INVALID_STATE);
            }

            // Check if player has enough batteries if they want to use them
            if (batteriesToUse > 0 && player.getShip().getBatteries() < batteriesToUse) {
                return createErrorResponse("Not enough batteries", ErrorResponse.INVALID_STATE);
            }

            // Create player choice
            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                context.getPlayerId(), 
                AdventureCardState.AdventureChoiceType.ENGINE_STRENGTH
            );
            choice.setParameter("engineStrength", engineStrength);
            choice.setParameter("batteriesToUse", batteriesToUse);

            // Submit choice to controller
            boolean success = cardController.handlePlayerChoice(context.getPlayerId(), choice);
            
            if (success) {
                return new EngineStrengthResponse(getCorrelationId(), engineStrength);
            } else {
                return createErrorResponse("Failed to submit engine strength - not your turn or invalid choice", 
                                         ErrorResponse.INVALID_STATE);
            }
        } catch (Exception e) {
            return createErrorResponse("Failed to process engine strength: " + e.getMessage(), 
                                     ErrorResponse.INTERNAL_ERROR);
        }
    }
}