package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.common.GameInfo;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.client.ClientModel;

import java.util.UUID;

/**
 * Response to join game request.
 */
public class JoinGameResponse extends AbstractResponse {
    private final GameInfo gameInfo;

    public JoinGameResponse(UUID correlationId, GameInfo gameInfo) {
        super(correlationId);
        this.gameInfo = gameInfo;
    }

    public GameInfo getGameInfo() {
        return gameInfo;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess()) {
            // Update model state
            context.getModel().setCurrentGame(gameInfo);
            if (gameInfo != null) {
                context.getModel().setPlayersInLobby(gameInfo.getPlayers());
            }
            context.getModel().setCurrentView(ClientModel.ViewState.GAME_LOBBY);
            
            // Show success notification
            context.showNotification(
                    "Joined Game",
                    "Successfully joined " + (gameInfo != null && gameInfo.gameName != null ? gameInfo.gameName : "game"),
                    NotificationType.SUCCESS
            );
        } else {
            // Show error notification for failed join
            context.showNotification(
                    "Join Failed",
                    getErrorMessage() != null ? getErrorMessage() : "Failed to join game",
                    NotificationType.ERROR
            );
        }
    }

}
