package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.event.BuildingTimerFlippedEvent;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.GenericSuccessResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.general.BuildingTimer;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;

/**
 * Request sent by a player to flip the building timer during the ship building phase.
 * Implements Galaxy Trucker three-stage timer system:
 * 1. Anyone can start first timer
 * 2. Anyone can flip first→second timer when expired
 * 3. Only finished players can flip second→end when expired
 */
public class FlipBuildingTimerRequest extends AbstractRequest {

    public FlipBuildingTimerRequest() {
        super();
    }

    @Override
    public ValidationResult validate() {
        // No validation needed for timer flip - always valid during building phase
        //TODO: basta lasciare implementazione vuota (?)
        return ValidationResult.success();
    }

    @Override
    public Response execute(RequestContext context) {
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), 
                ErrorResponse.VALIDATION_ERROR);
        }

        var gameSession = context.getGameSession();
        if (gameSession == null) {
            return createErrorResponse("Not in a game", 
                ErrorResponse.INVALID_STATE);
        }

        if (gameSession.getGameModel().getCurrentPhase() != GamePhase.BUILDING) {
            return createErrorResponse("Not in building phase", 
                ErrorResponse.INVALID_STATE);
        }

        // Check if timer system is enabled for this level
        GameLevel level = gameSession.getGameModel().getGameLevel();
        if (!supportsTimerSystem(level)) {
            return createErrorResponse("Timer system not available for " + level, 
                ErrorResponse.INVALID_STATE);
        }

        PlayerId playerId = context.getPlayerId();
        Player player = gameSession.getPlayer(playerId);

        // Check if player has finished their ship (for second timer flip)
        boolean playerHasFinishedShip = isPlayerShipFinished(player);

        // Attempt to flip the timer
        BuildingTimer timer = gameSession.getGameModel().getBuildingTimer();
        BuildingTimer.FlipResult result = timer.flipTimer(playerId.toString(), playerHasFinishedShip);

        switch (result) {
            case SUCCESS -> {
                // Model operation will fire the event automatically
                return new GenericSuccessResponse(getCorrelationId());
            }

            case TIMER_NOT_EXPIRED -> {
                return createErrorResponse(
                    "Cannot flip timer - current stage has " + (timer.getTimeRemaining() / 1000) + " seconds remaining",
                    ErrorResponse.INVALID_STATE
                );
            }

            case PLAYER_NOT_FINISHED -> {
                return createErrorResponse(
                    "Cannot end building phase - you must finish your ship first",
                    ErrorResponse.INVALID_STATE
                );
            }

            case BUILDING_ALREADY_ENDED -> {
                return createErrorResponse("Building phase has already ended", 
                    ErrorResponse.INVALID_STATE);
            }

            default -> {
                return createErrorResponse("Cannot flip timer in current stage", 
                    ErrorResponse.INVALID_STATE);
            }
        }
    }

    private boolean supportsTimerSystem(GameLevel level) {
        return level != GameLevel.TEST_FLIGHT;
    }

    private boolean isPlayerShipFinished(Player player) {
        // Check if player has validated their ship and marked themselves as ready
        if (player.isReady()) {
            return true;
        }

        // Alternative: Check if ship meets minimum requirements
        Ship ship = player.getShip();
        if (ship == null) {
            return false;
        }

        ship.updateStats();

        // Minimum requirements to be considered "finished"
        return ship.getEngines() >= 1 &&
               ship.getCrew() >= 2 &&
               ship.isStructurallyValid();
    }
}