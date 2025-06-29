package it.polimi.ingsw.common.message.request.flight;

import it.polimi.ingsw.common.message.request.AbstractRequest;
import it.polimi.ingsw.common.message.request.RequestContext;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.PlanetChoiceResponse;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardController;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardState;
import it.polimi.ingsw.server.model.enums.GamePhase;

/**
 * Request from a player to choose a planet during a Planets adventure card.
 * planetIndex = 0 if the player decides not to land on any planet.
 */
public class PlanetChoiceRequest extends AbstractRequest {
    private final int planetIndex;

    public PlanetChoiceRequest(int planetIndex) {
        this.planetIndex = planetIndex;
    }

    public int getPlanetIndex() {
        return planetIndex;
    }

    @Override
    public ValidationResult validate() {
        if (planetIndex < 0) {
            return ValidationResult.failure("Planet index cannot be negative", "planetIndex");
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
            return createErrorResponse("Can only make planet choices during flight phase", 
                                     ErrorResponse.INVALID_STATE);
        }

        try {
            // Get the adventure card controller from the session
            AdventureCardController cardController = session.getAdventureCardController();
            if (cardController == null || !cardController.isProcessingCard()) {
                return createErrorResponse("No adventure card is currently being processed", 
                                         ErrorResponse.INVALID_STATE);
            }

            // Create player choice
            AdventureCardState.PlayerChoice choice = new AdventureCardState.PlayerChoice(
                context.getPlayerId(), 
                AdventureCardState.AdventureChoiceType.PLANET_CHOICE
            );
            choice.setParameter("planetIndex", planetIndex);

            // Submit choice to controller
            boolean success = cardController.handlePlayerChoice(context.getPlayerId(), choice);
            
            if (success) {
                return new PlanetChoiceResponse(getCorrelationId(), planetIndex);
            } else {
                return createErrorResponse("Failed to submit planet choice - not your turn or invalid choice", 
                                         ErrorResponse.INVALID_STATE);
            }
        } catch (Exception e) {
            return createErrorResponse("Failed to process planet choice: " + e.getMessage(), 
                                     ErrorResponse.INTERNAL_ERROR);
        }
    }
}