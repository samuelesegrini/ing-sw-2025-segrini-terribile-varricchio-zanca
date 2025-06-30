package it.polimi.ingsw.common.message.event.flight;

import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Broadcast whenever player positions on the flight board change.
 */
public class FlightPositionUpdateEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(FlightPositionUpdateEvent.class.getName());
    private final Map<String, Integer> playerPositions;

    /**
     * constructor
     *
     * @param gameId the game ID
     * @param playerPositions the position of every player
     */

    public FlightPositionUpdateEvent(String gameId, Map<String, Integer> playerPositions) {
        super(EventType.FLIGHT_POSITION_UPDATE, gameId, null);
        this.playerPositions = Map.copyOf(playerPositions);
        LOGGER.fine("FlightPositionUpdateEvent instantiated for game: " + gameId + ", positions: " + playerPositions);
    }

    /**
     *
     * @return the position of every player
     */

    public Map<String, Integer> getPlayerPositions() {
        return playerPositions;
    }

    /**
     *
     * @param context The client event context
     */

    @Override
    public void handleOnClient(ClientEventContext context) {
        LOGGER.fine("Handling FlightPositionUpdateEvent for game: " + gameId + ", positions: " + playerPositions);
        // Client updates its local model and refreshes the flight board UI.
        // context.getClientState().updateFlightPositions(playerPositions);
        // context.getGameUI().refreshFlightBoard();
    }
}