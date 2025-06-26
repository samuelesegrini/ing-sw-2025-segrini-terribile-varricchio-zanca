package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.common.message.event.GameLobbyUpdateEvent;
import it.polimi.ingsw.common.message.event.PlayerReadyChangedEvent;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.SetPlayerReadyResponse;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Request sent by a player to mark themselves as ready in the game lobby.
 * Players must be ready before the game can start.
 */
public class SetPlayerReadyRequest extends AbstractRequest {
    private static final Logger LOGGER = Logger.getLogger(SetPlayerReadyRequest.class.getName());

    private final boolean ready;

    public SetPlayerReadyRequest(boolean ready) {
        super();
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }

    @Override
    public ValidationResult validate() {
        // No validation needed - ready status is always valid
        return ValidationResult.success();
    }

    @Override
    public Response execute(RequestContext context) {
        LOGGER.info("⚡ SET READY REQUEST - Player setting ready status to: " + ready + " from client: " + context.getSenderId());
        
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            LOGGER.warning("❌ SET READY FAILED - Validation error: " + validation.getErrorMessage());
            return createErrorResponse(validation.getErrorMessage(), "VALIDATION_ERROR");
        }

        PlayerId playerId = context.getPlayerId();
        if (playerId == null) {
            LOGGER.warning("❌ SET READY FAILED - Client " + context.getSenderId() + " is not authenticated");
            return createErrorResponse("Authentication required", "AUTHENTICATION_ERROR");
        }
        LOGGER.fine("✅ AUTH CHECK - Player " + playerId + " is authenticated");

        GameSessionManager sessionManager = context.getSessionManager();
        PlayerSessionRegistry registry = context.getPlayerRegistry();
        
        String gameId = sessionManager.getPlayerGameId(playerId.toString());
        if (gameId == null) {
            return createErrorResponse("Player is not in any game", "INVALID_STATE");
        }

        GameSession gameSession = sessionManager.getGameSession(gameId);
        if (gameSession == null) {
            return createErrorResponse("Game session not found", "NOT_FOUND");
        }

        if (gameSession.isStarted()) {
            return createErrorResponse("Cannot change ready status after game has started", "INVALID_STATE");
        }

        // Update player ready status
        LOGGER.info("⚡ UPDATING READY - Setting ready=" + ready + " for player: " + playerId + " in game: " + gameId);
        gameSession.setPlayerReady(playerId, ready);
        
        // Get player nickname for event
        String playerNickname = registry.getPlayerNickname(playerId);
        LOGGER.fine("👤 PLAYER INFO - Retrieved nickname: '" + playerNickname + "' for player: " + playerId);
        
        // Publish player ready changed event
        PlayerReadyChangedEvent event = new PlayerReadyChangedEvent(
                gameId, playerId, playerNickname, ready
        );
        LOGGER.info("📢 EVENT PUBLISH - Publishing PlayerReadyChangedEvent for player: " + playerNickname + 
                   " (" + playerId + ") ready=" + ready);
        context.publishEvent(event);
        
        // Check for auto-start and publish lobby update (exclude requesting player)
        LOGGER.info("📢 LOBBY UPDATE - Publishing GameLobbyUpdateEvent (excluding requester: " + playerId + ")");
        publishLobbyUpdateEvent(context, gameSession, gameId, playerId);
        
        LOGGER.info("🎉 SET READY SUCCESS - Player " + playerNickname + " (" + playerId + ") ready status set to: " + ready);
        return new SetPlayerReadyResponse(getCorrelationId(), ready);
    }
    
    private void publishLobbyUpdateEvent(RequestContext context, GameSession gameSession, String gameId, PlayerId excludePlayerId) {
        PlayerSessionRegistry registry = context.getPlayerRegistry();
        List<Player> playerInfos = new ArrayList<>();
        
        for (PlayerId pId : gameSession.getPlayerIds()) {
            String pNickname = registry.getPlayerNickname(pId);
            boolean isReady = gameSession.getPlayerState(pId) != null && gameSession.getPlayerState(pId).isReady();
            Player player = new Player(pId);
            player.setReady(isReady);
            playerInfos.add(player);
        }
        
        GameLobbyUpdateEvent lobbyEvent = new GameLobbyUpdateEvent(
                gameId, playerInfos, gameSession.getMaxPlayers(), excludePlayerId
        );
        context.publishEvent(lobbyEvent);
    }
}