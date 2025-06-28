package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.CreateGameResponse;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.GameLevel;

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
        // Validate
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), ErrorResponse.VALIDATION_ERROR);
        }

        // Check authentication
        PlayerId playerId = context.getPlayerId();
        if (playerId == null) {
            return createErrorResponse("Authentication required", ErrorResponse.AUTHENTICATION_ERROR);
        }

        // Check if already in a game
        if (context.getGameSession() != null) {
            return createErrorResponse("Already in a game", ErrorResponse.INVALID_STATE);
        }

        GameSessionManager sessionManager = context.getSessionManager();
        PlayerSessionRegistry registry = context.getPlayerRegistry();

        // Create the game
        String gameId = sessionManager.createGame(playerId, maxPlayers, gameLevel, gameName);

        if (gameId == null) {
            return createErrorResponse("Failed to create game", ErrorResponse.INTERNAL_ERROR);
        }

        //TODO: the fuckkkkk

        // Get player info
        String creatorNickname = registry.getPlayerNickname(playerId);

        // Model operation will fire the event automatically

        // Return gameId
        CreateGameResponse response = new CreateGameResponse(getCorrelationId(), gameId);
        return response;
    }
}
