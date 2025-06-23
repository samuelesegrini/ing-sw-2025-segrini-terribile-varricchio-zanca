package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;

/**
 * Event broadcast when a player reconnects.
 */
public class PlayerReconnectedEvent extends AbstractEvent {
    private final String playerId;
    private final String nickname;

    public PlayerReconnectedEvent(String playerId, String nickname, String gameId) {
        super(EventType.PLAYER_RECONNECTED, gameId, playerId);
        this.playerId = playerId;
        this.nickname = nickname;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        if (!context.isLocalPlayer(playerId)) {
            context.getNotificationService().showNotification(new Notification(
                    "Player Reconnected",
                    nickname + " has reconnected",
                    NotificationType.INFO
            ));

            context.runOnUIThread(() -> {
//                GameUI ui = context.getGameUI();
//                if (ui.isInGame() && getGameId() != null) {
//                    ui.showPlayerReconnected(playerId, nickname);
//                    ui.refreshPlayerList();
//                }
            });
        }
    }
}