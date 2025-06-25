package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.common.message.event.PlayerJoinedGameEvent;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.JoinGameResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.common.message.event.GameLobbyUpdateEvent;
import it.polimi.ingsw.common.message.event.GamesListUpdateEvent;

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

        // Broadcast updated games list to all clients in main lobby
        LOGGER.info("📢 GAMES LIST UPDATE - Broadcasting updated available games list to all lobby clients");
        publishGamesListUpdateEvent(context, sessionManager, registry);

        // Use server GameModel directly - Simple Direct Model Architecture
        GameModel gameModel = gameSession.getGameModel();
        LOGGER.fine("🎮 GAME MODEL - Retrieved GameModel directly from session for response");

        LOGGER.info("🎉 JOIN GAME SUCCESS - Returning JoinGameResponse for player: " + playerNickname + 
                   " (" + playerId + ") joined game: " + gameId);
        return new JoinGameResponse(getCorrelationId(), gameModel);
    }

    /**
     * Publishes a lobby update event to synchronize all clients with current lobby state.
     */
    private void publishLobbyUpdateEvent(RequestContext context, GameSession gameSession, 
                                       String gameId, PlayerSessionRegistry registry, String excludePlayerId) {
        LOGGER.fine("🔄 LOBBY UPDATE EVENT - Getting players from GameModel");
        
        // Use server model directly - Simple Direct Model Architecture
        List<Player> players = gameSession.getGameModel().getPlayers();
        
        // Create and publish the lobby update event (excluding the requesting client)
        LOGGER.fine("📤 LOBBY EVENT - Creating GameLobbyUpdateEvent for " + players.size() + 
                   " players, excluding: " + excludePlayerId);
        GameLobbyUpdateEvent lobbyEvent = new GameLobbyUpdateEvent(
                gameId, players, gameSession.getMaxPlayers(), excludePlayerId
        );
        context.publishEvent(lobbyEvent);
        LOGGER.fine("✅ LOBBY EVENT PUBLISHED - GameLobbyUpdateEvent sent to event system");
    }

    /**
     * Publishes a games list update event to broadcast current available games to all lobby clients.
     */
    private void publishGamesListUpdateEvent(RequestContext context, GameSessionManager sessionManager, 
                                           PlayerSessionRegistry registry) {
        LOGGER.fine("🔄 GAMES LIST UPDATE - Getting current available games from session manager");
        
        // Get the current list of available games - use GameModel directly
        List<GameModel> availableGames = sessionManager.getAvailableGameModels();
        
        LOGGER.fine("📤 GAMES LIST EVENT - Creating GamesListUpdateEvent for " + availableGames.size() + " available games");
        
        // Create and publish the games list update event
        GamesListUpdateEvent gamesListEvent = new GamesListUpdateEvent(availableGames);
        context.publishEvent(gamesListEvent);
        
        LOGGER.fine("✅ GAMES LIST EVENT PUBLISHED - GamesListUpdateEvent sent to event system");
    }
}
