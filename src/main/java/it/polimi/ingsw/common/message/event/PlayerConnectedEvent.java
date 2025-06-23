package it.polimi.ingsw.common.message.event;

/**
 * Event broadcast when a player connects to the server.
 */
public class PlayerConnectedEvent extends AbstractEvent {
    private final String playerId;
    private final String nickname;

    public PlayerConnectedEvent(String playerId, String nickname) {
        super(EventType.PLAYER_CONNECTED, null, playerId);
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
//        // Only show notification if it's not the local player
//        if (!context.isLocalPlayer(playerId)) {
//            context.getNotificationManager().showNotification(
//                    "Player Connected",
//                    nickname + " has connected to the server",
//                    NotificationType.INFO
//            );
//        }
//
//        // Update player list if in lobby
//        context.runOnUIThread(() -> {
//            GameUI ui = context.getGameUI();
//            if (ui.isInLobby()) {
//                ui.refreshOnlinePlayersList();
//            }
//        });
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Send to all authenticated clients
        return context.getPlayerIdForClient(clientId) != null;
    }
}