package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.client.ClientModel;

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
            // SIMPLIFIED: Direct model replacement instead of complex conversion
            
            // Update ClientModel for lobby/connection UI
            if (context.getController() != null && context.getController().getModel() != null) {
                ClientModel model = context.getController().getModel();
                model.setCurrentView(ClientModel.ViewState.GAME);
            }

            // NEW: Simple direct GameModel usage via ClientState
            if (context.getClientState() != null) {
                context.getClientState().setGameModel(gameModel);
            }

            // Show notification about game start
            if (context.getNotificationService() != null) {
                context.getNotificationService().showNotification(
                        new it.polimi.ingsw.client.ui.Notification(
                                "Game Started",
                                "The building phase has begun! Build your ship before time runs out.",
                                NotificationType.INFO
                        )
                );
            }

            // SIMPLIFIED: Single property change notification instead of multiple
            if (context.getController() != null && context.getController().getModel() != null) {
                context.getController().getModel().firePropertyChange("gameStarted", false, true);
            }
        });
    }
}