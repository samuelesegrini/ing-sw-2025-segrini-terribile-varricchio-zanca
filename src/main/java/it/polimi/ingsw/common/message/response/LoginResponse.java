package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;

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
            // Update client state
            ClientState clientState = context.getClientState();
            if (clientState != null) {
                clientState.setPlayerInfo(new it.polimi.ingsw.server.model.domain.player.PlayerId(java.util.UUID.fromString(playerId), nickname), nickname);
                clientState.setCurrentView(ClientState.ViewState.LOBBY);
            }
            
            // Show success notification
            context.showNotification(new Notification(
                    "Login Successful",
                    "Welcome " + nickname + "!",
                    NotificationType.SUCCESS
            ));
        } else {
            // Show error notification for failed login
            context.showNotification(new Notification(
                    "Login Failed",
                    getErrorMessage() != null ? getErrorMessage() : "Login failed",
                    NotificationType.ERROR
            ));
        }
    }

}
