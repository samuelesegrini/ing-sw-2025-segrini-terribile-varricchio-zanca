package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.client.ui.NotificationType;

/**
 * Event broadcast when the flight phase starts.
 * Contains the complete game state for the flight phase.
 */
public class FlightPhaseStartedEvent extends AbstractEvent {
    private final GameModel gameModel;

    public FlightPhaseStartedEvent(String gameId, GameModel gameModel) {
        super(EventType.FLIGHT_PHASE_STARTED, gameId, null);
        this.gameModel = gameModel;
    }

    public GameModel getGameModel() {
        return gameModel;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update game model with flight phase state
            if (context.getClientState() != null) {
                context.getClientState().setGameModel(gameModel);
                
                // Ensure we're in GAME view to see the flight phase UI
                if (context.getController().getUIContext() != null && 
                    context.getController().getUIContext().getViewNavigator() != null &&
                    context.getClientState().getCurrentView() != it.polimi.ingsw.client.core.ClientState.ViewState.GAME) {
                    
                    boolean success = context.getController().getUIContext().getViewNavigator()
                        .navigateTo(it.polimi.ingsw.client.core.ClientState.ViewState.GAME, 
                                   "Flight phase started - entering space exploration");
                    
                    if (!success) {
                        String reason = context.getController().getUIContext().getViewNavigator()
                            .getNavigationFailureReason(it.polimi.ingsw.client.core.ClientState.ViewState.GAME);
                        java.util.logging.Logger.getLogger(FlightPhaseStartedEvent.class.getName())
                            .severe("Failed to navigate to GAME for flight phase - Reason: " + reason);
                    }
                } else if (context.getClientState().getCurrentView() != it.polimi.ingsw.client.core.ClientState.ViewState.GAME) {
                    java.util.logging.Logger.getLogger(FlightPhaseStartedEvent.class.getName())
                        .severe("ViewNavigator not available - cannot navigate to GAME for flight phase");
                }
                
                java.util.logging.Logger.getLogger(FlightPhaseStartedEvent.class.getName())
                    .info("Flight phase started - game model updated");
            }

            // Show notification about phase transition
            if (context.getNotificationService() != null) {
                context.getNotificationService().showNotification(
                    new it.polimi.ingsw.client.ui.Notification(
                        "Flight Phase Started",
                        "The flight phase has begun! Navigate through space and complete adventures.",
                        NotificationType.INFO
                    )
                );
            }
        });
    }
}