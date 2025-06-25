package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.player.Player;
import java.util.List;
import it.polimi.ingsw.client.core.ClientState;

/**
 * Broadcast to all clients in a game lobby when its state changes (e.g., player joins/leaves).
 */
public class GameLobbyUpdateEvent extends AbstractEvent {
    private final List<Player> players;
    private final int requiredPlayers;

    public GameLobbyUpdateEvent(String gameId, List<Player> players, int requiredPlayers) {
        this(gameId, players, requiredPlayers, null);
    }

    public GameLobbyUpdateEvent(String gameId, List<Player> players, int requiredPlayers, String excludePlayerId) {
        super(EventType.GAME_LOBBY_UPDATE, gameId, excludePlayerId);
        this.players = List.copyOf(players);
        this.requiredPlayers = requiredPlayers;
    }

    public List<Player> getPlayers() {
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
            ClientState clientState = context.getClientState();
            if (clientState != null && gameId.equals(clientState.getCurrentGameId())) {
                clientState.setPlayersInLobby(players);
            }
        });
    }
}