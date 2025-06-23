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
import it.polimi.ingsw.common.message.event.GameLobbyUpdateEvent;

import java.util.ArrayList;
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
        LOGGER.info("🚪 JOIN GAME REQUEST - Player attempting to join game: " + gameId + " from client: " + context.getSenderId());
        
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            LOGGER.warning("❌ JOIN GAME FAILED - Validation error for gameId '" + gameId + "': " + validation.getErrorMessage());
            return createErrorResponse(validation.getErrorMessage(), ErrorResponse.VALIDATION_ERROR);
        }
        LOGGER.fine("✅ JOIN VALIDATION - GameId '" + gameId + "' passed validation");

        // Check authentication
        String playerId = context.getPlayerId();
        if (playerId == null) {
            LOGGER.warning("❌ JOIN GAME FAILED - Client " + context.getSenderId() + " is not authenticated");
            return createErrorResponse("Authentication required", ErrorResponse.AUTHENTICATION_ERROR);
        }
        LOGGER.fine("✅ AUTH CHECK - Player " + playerId + " is authenticated");

        GameSessionManager sessionManager = context.getSessionManager();
        PlayerSessionRegistry registry = context.getPlayerRegistry();
        LOGGER.fine("🔧 SERVICES - Retrieved session manager and player registry");

        // Try to join the game
        GameSession gameSession = sessionManager.getGameSession(gameId);
        if (gameSession == null) {
            LOGGER.warning("❌ JOIN GAME FAILED - Game not found: " + gameId);
            return createErrorResponse("Game not found", ErrorResponse.NOT_FOUND);
        }
        LOGGER.fine("✅ GAME FOUND - Game " + gameId + " exists");

        if (!gameSession.canJoin()) {
            LOGGER.warning("❌ JOIN GAME FAILED - Game " + gameId + " is full or already started (current players: " + 
                          gameSession.getPlayerCount() + "/" + gameSession.getMaxPlayers() + ", started: " + gameSession.isStarted() + ")");
            return createErrorResponse("Game is full or already started", ErrorResponse.INVALID_STATE);
        }
        LOGGER.fine("✅ GAME JOINABLE - Game " + gameId + " can accept new players");

        LOGGER.info("🚪 JOINING GAME - Calling sessionManager.joinGame() for player: " + playerId + " to game: " + gameId);
        boolean joined = sessionManager.joinGame(gameId, playerId);
        if (!joined) {
            LOGGER.severe("❌ JOIN GAME FAILED - SessionManager.joinGame() returned false for player: " + playerId + " and game: " + gameId);
            return createErrorResponse("Failed to join game", ErrorResponse.INTERNAL_ERROR);
        }
        LOGGER.info("✅ PLAYER JOINED - Player " + playerId + " successfully joined game: " + gameId);

        // Get player info
        String playerNickname = registry.getPlayerNickname(playerId);
        LOGGER.fine("👤 PLAYER INFO - Retrieved nickname: '" + playerNickname + "' for player: " + playerId);

        // Publish player joined event
        PlayerJoinedGameEvent event = new PlayerJoinedGameEvent(
                gameId, playerId, playerNickname, gameSession.getPlayerCount()
        );
        LOGGER.info("📢 EVENT PUBLISH - Publishing PlayerJoinedGameEvent for player: " + playerNickname + 
                   " (" + playerId + ") in game: " + gameId + " (total players: " + gameSession.getPlayerCount() + ")");
        context.publishEvent(event);
        LOGGER.fine("✅ EVENT PUBLISHED - PlayerJoinedGameEvent sent to event system");

        // Publish lobby update event to other clients (exclude requester since they get the response)
        LOGGER.info("📢 LOBBY UPDATE - Publishing GameLobbyUpdateEvent for game: " + gameId + " (excluding requester: " + playerId + ")");
        publishLobbyUpdateEvent(context, gameSession, gameId, registry, playerId);

        // Manually construct PlayerInfo list to include correct nicknames from the registry
        List<PlayerInfo> playerInfos = new ArrayList<>();
        LOGGER.fine("📋 RESPONSE PREP - Building player info list for response");
        for (String pId : gameSession.getPlayerIds()) {
            String pNickname = registry.getPlayerNickname(pId);
            // Use the actual ready status from PlayerState
            boolean isReady = gameSession.getPlayerState(pId) != null && gameSession.getPlayerState(pId).isReady();
            playerInfos.add(new PlayerInfo(pId, pNickname, isReady));
            LOGGER.fine("  📌 Player: " + pNickname + " (" + pId + "), ready: " + isReady);
        }

        // Get game info for response
        GameInfo gameInfo = new GameInfo(
                gameSession.getGameId(),
                gameSession.getGameName(),
                gameSession.getCreatorId(),
                gameSession.getMaxPlayers(),
                gameSession.getPlayerCount(),
                gameSession.getGameLevel(),
                playerInfos
        );
        LOGGER.fine("🎮 GAME INFO - Created GameInfo with " + playerInfos.size() + " players for response");

        LOGGER.info("🎉 JOIN GAME SUCCESS - Returning JoinGameResponse for player: " + playerNickname + 
                   " (" + playerId + ") joined game: " + gameId);
        return new JoinGameResponse(getCorrelationId(), gameInfo);
    }

    /**
     * Publishes a lobby update event to synchronize all clients with current lobby state.
     */
    private void publishLobbyUpdateEvent(RequestContext context, GameSession gameSession, 
                                       String gameId, PlayerSessionRegistry registry, String excludePlayerId) {
        LOGGER.fine("🔄 LOBBY UPDATE EVENT - Building player list for GameLobbyUpdateEvent");
        List<PlayerInfo> playerInfos = new ArrayList<>();
        
        // Build the player info list with current ready states
        for (String pId : gameSession.getPlayerIds()) {
            String pNickname = registry.getPlayerNickname(pId);
            boolean isReady = gameSession.getPlayerState(pId) != null && 
                             gameSession.getPlayerState(pId).isReady();
            playerInfos.add(new PlayerInfo(pId, pNickname, isReady));
            LOGGER.fine("  📌 Lobby Player: " + pNickname + " (" + pId + "), ready: " + isReady);
        }
        
        // Create and publish the lobby update event (excluding the requesting client)
        LOGGER.fine("📤 LOBBY EVENT - Creating GameLobbyUpdateEvent for " + playerInfos.size() + 
                   " players, excluding: " + excludePlayerId);
        GameLobbyUpdateEvent lobbyEvent = new GameLobbyUpdateEvent(
                gameId, playerInfos, gameSession.getMaxPlayers(), excludePlayerId
        );
        context.publishEvent(lobbyEvent);
        LOGGER.fine("✅ LOBBY EVENT PUBLISHED - GameLobbyUpdateEvent sent to event system");
    }
}
