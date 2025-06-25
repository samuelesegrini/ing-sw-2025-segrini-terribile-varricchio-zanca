package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.common.message.event.GameStartedEvent;
import it.polimi.ingsw.common.message.event.GamesListUpdateEvent;
import it.polimi.ingsw.common.message.response.GenericSuccessResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;

import java.util.*;
import java.util.logging.Logger;

/**
 * Request sent by a game creator to start the game.
 */
public class StartGameRequest extends AbstractRequest {
    private static final Logger LOGGER = Logger.getLogger(StartGameRequest.class.getName());

    private final String gameId;

    public StartGameRequest(String gameId) {
        super();
        this.gameId = gameId;
    }

    public String getGameId() {
        return gameId;
    }

    @Override
    public ValidationResult validate() {
        if (gameId == null || gameId.trim().isEmpty()) {
            return ValidationResult.failure("Game ID cannot be null or empty");
        }
        return ValidationResult.success();
    }

    @Override
    public Response execute(RequestContext context) {
        LOGGER.info("🚀 START GAME REQUEST - Attempting to start game: " + gameId + " from client: " + context.getSenderId());
        
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            LOGGER.warning("❌ START GAME FAILED - Validation error for gameId '" + gameId + "': " + validation.getErrorMessage());
            return createErrorResponse(validation.getErrorMessage(), "VALIDATION_ERROR");
        }
        LOGGER.fine("✅ START VALIDATION - GameId '" + gameId + "' passed validation");

        String playerId = context.getPlayerId();
        if (playerId == null) {
            LOGGER.warning("❌ START GAME FAILED - Client " + context.getSenderId() + " is not authenticated");
            return createErrorResponse("Authentication required", "AUTHENTICATION_ERROR");
        }
        LOGGER.fine("✅ AUTH CHECK - Player " + playerId + " is authenticated");

        GameSessionManager sessionManager = context.getSessionManager();
        PlayerSessionRegistry registry = context.getPlayerRegistry();
        LOGGER.fine("🔧 SERVICES - Retrieved session manager and player registry");
        
        GameSession gameSession = sessionManager.getGameSession(gameId);
        if (gameSession == null) {
            LOGGER.warning("❌ START GAME FAILED - Game not found: " + gameId);
            return createErrorResponse("Game not found", "NOT_FOUND");
        }
        LOGGER.fine("✅ GAME FOUND - Game " + gameId + " exists");

        // Check if player is the creator (host)
        if (!gameSession.isCreator(playerId)) {
            LOGGER.warning("❌ START GAME FAILED - Player " + playerId + " is not the creator of game: " + gameId);
            return createErrorResponse("Only the game creator can start the game", "UNAUTHORIZED");
        }
        LOGGER.fine("✅ CREATOR CHECK - Player " + playerId + " is the creator of game: " + gameId);

        if (gameSession.isStarted()) {
            return createErrorResponse("Game has already started", "INVALID_STATE");
        }

        // Check minimum player count first
        if (gameSession.getPlayerCount() < 2) {
            return createErrorResponse("Need at least 2 players to start the game", "INVALID_STATE");
        }
        
        // Then check if all players are ready
        if (!gameSession.areAllPlayersReady()) {
            LOGGER.warning("❌ START GAME FAILED - Not all players are ready in game: " + gameId + 
                          " (players: " + gameSession.getPlayerCount() + ")");
            return createErrorResponse("Not all players are ready", "INVALID_STATE");
        }
        LOGGER.fine("✅ READY CHECK - All players are ready in game: " + gameId);

        // Start the game
        LOGGER.info("🚀 STARTING GAME - Calling gameSession.startGame() for game: " + gameId);
        boolean started = gameSession.startGame();
        if (!started) {
            LOGGER.severe("❌ START GAME FAILED - gameSession.startGame() returned false for game: " + gameId);
            return createErrorResponse("Failed to start game", "INTERNAL_ERROR");
        }
        LOGGER.info("✅ GAME STARTED - Successfully started game: " + gameId);

        // Use server GameModel directly - Simple Direct Model Architecture
        GameModel gameModel = gameSession.getGameModel();

        // Simple Direct Model Architecture - no complex DTO conversion needed
        LOGGER.info("📢 EVENT PUBLISH - Publishing GameStartedEvent for game: " + gameId + 
                   " with " + gameModel.getPlayers().size() + " players entering building phase");
        GameStartedEvent event = new GameStartedEvent(gameId, gameModel);
        context.publishEvent(event);
        LOGGER.fine("✅ EVENT PUBLISHED - GameStartedEvent sent to event system");

        // Broadcast updated games list since game phase changed from SETUP to BUILDING
        publishGamesListUpdateEvent(context, sessionManager);
        LOGGER.fine("✅ GAMES LIST UPDATED - Games list update published after game phase change");

        LOGGER.info("🎉 START GAME SUCCESS - Game " + gameId + " successfully started with " + 
                   gameModel.getPlayers().size() + " players entering building phase");
        return new GenericSuccessResponse(getCorrelationId());
    }

    private void publishGamesListUpdateEvent(RequestContext context, GameSessionManager sessionManager) {
        List<GameModel> availableGames = sessionManager.getAvailableGames();
        GamesListUpdateEvent gamesListEvent = new GamesListUpdateEvent(availableGames);
        context.publishEvent(gamesListEvent);
    }
} 