package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.ReconnectResponse;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.common.message.event.PlayerReconnectedEvent;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

/**
 * Request to reconnect to an existing session.
 */
public class ReconnectRequest extends AbstractRequest {
    private final String playerId;
    private final String sessionToken;

    /**
     * constructor
     *
     * @param playerId the player ID
     * @param sessionToken the session token
     */

    public ReconnectRequest(String playerId, String sessionToken) {
        super();
        this.playerId = playerId;
        this.sessionToken = sessionToken;
    }

    /**
     *
     * @return an error message or a success
     */

    @Override
    public ValidationResult validate() {
        if (playerId == null || playerId.trim().isEmpty()) {
            return ValidationResult.failure("Player ID is required", "playerId");
        }
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return ValidationResult.failure("Session token is required", "sessionToken");
        }
        return ValidationResult.success();
    }

    /**
     *
     * @param context The execution context providing access to server resources
     * @return an errorResponse or a ReconnectResponse
     */

    @Override
    public Response execute(RequestContext context) {
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), ErrorResponse.VALIDATION_ERROR);
        }

        PlayerSessionRegistry registry = context.getPlayerRegistry();

        // Validate reconnection
        if (!registry.validateReconnection(playerId, sessionToken)) {
            return createErrorResponse(
                    "Invalid reconnection credentials",
                    ErrorResponse.AUTHENTICATION_ERROR
            );
        }

        // Restore session
        String nickname = registry.getPlayerNickname(playerId);
        registry.restoreSession(context.getSenderId(), playerId);

        // Get current game if any
        GameSession gameSession = context.getSessionManager().getGameSessionForPlayer(playerId);
        String gameId = gameSession != null ? gameSession.getGameId() : null;

        // Publish reconnection event if player was in a game
        //TODO: eventi con listener
        if (gameSession != null) {
            boolean wasInActiveGame = gameSession.isStarted();
            PlayerReconnectedEvent event = new PlayerReconnectedEvent(
                    gameId,
                    PlayerId.fromString(playerId),
                    nickname,
                    wasInActiveGame
            );
            // Model operation will fire the event automatically
        }

        return new ReconnectResponse(
                getCorrelationId(),
                playerId.toString(),
                nickname,
                gameId,
                null // Client state is managed on client side, not passed from server
        );
    }
}