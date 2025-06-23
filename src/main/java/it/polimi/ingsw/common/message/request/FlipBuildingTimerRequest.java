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
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), 
                it.polimi.ingsw.common.message.response.ErrorResponse.VALIDATION_ERROR);
        }

        // Get game session
        var gameSession = context.getGameSession();
        if (gameSession == null) {
            return createErrorResponse("Not in a game", 
                it.polimi.ingsw.common.message.response.ErrorResponse.INVALID_STATE);
        }
        
        // Check if building phase is active
        if (gameSession.getGameModel().getCurrentPhase() != it.polimi.ingsw.server.model.enums.GamePhase.BUILDING) {
            return createErrorResponse("Not in building phase", 
                it.polimi.ingsw.common.message.response.ErrorResponse.INVALID_STATE);
        }
        
        String playerId = context.getPlayerId();
        String playerNickname = context.getPlayerNickname();
        String gameId = context.getGameId();
        
        try {
            // Flip the timer - typically extends building time
            long additionalTime = 30000; // 30 seconds additional time
            long newTimeRemaining = gameSession.flipBuildingTimer(additionalTime);
            
            // Publish event to notify all players
            context.getEventPublisher().publishEvent(
                new it.polimi.ingsw.common.message.event.BuildingTimerFlippedEvent(
                    gameId, playerId, playerNickname, newTimeRemaining, 1
                )
            );
            
            return new it.polimi.ingsw.common.message.response.FlipBuildingTimerResponse(
                getCorrelationId(), newTimeRemaining);
                
        } catch (Exception e) {
            return createErrorResponse("Failed to flip building timer: " + e.getMessage(), 
                it.polimi.ingsw.common.message.response.ErrorResponse.INTERNAL_ERROR);
        }
    }
}