package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;

import java.util.logging.Logger;

/**
 * Request sent by a game creator to start the game.
 */
public class StartGameRequest extends AbstractRequest {
    private static final Logger LOGGER = Logger.getLogger(StartGameRequest.class.getName());

    private final String gameId;

    /**
     * constructor
     *
     * @param gameId the game ID
     */

    public StartGameRequest(String gameId) {
        super();
        this.gameId = gameId;
    }

    /**
     *
     * @return the game ID
     */

    public String getGameId() {
        return gameId;
    }

    /**
     *
     * @return whether the start game request succedeed or not
     */

    @Override
    public ValidationResult validate() {
        if (gameId == null || gameId.trim().isEmpty()) {
            return ValidationResult.failure("Game ID cannot be null or empty");
        }
        return ValidationResult.success();
    }

    /**
     *
     * @param context The execution context providing access to server resources
     * @return an error message or a success response
     */

    @Override
    public Response execute(RequestContext context) {
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), "VALIDATION_ERROR");
        }

        PlayerId playerId = context.getPlayerId();
        if (playerId == null) {
            return createErrorResponse("Authentication required", "AUTHENTICATION_ERROR");
        }

        GameSessionManager sessionManager = context.getSessionManager();
        PlayerSessionRegistry registry = context.getPlayerRegistry();

        GameSession gameSession = sessionManager.getGameSession(gameId);
        if (gameSession == null) {
            return createErrorResponse("Game not found", "NOT_FOUND");
        }

        // Check if player is the creator (host)
        if (!gameSession.isCreator(playerId)) {
            return createErrorResponse("Only the game creator can start the game", "UNAUTHORIZED");
        }

        if (gameSession.isStarted()) {
            return createErrorResponse("Game has already started", "INVALID_STATE");
        }

        // Check minimum player count first
        if (gameSession.getPlayerCount() < 2) {
            return createErrorResponse("Need at least 2 players to start the game", "INVALID_STATE");
        }
        
        // Then check if all players are ready
        if (!gameSession.areAllPlayersReady()) {
            return createErrorResponse("Not all players are ready", "INVALID_STATE");
        }

        // Start the game
        LOGGER.info("DEBUG: StartGameRequest.execute() - About to call gameSession.startGame()");
        boolean started = gameSession.startGame();
        LOGGER.info("DEBUG: StartGameRequest.execute() - gameSession.startGame() returned: " + started);
        
        if (!started) {
            LOGGER.warning("DEBUG: StartGameRequest.execute() - Game failed to start, returning error response");
            return createErrorResponse("Failed to start game", "INTERNAL_ERROR");
        }

        LOGGER.info("DEBUG: StartGameRequest.execute() - Game started successfully, returning success response");
        // Model operation will fire the event automatically
        return createSuccessResponse();
    }
} 