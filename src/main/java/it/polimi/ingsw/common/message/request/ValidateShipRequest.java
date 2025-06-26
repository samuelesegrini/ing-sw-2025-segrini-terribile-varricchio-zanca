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

        // Publish validation event
        ShipValidationEvent event = new ShipValidationEvent(
                session.getGameId(),
                playerId.toString(),
                context.getPlayerRegistry().getPlayerNickname(playerId),
                errors.isEmpty(),
                errors
        );
        context.publishEvent(event);

        if (errors.isEmpty()) {
            // Mark player as ready
            session.setPlayerReady(playerId, true);
            
            // ENHANCED: Include warnings in success response for player feedback
            List<String> feedbackMessages = new ArrayList<>();
            if (!warnings.isEmpty()) {
                feedbackMessages.add("Ship validated successfully with " + warnings.size() + " optimization suggestions:");
                feedbackMessages.addAll(warnings);
            } else {
                feedbackMessages.add("Ship validated successfully - excellent construction!");
            }
            
            return new ValidateShipResponse(getCorrelationId(), true, feedbackMessages);
        } else {
            // ENHANCED: Combine errors and warnings for comprehensive feedback
            List<String> allFeedback = new ArrayList<>();
            allFeedback.addAll(errors);
            if (!warnings.isEmpty()) {
                allFeedback.add("--- Additional Warnings ---");
                allFeedback.addAll(warnings);
            }
            
            return new ValidateShipResponse(getCorrelationId(), false, allFeedback);
        }
    }
}
