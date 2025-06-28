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
        LOGGER.info("🚪 JOIN GAME REQUEST - Player attempting to join game: " + gameId + " from client: " + context.getSenderId());
        
        // Debug: Get current available games to compare
        List<it.polimi.ingsw.server.model.domain.general.GameModel> availableGames = context.getSessionManager().getAvailableGames();
        LOGGER.info("🎮 AVAILABLE GAMES ON SERVER: " + availableGames.size());
        for (it.polimi.ingsw.server.model.domain.general.GameModel game : availableGames) {
            LOGGER.info("  - Game ID: " + game.getGameId() + ", Name: " + game.getGameName());
        }
        
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            LOGGER.warning("❌ JOIN GAME FAILED - Validation error for gameId '" + gameId + "': " + validation.getErrorMessage());
            return createErrorResponse(validation.getErrorMessage(), ErrorResponse.VALIDATION_ERROR);
        }
        LOGGER.fine("✅ JOIN VALIDATION - GameId '" + gameId + "' passed validation");

        // Check authentication
        PlayerId playerId = context.getPlayerId();
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
            
            LOGGER.info("📤 RECOVERY - Game not found, but PropertyChange system will handle state synchronization");
            
            return createErrorResponse("Game not found - games list updated", ErrorResponse.NOT_FOUND);
        }
        LOGGER.fine("✅ GAME FOUND - Game " + gameId + " exists");

        if (!gameSession.canJoin()) {
            LOGGER.warning("❌ JOIN GAME FAILED - Game " + gameId + " is full or already started (current players: " + 
                          gameSession.getPlayerCount() + "/" + gameSession.getMaxPlayers() + ", started: " + gameSession.isStarted() + ")");
            return createErrorResponse("Game is full or already started", ErrorResponse.INVALID_STATE);
        }
        LOGGER.fine("✅ GAME JOINABLE - Game " + gameId + " can accept new players");

        LOGGER.info("🚪 JOINING GAME - Calling sessionManager.joinGame() for player: " + playerId + " to game: " + gameId);
        LOGGER.info("🔍 BEFORE JOIN - Available games in SessionManager:");
        for (it.polimi.ingsw.server.model.domain.general.GameModel game : sessionManager.getAvailableGames()) {
            LOGGER.info("  - Available: " + game.getGameId() + " (Name: " + game.getGameName() + ")");
        }
        boolean joined = sessionManager.joinGame(gameId, playerId);
        if (!joined) {
            LOGGER.severe("❌ JOIN GAME FAILED - SessionManager.joinGame() returned false for player: " + playerId + " and game: " + gameId);
            return createErrorResponse("Failed to join game", ErrorResponse.INTERNAL_ERROR);
        }
        LOGGER.info("✅ PLAYER JOINED - Player " + playerId + " successfully joined game: " + gameId);

        // Get player info
        String playerNickname = registry.getPlayerNickname(playerId);
        LOGGER.fine("👤 PLAYER INFO - Retrieved nickname: '" + playerNickname + "' for player: " + playerId);

        LOGGER.info("📢 PROPERTY CHANGE - Player join will trigger PlayerJoinedGameEvent via PropertyChange system for game: " + gameId);

        // Return lightweight response - event contains the state update
        JoinGameResponse response = new JoinGameResponse(getCorrelationId());
        LOGGER.info("🎉 JOIN GAME SUCCESS - Player: " + playerNickname + 
                   " (" + playerId + ") joined game: " + gameId + " - returning lightweight acknowledgment");
        return response;
    }

}
