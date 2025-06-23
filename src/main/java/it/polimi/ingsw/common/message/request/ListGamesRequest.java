package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.GameInfo;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.ListGamesResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.server.core.GameSessionManager;

import java.util.List;

/**
 * Request to get list of available games.
 */
public class ListGamesRequest extends AbstractRequest {

    @Override
    public Response execute(RequestContext context) {
        // Check authentication
        if (context.getPlayerId() == null) {
            return createErrorResponse("Authentication required", ErrorResponse.AUTHENTICATION_ERROR);
        }

        GameSessionManager sessionManager = context.getSessionManager();
        List<GameInfo> games = sessionManager.getAvailableGames();
        return new ListGamesResponse(getCorrelationId(), games);    }
}