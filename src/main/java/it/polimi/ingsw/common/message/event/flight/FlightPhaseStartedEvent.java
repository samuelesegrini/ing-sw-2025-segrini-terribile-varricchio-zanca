package it.polimi.ingsw.common.message.event.flight;

import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;

import java.util.logging.Logger;

/**
 * Event broadcast when the flight phase begins.
 * This triggers navigation to flight views and notifies players.
 */
public class FlightPhaseStartedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(FlightPhaseStartedEvent.class.getName());
    private final int playerCount;
    private final int routeLength;

    /**
     * constructor
     *
     * @param gameId the game ID
     * @param playerCount the number of player
     * @param routeLength the length of the route
     */

    public FlightPhaseStartedEvent(String gameId, int playerCount, int routeLength) {
        super(EventType.FLIGHT_PHASE_STARTED, gameId, null);
        this.playerCount = playerCount;
        this.routeLength = routeLength;
        LOGGER.fine("FlightPhaseStartedEvent instantiated for game: " + gameId + ", players: " + playerCount + ", route length: " + routeLength);
    }

    /**
     *
     * @return the number of player
     */

    public int getPlayerCount() {
        return playerCount;
    }

    /**
     *
     * @return the length of the route
     */

    public int getRouteLength() {
        return routeLength;
    }

    /**
     *
     * @param context The client event context
     */

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

    /**
     *
     * @return the string: FlightPhaseStartedEvent{gameId= .., playerCount= .., routeLength= ..}
     */

    @Override
    public String toString() {
        return "FlightPhaseStartedEvent{" +
                "gameId='" + gameId + '\'' +
                ", playerCount=" + playerCount +
                ", routeLength=" + routeLength +
                '}';
    }
}