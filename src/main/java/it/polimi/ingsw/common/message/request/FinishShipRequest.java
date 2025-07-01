package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.FinishShipResponse;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.ShipValidationService;
import it.polimi.ingsw.server.model.domain.ship.UnifiedShipValidationService;
import it.polimi.ingsw.server.model.enums.GamePhase;

import java.util.logging.Logger;

/**
 * Request sent by a player to finish ship building.
 * This triggers ship validation, component correction if needed, 
 * and starting position selection according to Galaxy Trucker rules.
 */
public class FinishShipRequest extends AbstractRequest {
    private static final Logger LOGGER = Logger.getLogger(FinishShipRequest.class.getName());

    /**
     * Creates a new finish ship request.
     */
    public FinishShipRequest() {
        super();
    }

    @Override
    public ValidationResult validate() {
        // No validation needed for finish ship request
        return ValidationResult.success();
    }

    @Override
    public Response execute(RequestContext context) {
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), ErrorResponse.VALIDATION_ERROR);
        }

        // Get game session
        GameSession session = context.getGameSession();
        if (session == null) {
            return createErrorResponse("Not in a game", ErrorResponse.INVALID_STATE);
        }

        // Check phase
        if (session.getGameModel().getCurrentPhase() != GamePhase.BUILDING) {
            return createErrorResponse("Not in building phase", ErrorResponse.INVALID_STATE);
        }

        // Get player
        PlayerId playerId = context.getPlayerId();
        Player player = session.getPlayer(playerId);
        if (player == null) {
            return createErrorResponse("Player not found", ErrorResponse.INTERNAL_ERROR);
        }

        // Check if player already finished
        if (player.isShipFinished()) {
            return createErrorResponse("Ship already finished", ErrorResponse.INVALID_STATE);
        }

        try {
            // Execute ship finishing process
            FinishShipResponse.ShipFinishResult result = session.finishPlayerShip(playerId);
            
            if (result.isSuccess()) {
                LOGGER.info("Player " + playerId.getNickname() + " successfully finished ship building");
                return new FinishShipResponse(getCorrelationId(), true, result);
            } else {
                LOGGER.warning("Failed to finish ship for player " + playerId.getNickname() + ": " + result.getErrorMessage());
                return new FinishShipResponse(getCorrelationId(), false, result);
            }

        } catch (Exception e) {
            LOGGER.severe("Error finishing ship for player " + playerId.getNickname() + ": " + e.getMessage());
            return createErrorResponse("Internal error during ship finishing", ErrorResponse.INTERNAL_ERROR);
        }
    }
}