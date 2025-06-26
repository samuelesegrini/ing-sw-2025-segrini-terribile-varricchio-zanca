package it.polimi.ingsw.common.message.event;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Broadcast whenever player positions on the flight board change.
 */
public class FlightPositionUpdateEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(FlightPositionUpdateEvent.class.getName());
    private final Map<String, Integer> playerPositions;

    public FlightPositionUpdateEvent(String gameId, Map<String, Integer> playerPositions) {
        super(EventType.FLIGHT_POSITION_UPDATE, gameId, null);
        this.playerPositions = Map.copyOf(playerPositions);
        LOGGER.fine("FlightPositionUpdateEvent instantiated for game: " + gameId + ", positions: " + playerPositions);
    }

    public Map<String, Integer> getPlayerPositions() {
        return playerPositions;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        LOGGER.fine("Handling FlightPositionUpdateEvent for game: " + gameId + ", positions: " + playerPositions);
        // Client updates its local model and refreshes the flight board UI.
        // context.getClientState().updateFlightPositions(playerPositions);
        // context.getGameUI().refreshFlightBoard();
    }
}