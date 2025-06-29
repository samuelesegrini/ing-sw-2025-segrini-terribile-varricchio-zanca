package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.JoinGameResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.List;
import java.util.logging.Logger;

/**
 * Request to join an existing game.
 */
public class JoinGameRequest extends AbstractRequest {
    private static final Logger LOGGER = Logger.getLogger(JoinGameRequest.class.getName());
    private final String gameId;

    public JoinGameRequest(String gameId) {
        super();
        this.gameId = gameId;
    }

    @Override
    public ValidationResult validate() {
        if (gameId == null || gameId.trim().isEmpty()) {
            return ValidationResult.failure("Game ID is required", "gameId");
        }
        return ValidationResult.success();
    }

    @Override
    public Response execute(RequestContext context) {
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), ErrorResponse.VALIDATION_ERROR);
        }

        // Check authentication
        PlayerId playerId = context.getPlayerId();
        if (playerId == null) {
            return createErrorResponse("Authentication required", ErrorResponse.AUTHENTICATION_ERROR);
        }

        GameSessionManager sessionManager = context.getSessionManager();
        PlayerSessionRegistry registry = context.getPlayerRegistry();

        // Try to join the game
        GameSession gameSession = sessionManager.getGameSession(gameId);
        if (gameSession == null) {
            LOGGER.warning("OIN GAME FAILED - Game not found: " + gameId);
            
            LOGGER.info("RECOVERY - Game not found, but PropertyChange system will handle state synchronization");
            
            return createErrorResponse("Game not found - games list updated", ErrorResponse.NOT_FOUND);
        }

        if (!gameSession.canJoin()) {
            return createErrorResponse("Game is full or already started", ErrorResponse.INVALID_STATE);
        }


        boolean joined = sessionManager.joinGame(gameId, playerId);
        if (!joined) {
            return createErrorResponse("Failed to join game", ErrorResponse.INTERNAL_ERROR);
        }

        // Get player info
        String playerNickname = registry.getPlayerNickname(playerId);

        //TODO: capire come cavolo gestire richieste e messaggi e listeners
        // per il resto fa cose giuste
        // POTENZIALE GENERIC RESPONSE
        // INTERESSANTE USO DI createSuccessResponse()

        // Return lightweight response - event contains the state update
        JoinGameResponse response = new JoinGameResponse(getCorrelationId());
        return response;
    }

}
