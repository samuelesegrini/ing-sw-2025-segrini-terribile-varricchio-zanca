package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;

import java.util.UUID;

/**
 * Response to join game request.
 */
public class JoinGameResponse extends AbstractResponse {
    private final GameModel gameModel;

    public JoinGameResponse(UUID correlationId, GameModel gameModel) {
        super(correlationId);
        this.gameModel = gameModel;
    }

    public GameModel getGameModel() {
        return gameModel;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess()) {
            // Update client state - Simple Direct Model Architecture
            ClientState clientState = context.getClientState();
            if (clientState != null) {
                clientState.setGameModel(gameModel);
                if (gameModel != null) {
                    clientState.setPlayersInLobby(gameModel.getPlayers());
                }
                clientState.setCurrentView(ClientState.ViewState.GAME_LOBBY);
            }
            
            // Show success notification
            context.showNotification(
                    "Joined Game",
                    "Successfully joined " + (gameModel != null && gameModel.getGameName() != null ? gameModel.getGameName() : "game"),
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
