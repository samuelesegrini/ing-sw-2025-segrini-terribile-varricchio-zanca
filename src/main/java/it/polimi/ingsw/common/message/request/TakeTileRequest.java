package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.enums.GamePhase;

import java.util.Optional;

/**
 * Request to take a random, face-down component tile from the communal pile.
 */
public class TakeTileRequest extends AbstractRequest {

    /**
     *
     * @param context The execution context providing access to server resources
     * @return an error or a success reponse
     */
    @Override
    public Response execute(RequestContext context) {
        GameSession session = context.getGameSession();
        if (session == null) {
            return createErrorResponse("Not in a game", ErrorResponse.INVALID_STATE);
        }
        if (session.getCurrentPhase() != GamePhase.BUILDING) {
            return createErrorResponse("Not in building phase", ErrorResponse.INVALID_STATE);
        }

        // Use GameSession method which handles both business logic and event firing
        boolean success = session.getGameModel().takeComponent(context.getPlayerId());
        if (!success) {
            return createErrorResponse("Failed to take component - deck may be empty", ErrorResponse.INVALID_STATE);
        }

        return createSuccessResponse();
    }
}