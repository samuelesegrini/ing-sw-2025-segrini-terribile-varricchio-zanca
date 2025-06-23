package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.ClientModel;

/**
 * Event broadcast when a player leaves a game lobby or active game.
 * Updates the player list and notifies remaining players.
 */
public class PlayerLeftGameEvent extends AbstractEvent {
    private final String playerId;
    private final String playerNickname;

    public PlayerLeftGameEvent(String gameId, String playerId, String playerNickname) {
        super(EventType.PLAYER_LEFT_GAME, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Don't send to the leaving player (they already have the response)
        String playerId = context.getPlayerIdForClient(clientId);
        if (this.playerId.equals(playerId)) {
            return false;
        }
        
        // Use default game filtering for other clients
        return super.shouldSendTo(clientId, context);
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Only show notification for remaining players (not the leaving player)
            if (!context.isLocalPlayer(playerId) && context.getNotificationService() != null) {
                context.getNotificationService().showWarning(
                        "Player Left",
                        playerNickname + " left the game"
                );
            }
        });
    }
}