package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.UUID;

/**
 * Response to a successful login request.
 */
public class LoginResponse extends AbstractResponse {
    private final PlayerId playerId;
    private final String nickname;

    /**
     * constructor
     *
     * @param correlationId The correlation ID
     * @param playerId The player ID
     * @param nickname The nickname
     */

    public LoginResponse(UUID correlationId, PlayerId playerId, String nickname) {
        super(correlationId);
        this.playerId = playerId;
        this.nickname = nickname;
    }

    /**
     *
     * @return The player ID
     */

    public PlayerId getPlayerId() {
        return playerId;
    }

    /**
     *
     * @return The nickname
     */

    public String getNickname() {
        return nickname;
    }

    /**
     *
     * @param context The client context
     */

    @Override
    public void handleOnClient(ClientContext context) {
        System.out.println("Player " + nickname + " logged in.");
        if (isSuccess()) {
            // Update client state
            ClientState clientState = context.getClientState();
            if (clientState != null) {
                clientState.setPlayerInfo(playerId, nickname);


                // Note: View navigation is now handled by ClientController.login() method
                // to ensure proper ViewNavigator usage. This response handler should not
                // directly change views to maintain architectural consistency.
            }
            
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

        context.getController().getUI().onLoginResponse(this);
    }

}
