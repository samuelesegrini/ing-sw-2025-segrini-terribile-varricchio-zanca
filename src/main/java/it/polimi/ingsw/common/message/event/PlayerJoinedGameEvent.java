package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.common.PlayerInfo;
import it.polimi.ingsw.client.ClientModel;

/**
 * Event broadcast when a player joins a game lobby.
 * Updates the lobby UI and notifies other players.
 */
public class PlayerJoinedGameEvent extends AbstractEvent {
    private final String playerId;
    private final String playerNickname;
    private final int currentPlayerCount;

    public PlayerJoinedGameEvent(String gameId, String playerId, String playerNickname,
                                 int currentPlayerCount) {
        super(EventType.PLAYER_JOINED_GAME, gameId, playerId);
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
        context.runOnUIThread(() -> {
            // Only show notification for other players (not the joining player)
            if (!context.isLocalPlayer(playerId) && context.getNotificationService() != null) {
                context.getNotificationService().showInfo(
                        "Player Joined",
                        playerNickname + " joined the game (" + currentPlayerCount + " players)"
                );
            }
        });
    }
}
