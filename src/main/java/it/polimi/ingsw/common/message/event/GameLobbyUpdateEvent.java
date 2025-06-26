package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import java.util.List;
import it.polimi.ingsw.client.core.ClientState;

import java.util.logging.Logger;

/**
 * Broadcast to all clients in a game lobby when its state changes (e.g., player joins/leaves).
 */
public class GameLobbyUpdateEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(GameLobbyUpdateEvent.class.getName());
    private final List<Player> players;
    private final int requiredPlayers;

    public GameLobbyUpdateEvent(String gameId, List<Player> players, int requiredPlayers) {
        this(gameId, players, requiredPlayers, null);
    }

    public GameLobbyUpdateEvent(String gameId, List<Player> players, int requiredPlayers, PlayerId excludePlayerId) {
        super(EventType.GAME_LOBBY_UPDATE, gameId, excludePlayerId);
        this.players = List.copyOf(players);
        this.requiredPlayers = requiredPlayers;
        LOGGER.fine("GameLobbyUpdateEvent instantiated for game: " + gameId + ", players: " + players.size() + ", required: " + requiredPlayers);
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
            PlayerId playerId = context.getPlayerIdForClient(clientId);
            if (sourcePlayerId.equals(playerId)) {
                LOGGER.finer("EVENT FILTERING - GameLobbyUpdateEvent NOT sent to excluded player: " + clientId);
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
                LOGGER.fine("Client state updated with new lobby players for game: " + gameId + ", count: " + players.size());
                
                // Note: GameModel players list is managed through addPlayer/removePlayer
                // so we don't directly set the players list here
            }
        });
    }
}