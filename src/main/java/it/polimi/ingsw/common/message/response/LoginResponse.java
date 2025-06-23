package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.client.ClientModel;

import java.util.UUID;

/**
 * Response to a successful login request.
 */
public class LoginResponse extends AbstractResponse {
    private final String playerId;
    private final String nickname;

    public LoginResponse(UUID correlationId, String playerId, String nickname) {
        super(correlationId);
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
    public void handleOnClient(ClientContext context) {
        if (isSuccess()) {
            // Update model state
            context.getModel().setPlayerId(playerId);
            context.getModel().setNickname(nickname);
            context.getModel().setAuthenticated(true);
            context.getModel().setCurrentView(ClientModel.ViewState.LOBBY);
            
            // Show success notification
            context.showNotification(
                    "Login Successful",
                    "Welcome " + nickname + "!",
                    NotificationType.SUCCESS
            );
        } else {
            // Show error notification for failed login
            context.showNotification(
                    "Login Failed",
                    getErrorMessage() != null ? getErrorMessage() : "Login failed",
                    NotificationType.ERROR
            );
        }
    }
    
    @Override
    public void handleOnClient(it.polimi.ingsw.common.message.ClientMessageContext context) {
        if (isSuccess()) {
            // Update model state
            context.getModel().setPlayerId(playerId);
            context.getModel().setNickname(nickname);
            context.getModel().setAuthenticated(true);
            context.getModel().setCurrentView(ClientModel.ViewState.LOBBY);
            
            // Show success notification
            context.showNotification(
                    "Login Successful",
                    "Welcome " + nickname + "!",
                    NotificationType.SUCCESS
            );
            
            // Request game list to update available games
            context.getController().requestGameList();
        } else {
            // Show error notification for failed login
            context.showNotification(
                    "Login Failed",
                    getErrorMessage() != null ? getErrorMessage() : "Login failed",
                    NotificationType.ERROR
            );
        }
    }

}
