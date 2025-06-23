package it.polimi.ingsw.common.message.event;

/**
 * Event broadcast when a player disconnects from a game session (network disconnection).
 * This is different from PlayerLeftGameEvent which is for intentional leaving.
 * Only notifies other players in the same game, showing that the player is temporarily unavailable.
 */
public class PlayerDisconnectedEvent extends AbstractEvent {
    private final String playerId;
    private final String playerNickname;
    private final boolean isInActiveGame; // Whether disconnection happened during active gameplay

    public PlayerDisconnectedEvent(String gameId, String playerId, String playerNickname, boolean isInActiveGame) {
        super(EventType.PLAYER_DISCONNECTED, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.isInActiveGame = isInActiveGame;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public boolean isInActiveGame() {
        return isInActiveGame;
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Don't send to the disconnected player (they're not connected anyway)
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
                String message = isInActiveGame 
                    ? playerNickname + " disconnected from the game (may reconnect)"
                    : playerNickname + " disconnected from the lobby";
                    
                context.getNotificationService().showWarning(
                        "Player Disconnected",
                        message
                );
            }
        });
    }
}