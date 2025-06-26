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
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(BuildingPhaseStartedEvent.class.getName());
            
            // Update game model with building phase state
            if (context.getClientState() == null) {
                logger.severe("ClientState is null - cannot update game model for building phase");
                return;
            }
            
            context.getClientState().setGameModel(gameModel);
            logger.info("Building phase started - game model updated");
            
            // Navigate to GAME view if not already there
            if (context.getClientState().getCurrentView() != it.polimi.ingsw.client.core.ClientState.ViewState.GAME) {
                
                if (context.getController() == null || context.getController().getUIContext() == null || 
                    context.getController().getUIContext().getViewNavigator() == null) {
                    logger.severe("ViewNavigator not available - cannot navigate to GAME view for building phase. " +
                                 "Controller or UIContext not properly initialized.");
                    return;
                }
                
                boolean success = context.getController().getUIContext().getViewNavigator()
                    .navigateTo(it.polimi.ingsw.client.core.ClientState.ViewState.GAME, 
                               "Building phase started - entering ship building");
                
                if (!success) {
                    String reason = context.getController().getUIContext().getViewNavigator()
                        .getNavigationFailureReason(it.polimi.ingsw.client.core.ClientState.ViewState.GAME);
                    logger.severe("Failed to navigate to GAME view for building phase - Reason: " + reason);
                    
                    // Show error notification to user
                    if (context.getNotificationService() != null) {
                        context.getNotificationService().showNotification(
                            new it.polimi.ingsw.client.ui.Notification(
                                "Navigation Error",
                                "Cannot enter building phase view: " + reason,
                                NotificationType.ERROR
                            )
                        );
                    }
                    return;
                } else {
                    logger.info("Successfully navigated to GAME view for building phase");
                }
            }

            // Show success notification about phase transition
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