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
                clientState.setGameModel(null);
                clientState.setPlayersInLobby(new java.util.ArrayList<>());
                
                // Use ViewNavigator through controller for consistent navigation
                if (context.getController() != null && 
                    context.getController().getUIContext() != null && 
                    context.getController().getUIContext().getViewNavigator() != null) {
                    
                    boolean success = context.getController().getUIContext().getViewNavigator()
                        .navigateTo(ClientState.ViewState.LOBBY, "Left game successfully");
                    
                    if (!success) {
                        String reason = context.getController().getUIContext().getViewNavigator()
                            .getNavigationFailureReason(ClientState.ViewState.LOBBY);
                        System.err.println("LeaveGameResponse: ViewNavigator failed, using fallback - Reason: " + reason);
                        clientState.setCurrentView(ClientState.ViewState.LOBBY);
                    }
                } else {
                    System.err.println("LeaveGameResponse: ViewNavigator not available, using direct navigation fallback");
                    clientState.setCurrentView(ClientState.ViewState.LOBBY);
                }
            }
            
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