package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.GameInfo;
import it.polimi.ingsw.common.message.event.GameCreatedEvent;
import it.polimi.ingsw.common.message.response.CreateGameResponse;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.model.enums.GameLevel;

import java.util.List;
import java.util.logging.Logger;

/**
 * Request to create a new game.
 */
public class CreateGameRequest extends AbstractRequest {
    private static final Logger LOGGER = Logger.getLogger(CreateGameRequest.class.getName());
    private final int maxPlayers;
    private final GameLevel gameLevel;
    private final String gameName;

    public CreateGameRequest(int maxPlayers, GameLevel gameLevel, String gameName) {
        super();
        this.maxPlayers = maxPlayers;
        this.gameLevel = gameLevel;
        this.gameName = gameName;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public GameLevel getGameLevel() {
        return gameLevel;
    }

    public String getGameName() {
        return gameName;
    }

    @Override
    public ValidationResult validate() {
        if (maxPlayers < 2 || maxPlayers > 4) {
            return ValidationResult.failure("Max players must be between 2 and 4", "maxPlayers");
        }

        if (gameLevel == null) {
            return ValidationResult.failure("Game level is required", "gameLevel");
        }

        if (gameName != null && gameName.length() > 50) {
            return ValidationResult.failure("Game name too long (max 50 characters)", "gameName");
        }

        return ValidationResult.success();
    }

    @Override
    public Response execute(RequestContext context) {
        LOGGER.info("🎮 CREATE GAME REQUEST - Starting game creation for maxPlayers: " + maxPlayers + 
                   ", level: " + gameLevel + ", name: '" + (gameName != null ? gameName : "unnamed") + 
                   "' by client: " + context.getSenderId());
        
        // Validate
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            LOGGER.warning("❌ CREATE GAME FAILED - Validation error: " + validation.getErrorMessage());
            return createErrorResponse(validation.getErrorMessage(), ErrorResponse.VALIDATION_ERROR);
        }
        LOGGER.fine("✅ GAME VALIDATION - Game parameters passed validation");

        // Check authentication
        String playerId = context.getPlayerId();
        if (playerId == null) {
            LOGGER.warning("❌ CREATE GAME FAILED - Client " + context.getSenderId() + " is not authenticated");
            return createErrorResponse("Authentication required", ErrorResponse.AUTHENTICATION_ERROR);
        }
        LOGGER.fine("✅ AUTH CHECK - Player " + playerId + " is authenticated");

        // Check if already in a game
        if (context.getGameSession() != null) {
            LOGGER.warning("❌ CREATE GAME FAILED - Player " + playerId + " is already in a game");
            return createErrorResponse("Already in a game", ErrorResponse.INVALID_STATE);
        }
        LOGGER.fine("✅ GAME STATE - Player " + playerId + " is not in any game");

        GameSessionManager sessionManager = context.getSessionManager();
        PlayerSessionRegistry registry = context.getPlayerRegistry();
        LOGGER.fine("🔧 SERVICES - Retrieved session manager and player registry");

        // Create the game
        LOGGER.info("🏗️ GAME CREATION - Calling sessionManager.createGame() for player: " + playerId);
        String gameId = sessionManager.createGame(playerId, maxPlayers, gameLevel, gameName);

        if (gameId == null) {
            LOGGER.severe("❌ CREATE GAME FAILED - SessionManager returned null gameId for player: " + playerId);
            return createErrorResponse("Failed to create game", ErrorResponse.INTERNAL_ERROR);
        }
        LOGGER.info("✅ GAME CREATED - Successfully created game with ID: " + gameId + " for player: " + playerId);

        // Get player info
        String creatorNickname = registry.getPlayerNickname(playerId);
        LOGGER.fine("👤 CREATOR INFO - Retrieved creator nickname: '" + creatorNickname + "' for player: " + playerId);

        // Publish game created event
        GameCreatedEvent event = new GameCreatedEvent(
                gameId, playerId, creatorNickname, maxPlayers, gameLevel, gameName
        );
        LOGGER.info("📢 EVENT PUBLISH - Publishing GameCreatedEvent for game: " + gameId + 
                   " created by: " + creatorNickname + " (" + playerId + ")");
        context.publishEvent(event);
        LOGGER.fine("✅ EVENT PUBLISHED - GameCreatedEvent sent to event system");

        LOGGER.info("🎉 CREATE GAME SUCCESS - Returning CreateGameResponse for game: " + gameId + 
                   " to creator: " + creatorNickname + " (" + playerId + ")");
        return new CreateGameResponse(getCorrelationId(), gameId, gameName, maxPlayers, gameLevel);
    }

}
