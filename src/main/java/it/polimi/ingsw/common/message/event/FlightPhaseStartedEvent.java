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
            // SIMPLIFIED: Direct model replacement instead of complex sync
            context.getClientState().setGameModel(gameModel);


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

            // SIMPLIFIED: Single property change notification
            if (context.getController() != null && context.getController().getClientState() != null) {
                context.getController().getClientState().firePropertyChange("flightPhaseStarted", null, gameModel);
            }
        });
    }
}