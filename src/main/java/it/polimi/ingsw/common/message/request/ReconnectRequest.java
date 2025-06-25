package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.ReconnectResponse;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.common.message.event.PlayerReconnectedEvent;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;

/**
 * Request to reconnect to an existing session.
 */
public class ReconnectRequest extends AbstractRequest {
    private final String playerId;
    private final String sessionToken;

    public ReconnectRequest(String playerId, String sessionToken) {
        super();
        this.playerId = playerId;
        this.sessionToken = sessionToken;
    }

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
        if (gameSession != null) {
            boolean wasInActiveGame = gameSession.isStarted();
            PlayerReconnectedEvent event = new PlayerReconnectedEvent(
                    gameId,
                    playerId,
                    nickname,
                    wasInActiveGame
            );
            context.getEventPublisher().publishEvent(event);
        }

        return new ReconnectResponse(
                getCorrelationId(),
                playerId,
                nickname,
                gameId,
                null // Client state is managed on client side, not passed from server
        );
    }
}