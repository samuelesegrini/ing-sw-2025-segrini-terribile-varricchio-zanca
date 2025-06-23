package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.common.PlayerInfo;
import java.util.List;
import it.polimi.ingsw.client.ClientModel;

/**
 * Broadcast to all clients in a game lobby when its state changes (e.g., player joins/leaves).
 */
public class GameLobbyUpdateEvent extends AbstractEvent {
    private final List<PlayerInfo> players;
    private final int requiredPlayers;

    public GameLobbyUpdateEvent(String gameId, List<PlayerInfo> players, int requiredPlayers) {
        this(gameId, players, requiredPlayers, null);
    }

    public GameLobbyUpdateEvent(String gameId, List<PlayerInfo> players, int requiredPlayers, String excludePlayerId) {
        super(EventType.GAME_LOBBY_UPDATE, gameId, excludePlayerId);
        this.players = List.copyOf(players);
        this.requiredPlayers = requiredPlayers;
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }

    public int getRequiredPlayers() {
        return requiredPlayers;
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Don't send to the requesting client if they're excluded
        if (sourcePlayerId != null) {
            String playerId = context.getPlayerIdForClient(clientId);
            if (sourcePlayerId.equals(playerId)) {
                return false;
            }
        }
        
        // Use default game filtering for other clients
        return super.shouldSendTo(clientId, context);
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            if (context.getController() != null && context.getController().getModel() != null) {
                ClientModel model = context.getController().getModel();
                if (gameId.equals(model.getCurrentGameId())) {
                    model.setPlayersInLobby(players);
                }
            }
        });
    }
}