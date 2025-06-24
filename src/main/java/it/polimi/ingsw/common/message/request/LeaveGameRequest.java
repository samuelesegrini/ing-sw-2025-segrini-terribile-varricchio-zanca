package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.event.GameEndedEvent;
import it.polimi.ingsw.common.message.event.GameLobbyUpdateEvent;
import it.polimi.ingsw.common.message.event.PlayerLeftGameEvent;
import it.polimi.ingsw.common.message.event.GamesListUpdateEvent;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.LeaveGameResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.common.GameInfo;
import it.polimi.ingsw.common.PlayerInfo;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Request from a player to leave the current game lobby.
 */
public class LeaveGameRequest extends AbstractRequest {
    private final String gameId;

    public LeaveGameRequest(String gameId) {
        super();
        this.gameId = gameId;
    }

    public String getGameId() {
        return gameId;
    }

    @Override
    public ValidationResult validate() {
        if (gameId == null || gameId.trim().isEmpty()) {
            return ValidationResult.failure("Game ID is required");
        }
        return ValidationResult.success();
    }

    @Override
    public Response execute(RequestContext context) {
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), "VALIDATION_ERROR");
        }

        // Check authentication
        String playerId = context.getPlayerId();
        if (playerId == null) {
            return createErrorResponse("Authentication required", "AUTHENTICATION_ERROR");
        }

        GameSessionManager sessionManager = context.getSessionManager();
        PlayerSessionRegistry registry = context.getPlayerRegistry();
        
        // Check if game exists
        GameSession gameSession = sessionManager.getGameSession(gameId);
        if (gameSession == null) {
            return createErrorResponse("Game not found", "NOT_FOUND");
        }

        // Leave the game
        boolean left = sessionManager.removePlayerFromGame(gameId, playerId);
        if (!left) {
            return createErrorResponse("Failed to leave game", "INTERNAL_ERROR");
        }

        // Get player info
        String playerNickname = registry.getPlayerNickname(playerId);

        // Publish player left event
        PlayerLeftGameEvent event = new PlayerLeftGameEvent(
                gameId, playerId, playerNickname
        );
        context.publishEvent(event);

        // Check if game should be ended
        if (gameSession.getPlayerCount() == 0) {
            context.publishEvent(new GameEndedEvent(gameId, "All players left", null));
        } else {
            // Update lobby state for remaining players (exclude the leaving player)
            publishLobbyUpdateEvent(context, gameSession, gameId, registry, playerId);
        }

        // Broadcast updated games list to all clients in main lobby
        publishGamesListUpdateEvent(context, sessionManager, registry);

        return new LeaveGameResponse(getCorrelationId());
    }

    /**
     * Publishes a lobby update event to synchronize remaining clients with updated lobby state.
     */
    private void publishLobbyUpdateEvent(RequestContext context, GameSession gameSession, 
                                       String gameId, PlayerSessionRegistry registry, String excludePlayerId) {
        List<PlayerInfo> playerInfos = new ArrayList<>();
        
        // Build the player info list with current ready states
        for (String pId : gameSession.getPlayerIds()) {
            String pNickname = registry.getPlayerNickname(pId);
            boolean isReady = gameSession.getPlayerState(pId) != null && 
                             gameSession.getPlayerState(pId).isReady();
            playerInfos.add(new PlayerInfo(pId, pNickname, isReady));
        }
        
        // Create and publish the lobby update event (excluding the leaving player)
        GameLobbyUpdateEvent lobbyEvent = new GameLobbyUpdateEvent(
                gameId, playerInfos, gameSession.getMaxPlayers(), excludePlayerId
        );
        context.publishEvent(lobbyEvent);
    }

    /**
     * Publishes a games list update event to broadcast current available games to all lobby clients.
     */
    private void publishGamesListUpdateEvent(RequestContext context, GameSessionManager sessionManager, 
                                           PlayerSessionRegistry registry) {
        // Get the current list of available games
        List<GameInfo> availableGames = sessionManager.getAvailableGames();
        
        // Create and publish the games list update event
        GamesListUpdateEvent gamesListEvent = new GamesListUpdateEvent(availableGames);
        context.publishEvent(gamesListEvent);
    }
}
