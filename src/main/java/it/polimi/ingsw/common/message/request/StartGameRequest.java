package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.PlayerInfo;
import it.polimi.ingsw.common.message.event.GameStartedEvent;
import it.polimi.ingsw.common.message.response.GenericSuccessResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Request sent by a game creator to start the game.
 */
public class StartGameRequest extends AbstractRequest {

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
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), "VALIDATION_ERROR");
        }

        String playerId = context.getPlayerId();
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
        boolean started = gameSession.startGame();
        if (!started) {
            return createErrorResponse("Failed to start game", "INTERNAL_ERROR");
        }

        // Prepare game started event
        List<PlayerInfo> playerInfos = new ArrayList<>();
        for (String pId : gameSession.getPlayerIds()) {
            String pNickname = registry.getPlayerNickname(pId);
            playerInfos.add(new PlayerInfo(pId, pNickname, true));
        }

        // Build full building phase state maps
        java.util.Map<String, java.util.Map<it.polimi.ingsw.server.model.domain.ship.Position, it.polimi.ingsw.server.model.enums.ship.ComponentType>> playerShipGrids = new java.util.HashMap<>();
        java.util.Map<String, List<it.polimi.ingsw.server.model.enums.ship.ComponentType>> playerAvailableTiles = new java.util.HashMap<>();
        java.util.Map<String, List<it.polimi.ingsw.server.model.enums.ship.ComponentType>> playerHeldTiles = new java.util.HashMap<>();
        java.util.Map<String, java.util.Set<it.polimi.ingsw.server.model.domain.ship.Position>> playerForbiddenPositions = new java.util.HashMap<>();
        java.util.Map<String, Long> playerBuildingTimeRemaining = new java.util.HashMap<>();
        java.util.Map<String, Boolean> playerTimerFlipped = new java.util.HashMap<>();
        for (String pId : gameSession.getPlayerIds()) {
            GameSession.ShipBuildingSyncState syncState = gameSession.getShipBuildingSyncState(pId);
            if (syncState != null) {
                playerShipGrids.put(pId, syncState.shipGrid);
                playerAvailableTiles.put(pId, syncState.availableTiles);
                playerHeldTiles.put(pId, syncState.heldTiles);
                playerForbiddenPositions.put(pId, syncState.forbiddenPositions);
                playerBuildingTimeRemaining.put(pId, syncState.buildingTimeRemaining);
                playerTimerFlipped.put(pId, syncState.timerFlipped);
            }
        }
        GameStartedEvent event = new GameStartedEvent(
                gameId, playerInfos, gameSession.getShipGridConfig(),
                playerShipGrids, playerAvailableTiles, playerHeldTiles,
                playerForbiddenPositions, playerBuildingTimeRemaining, playerTimerFlipped
        );
        context.publishEvent(event);

        return new GenericSuccessResponse(getCorrelationId());
    }
} 