package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.logging.Logger;

/**
 * Event broadcast when the flight phase begins.
 * This triggers navigation to flight views and notifies players.
 */
public class FlightPhaseStartedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(FlightPhaseStartedEvent.class.getName());
    private final int playerCount;
    private final int routeLength;

    public FlightPhaseStartedEvent(String gameId, int playerCount, int routeLength) {
        super(EventType.FLIGHT_PHASE_STARTED, gameId, null);
        this.playerCount = playerCount;
        this.routeLength = routeLength;
        LOGGER.fine("FlightPhaseStartedEvent instantiated for game: " + gameId + ", players: " + playerCount + ", route length: " + routeLength);
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public int getRouteLength() {
        return routeLength;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update client state to flight phase
            if (context.getClientState() != null) {
                // The phase change will be handled by PhaseChangedEvent
                // This event is primarily for UI-specific flight phase setup
            }

            // Show flight phase notification
            if (context.getNotificationService() != null) {
                LOGGER.fine("Displaying flight phase started notification.");
                context.getNotificationService().showNotification(
                    new Notification("Flight Phase Started", 
                        "Ready for adventure with " + playerCount + " players! Route length: " + routeLength,
                        NotificationType.INFO)
                );
            }

            // Navigation to flight view is handled by PhaseChangedEvent
            // This event focuses on flight-specific UI setup
        });
    }

    @Override
    public String toString() {
        return "FlightPhaseStartedEvent{" +
                "gameId='" + gameId + '\'' +
                ", playerCount=" + playerCount +
                ", routeLength=" + routeLength +
                '}';
    }
}