package it.polimi.ingsw.common.message.event;

/**
 * Event broadcast when a player reconnects to a game session.
 * Only notifies other players in the same game, showing that the player is back online.
 */
public class PlayerReconnectedEvent extends AbstractEvent {
    private final String playerId;
    private final String playerNickname;
    private final boolean wasInActiveGame; // Whether reconnection happened during active gameplay

    public PlayerReconnectedEvent(String gameId, String playerId, String playerNickname, boolean wasInActiveGame) {
        super(EventType.PLAYER_RECONNECTED, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.wasInActiveGame = wasInActiveGame;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public boolean wasInActiveGame() {
        return wasInActiveGame;
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Don't send to the reconnected player (they get the reconnection response)
        String playerIdForClient = context.getPlayerIdForClient(clientId);
        if (this.playerId.equals(playerIdForClient)) {
            return false;
        }
        
        // Use default game filtering for other clients in the same game
        return super.shouldSendTo(clientId, context);
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Only show notification to other players in the game
            if (!context.isLocalPlayer(playerId) && context.getNotificationService() != null) {
                String message = wasInActiveGame 
                    ? playerNickname + " reconnected to the game"
                    : playerNickname + " reconnected to the lobby";
                    
                context.getNotificationService().showInfo(
                        "Player Reconnected",
                        message
                );
            }
        });
    }
}