package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Event broadcast when a player joins a game lobby.
 * Updates the lobby UI and notifies other players.
 */
public class PlayerJoinedGameEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(PlayerJoinedGameEvent.class.getName());
    private final String gameId;
    private final String playerId;
    private final String playerNickname;
    private final int currentPlayerCount;

    public PlayerJoinedGameEvent(String gameId, String playerId, String playerNickname,
                                 int currentPlayerCount) {
        super(EventType.PLAYER_JOINED_GAME, gameId, playerId);
        this.gameId = gameId;
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.currentPlayerCount = currentPlayerCount;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public int getCurrentPlayerCount() {
        return currentPlayerCount;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        ClientState clientState = context.getClientState();
        if (gameId != null && clientState != null) {
            List<GameModel> availableGames = clientState.getAvailableGames();
            for (int i = 0; i < availableGames.size(); i++) {
                GameModel game = availableGames.get(i);
                if (game.getGameId().equals(gameId)) {
                    LOGGER.info("PlayerJoined: " + playerNickname + " -> " + gameId + " (" + currentPlayerCount + " players)");
                    break;
                }
            }
        }
        
        context.runOnUIThread(() -> {
            // Only show notification for other players (not the joining player)
            if (!context.isLocalPlayer(playerId) && context.getNotificationService() != null) {
                context.getNotificationService().showNotification(new Notification(
                        "Player Joined",
                        playerNickname + " joined the game (" + currentPlayerCount + " players)",
                        NotificationType.INFO
                ));
            }
        });
    }
}
