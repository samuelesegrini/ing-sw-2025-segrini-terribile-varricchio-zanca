package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.state.LocalGameState;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;

/**
 * Event broadcast when a player disconnects from the server.
 */
public class PlayerDisconnectedEvent extends AbstractEvent {
    private final String playerId;
    private final String nickname;

    public PlayerDisconnectedEvent(String playerId, String nickname) {
        super(EventType.PLAYER_DISCONNECTED, null, playerId);
        this.playerId = playerId;
        this.nickname = nickname;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        LocalGameState gameState = context.getGameState();

        // Remove player from local state
        gameState.removePlayer(playerId);

        context.runOnUIThread(() -> {
            //GameUI ui = context.getGameUI();

            // Show notification
            if (!context.isLocalPlayer(playerId)) {
                context.getNotificationService().showNotification(new Notification(
                        "Player Disconnected",
                        nickname + " has disconnected",
                        NotificationType.WARNING
                ));
            }

            // Update UI
        });
    }
}