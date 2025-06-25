package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.general.GameModel;

/**
 * Broadcast to all players in a game when the lobby is full and the game starts.
 * Signals the transition to the Ship Building phase.
 * 
 * SIMPLIFIED VERSION: Carries full GameModel instead of complex individual state.
 */
public class GameStartedEvent extends AbstractEvent {
    private final GameModel gameModel; // Full server game model

    public GameStartedEvent(String gameId, GameModel gameModel) {
        super(EventType.GAME_STARTED, gameId, null);
        this.gameModel = gameModel;
    }

    public GameModel getGameModel() {
        return gameModel;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Simple Direct Model Architecture: Update ClientState directly
            ClientState clientState = context.getClientState();
            if (clientState != null) {
                clientState.setGameModel(gameModel);
                
                // Use ViewNavigator for proper navigation instead of direct state manipulation
                if (context.getController().getUIContext() != null && 
                    context.getController().getUIContext().getViewNavigator() != null) {
                    
                    boolean success = context.getController().getUIContext().getViewNavigator()
                        .navigateTo(ClientState.ViewState.GAME, "Game started - entering building phase");
                    
                    if (!success) {
                        String reason = context.getController().getUIContext().getViewNavigator()
                            .getNavigationFailureReason(ClientState.ViewState.GAME);
                        java.util.logging.Logger.getLogger(GameStartedEvent.class.getName())
                            .severe("Failed to navigate to GAME after game started - Reason: " + reason);
                    }
                } else {
                    java.util.logging.Logger.getLogger(GameStartedEvent.class.getName())
                        .severe("ViewNavigator not available - cannot navigate to GAME after game started");
                }
            }

            // Show notification about game start
            if (context.getNotificationService() != null) {
                context.getNotificationService().showNotification(
                        new Notification(
                                "Game Started",
                                "The building phase has begun! Build your ship before time runs out.",
                                NotificationType.INFO
                        )
                );
            }
        });
    }
}