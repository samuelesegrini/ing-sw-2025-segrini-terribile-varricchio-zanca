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
                // Set current game lobby for UI display (critical for lobby view text)
                clientState.setCurrentGameLobby(gameModel);
                
                if (gameModel != null) {
                    clientState.setPlayersInLobby(gameModel.getPlayers());
                }
                
                // Navigate to GAME_LOBBY using proper ViewNavigator
                if (context.getController() == null) {
                    throw new IllegalStateException("Controller not available - cannot navigate to GAME_LOBBY after joining game");
                }
                
                if (context.getController().getUIContext() != null && 
                    context.getController().getUIContext().getViewNavigator() != null) {
                    
                    boolean success = context.getController().getUIContext().getViewNavigator()
                        .navigateTo(ClientState.ViewState.GAME_LOBBY, "Joined game: " + 
                                   (gameModel != null ? gameModel.getGameName() : "Unknown"));
                    
                    if (!success) {
                        String reason = context.getController().getUIContext().getViewNavigator()
                            .getNavigationFailureReason(ClientState.ViewState.GAME_LOBBY);
                        throw new IllegalStateException("Failed to navigate to GAME_LOBBY after joining game: " + reason);
                    }
                } else {
                    throw new IllegalStateException("ViewNavigator not available - cannot navigate to GAME_LOBBY after joining game");
                }
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
