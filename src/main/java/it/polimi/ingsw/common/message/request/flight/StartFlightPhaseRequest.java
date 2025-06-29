package it.polimi.ingsw.common.message.request.flight;

import it.polimi.ingsw.common.message.request.AbstractRequest;
import it.polimi.ingsw.common.message.request.RequestContext;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.StartFlightPhaseResponse;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.common.message.event.flight.FlightPhaseStartedEvent;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.enums.GamePhase;

/**
 * Request to start the flight phase of the game.
 * This can only be executed when the game is in BUILDING phase.
 */
public class StartFlightPhaseRequest extends AbstractRequest {

    public StartFlightPhaseRequest() {
        super();
    }

    @Override
    public ValidationResult validate() {
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

        // Check if current phase allows flight phase start
        if (session.getCurrentPhase() != GamePhase.BUILDING) {
            return createErrorResponse("Cannot start flight phase from " + session.getCurrentPhase(), 
                                     ErrorResponse.INVALID_STATE);
        }

        try {
            // Transition to flight phase using existing GameSession method
            session.getGameModel().changePhase(GamePhase.FLIGHT);
            
            // Publish flight phase started event
            int playerCount = session.getGameModel().getPlayers().size();
            int routeLength = session.getGameModel().getFlightBoard().getRoute().getLength();
            context.publishEvent(new FlightPhaseStartedEvent(context.getGameId(), playerCount, routeLength));
            
            return new StartFlightPhaseResponse(getCorrelationId());
        } catch (Exception e) {
            return createErrorResponse("Failed to start flight phase: " + e.getMessage(), 
                                     ErrorResponse.INTERNAL_ERROR);
        }
    }
}