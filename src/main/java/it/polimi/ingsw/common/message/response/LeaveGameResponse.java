package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;

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
            // Update client state - clear current game and return to lobby
            ClientState clientState = context.getClientState();
            if (clientState != null) {
                clientState.setCurrentGame(null);
                clientState.setPlayersInLobby(new java.util.ArrayList<>());
                clientState.setCurrentView(ClientState.ViewState.LOBBY);
            }
            
            // Show success notification
            context.showNotification(new Notification(
                    "Left Game",
                    "You have left the game",
                    NotificationType.INFO
            ));
        } else {
            // Show error notification
            context.showNotification(new Notification(
                    "Failed to Leave",
                    getErrorMessage() != null ? getErrorMessage() : "Failed to leave game",
                    NotificationType.ERROR
            ));
        }
    }

}