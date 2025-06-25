package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.client.ui.NotificationType;

/**
 * Event broadcast when the building phase starts.
 * Contains the complete game state for the building phase.
 */
public class BuildingPhaseStartedEvent extends AbstractEvent {
    private final GameModel gameModel;

    public BuildingPhaseStartedEvent(String gameId, GameModel gameModel) {
        super(EventType.BUILDING_PHASE_STARTED, gameId, null);
        this.gameModel = gameModel;
    }

    public GameModel getGameModel() {
        return gameModel;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // SIMPLIFIED: Direct model replacement instead of complex sync
            
            // NEW: Simple direct GameModel usage via ClientState
            if (context.getClientState() != null) {
                context.getClientState().setGameModel(gameModel);
            } else {
                // LEGACY: Fallback to LocalGameState
                if (context.getClientState() != null) {
                    // Note: syncWithGameModel method doesn't exist in current LocalGameState
                    // This is legacy code that needs to be replaced
                }
            }

            // Show notification about phase transition
            if (context.getNotificationService() != null) {
                context.getNotificationService().showNotification(
                    new it.polimi.ingsw.client.ui.Notification(
                        "Building Phase Started",
                        "The building phase has begun! Build your ship.",
                        NotificationType.INFO
                    )
                );
            }

            // SIMPLIFIED: Single property change notification
            if (context.getController() != null && context.getController().getClientState() != null) {
                context.getController().getClientState().firePropertyChange("buildingPhaseStarted", null, gameModel);
            }
        });
    }
}