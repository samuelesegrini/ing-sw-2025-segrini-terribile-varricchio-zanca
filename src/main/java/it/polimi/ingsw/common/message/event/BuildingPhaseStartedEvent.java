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
            // Update game model with building phase state
            if (context.getClientState() != null) {
                context.getClientState().setGameModel(gameModel);
                
                // Ensure we're in GAME view to see the building phase UI
                if (context.getController().getUIContext() != null && 
                    context.getController().getUIContext().getViewNavigator() != null &&
                    context.getClientState().getCurrentView() != it.polimi.ingsw.client.core.ClientState.ViewState.GAME) {
                    
                    boolean success = context.getController().getUIContext().getViewNavigator()
                        .navigateTo(it.polimi.ingsw.client.core.ClientState.ViewState.GAME, 
                                   "Building phase started - entering ship building");
                    
                    if (!success) {
                        String reason = context.getController().getUIContext().getViewNavigator()
                            .getNavigationFailureReason(it.polimi.ingsw.client.core.ClientState.ViewState.GAME);
                        java.util.logging.Logger.getLogger(BuildingPhaseStartedEvent.class.getName())
                            .severe("Failed to navigate to GAME for building phase - Reason: " + reason);
                    }
                } else if (context.getClientState().getCurrentView() != it.polimi.ingsw.client.core.ClientState.ViewState.GAME) {
                    java.util.logging.Logger.getLogger(BuildingPhaseStartedEvent.class.getName())
                        .severe("ViewNavigator not available - cannot navigate to GAME for building phase");
                }
                
                java.util.logging.Logger.getLogger(BuildingPhaseStartedEvent.class.getName())
                    .info("Building phase started - game model updated");
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
        });
    }
}