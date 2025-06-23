package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.client.ClientModel;

import java.util.UUID;

/**
 * Response to leave game request.
 */
public class LeaveGameResponse extends AbstractResponse {

    public LeaveGameResponse(UUID correlationId) {
        super(correlationId);
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess()) {
            // Update model state - clear current game and return to lobby
            context.getModel().setCurrentGame(null);
            context.getModel().setPlayersInLobby(new java.util.ArrayList<>());
            context.getModel().setCurrentView(ClientModel.ViewState.LOBBY);
            
            // Show success notification
            context.showNotification(
                    "Left Game",
                    "You have left the game",
                    NotificationType.INFO
            );
        } else {
            // Show error notification
            context.showNotification(
                    "Failed to Leave",
                    getErrorMessage() != null ? getErrorMessage() : "Failed to leave game",
                    NotificationType.ERROR
            );
        }
    }

}