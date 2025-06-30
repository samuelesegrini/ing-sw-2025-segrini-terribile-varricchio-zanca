package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.event.GameLobbyUpdateEvent;
import it.polimi.ingsw.common.message.event.GamesListUpdateEvent;
import it.polimi.ingsw.common.message.response.LeaveGameResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;

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
        PlayerId playerId = context.getPlayerId();
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

        // Check if game should be ended
        //TODO: Use new listeners for event
        // non so se questo è il posto giusto per questa regola (dovremmo essere nella gamelobby)
        if (gameSession.getPlayerCount() == 0) {
            // Model operation will fire the event automatically
        } else {
            // Update lobby state for remaining players (exclude the leaving player)
            //TODO: Use new listeners for event
            publishLobbyUpdateEvent(gameSession, gameId, registry, playerId.toString());
        }

        // Broadcast updated games list to all clients in main lobby
        //TODO: Use new listeners for event
        publishGamesListUpdateEvent(gameSession, sessionManager, registry);


        //TODO: capire come cavolo gestire richieste e messaggi e listeners
        // per il resto fa cose giuste
        // POTENZIALE GENERIC RESPONSE
        // INTERESSANTE USO DI createSuccessResponse()
        return new LeaveGameResponse(getCorrelationId());
    }

    /**
     * Publishes a lobby update event to synchronize remaining clients with updated lobby state.
     */
    private void publishLobbyUpdateEvent(GameSession gameSession, 
                                       String gameId, PlayerSessionRegistry registry, String excludePlayerId) {
        // Use server model directly - Simple Direct Model Architecture
        List<Player> players = gameSession.getGameModel().getPlayers();
        
        // Create and publish the lobby update event (excluding the leaving player)
        GameLobbyUpdateEvent lobbyEvent = new GameLobbyUpdateEvent(
                gameId, players, gameSession.getMaxPlayers(), PlayerId.fromString(excludePlayerId)
        );
        // Model operation will fire the event automatically
    }

    /**
     * Publishes a games list update event to broadcast current available games to all lobby clients.
     */
    private void publishGamesListUpdateEvent(GameSession gameSession, GameSessionManager sessionManager, 
                                           PlayerSessionRegistry registry) {
        // Get the current list of available games - use GameModel directly
        List<GameModel> availableGames = sessionManager.getAvailableGames();
        
        // Create and publish the games list update event
        GamesListUpdateEvent gamesListEvent = new GamesListUpdateEvent(availableGames);
        // Model operation will fire the event automatically
    }
}
