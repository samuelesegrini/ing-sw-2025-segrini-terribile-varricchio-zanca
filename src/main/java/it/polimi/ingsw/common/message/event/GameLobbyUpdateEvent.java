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
        super(EventType.GAME_LOBBY_UPDATE, gameId, null);
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