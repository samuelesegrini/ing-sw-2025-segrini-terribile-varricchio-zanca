package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Response to a reconnection request.
 */
public class ReconnectResponse extends AbstractResponse {
    private final String playerId;
    private final String nickname;
    private final String gameId;
    private final Object gameState; // Serialized game state

    public ReconnectResponse(UUID correlationId, String playerId, String nickname,
                             String gameId, Object gameState) {
        super(correlationId);
        this.playerId = playerId;
        this.nickname = nickname;
        this.gameId = gameId;
        this.gameState = gameState;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getGameId() {
        return gameId;
    }

    public Object getGameState() {
        return gameState;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess()) {
            // Update client model state with reconnection data
            context.getModel().setPlayerId(playerId);
            context.getModel().setNickname(nickname);
            context.getModel().setAuthenticated(true);
            
            if (gameId != null) {
                // Restore game state - this would need more sophisticated handling
                // For now, just update the game ID and show notification
                context.showNotification(
                        "Reconnected",
                        "Successfully reconnected to your game",
                        NotificationType.SUCCESS
                );
            } else {
                context.showNotification(
                        "Reconnected", 
                        "Successfully reconnected to the server",
                        NotificationType.SUCCESS
                );
            }
        } else {
            context.showNotification(
                    "Reconnection Failed",
                    getErrorMessage() != null ? getErrorMessage() : "Failed to reconnect",
                    NotificationType.ERROR
            );
        }
    }

}