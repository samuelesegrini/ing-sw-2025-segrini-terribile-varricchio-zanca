package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.GameInfo;
import it.polimi.ingsw.common.message.event.PlayerJoinedGameEvent;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.JoinGameResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.common.PlayerInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Request to join an existing game.
 */
public class JoinGameRequest extends AbstractRequest {
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
        String playerId = context.getPlayerId();
        if (playerId == null) {
            return createErrorResponse("Authentication required", ErrorResponse.AUTHENTICATION_ERROR);
        }

        GameSessionManager sessionManager = context.getSessionManager();
        PlayerSessionRegistry registry = context.getPlayerRegistry();

        // Try to join the game
        GameSession gameSession = sessionManager.getGameSession(gameId);
        if (gameSession == null) {
            return createErrorResponse("Game not found", ErrorResponse.NOT_FOUND);
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

        // Publish player joined event
        PlayerJoinedGameEvent event = new PlayerJoinedGameEvent(
                gameId, playerId, playerNickname, gameSession.getPlayerCount()
        );
        context.publishEvent(event);

        // Manually construct PlayerInfo list to include correct nicknames from the registry
        List<PlayerInfo> playerInfos = new ArrayList<>();
        for (String pId : gameSession.getPlayerIds()) {
            String pNickname = registry.getPlayerNickname(pId);
            // Use the actual ready status from PlayerState
            boolean isReady = gameSession.getPlayerState(pId) != null && gameSession.getPlayerState(pId).isReady();
            playerInfos.add(new PlayerInfo(pId, pNickname, isReady));
        }

        // Get game info for response
        GameInfo gameInfo = new GameInfo(
                gameSession.getGameId(),
                gameSession.getGameName(),
                gameSession.getMaxPlayers(),
                gameSession.getPlayerCount(),
                gameSession.getGameLevel(),
                playerInfos
        );

        return new JoinGameResponse(getCorrelationId(), gameInfo);
    }
}
