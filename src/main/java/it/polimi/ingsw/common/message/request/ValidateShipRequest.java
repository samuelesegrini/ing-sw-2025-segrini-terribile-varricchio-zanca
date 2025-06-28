package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.ValidateShipResponse;
import it.polimi.ingsw.common.message.event.ShipValidationEvent;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.ShipValidationService;
import it.polimi.ingsw.server.model.domain.general.GameModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Request to validate the ship construction.
 */
public class ValidateShipRequest extends AbstractRequest {
    @Override
    public Response execute(RequestContext context) {
        GameSession session = context.getGameSession();
        if (session == null) {
            return createErrorResponse("Not in a game", ErrorResponse.INVALID_STATE);
        }
        PlayerId playerId = context.getPlayerId();
        Player player = session.getPlayer(playerId);
        Ship ship = player.getShip();

        // ENHANCED: Use comprehensive validation from ShipValidationService
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Get current game phase for validation context
        var currentPhase = session.getCurrentPhase();
        
        // Use the comprehensive Galaxy Trucker validation system
        ShipValidationService.ValidationResult validationResult = 
            ShipValidationService.validateGalaxyTruckerRules(ship, currentPhase);
        
        errors.addAll(validationResult.getErrors());
        warnings.addAll(validationResult.getWarnings());

        // Mark player as ready if validation passes
        if (errors.isEmpty()) {
            session.setPlayerReady(playerId, true);
        }
        
        // Combine errors and warnings for comprehensive feedback
        List<String> allFeedback = new ArrayList<>();
        allFeedback.addAll(errors);
        if (!warnings.isEmpty()) {
            if (!errors.isEmpty()) {
                allFeedback.add("--- Additional Warnings ---");
            }
            allFeedback.addAll(warnings);
        }
        
        // Publish event FIRST - this is the single source of truth for state updates
        ShipValidationEvent event = new ShipValidationEvent(
                session.getGameId(),
                playerId.toString(),
                context.getPlayerRegistry().getPlayerNickname(playerId),
                errors.isEmpty(),
                allFeedback,
                player,                // Full Player model
                session.getGameModel() // Full GameModel
        );
        context.publishEvent(event);

        // Return lightweight response - event contains the state update
        return new ValidateShipResponse(getCorrelationId());
    }
}
