package it.polimi.ingsw.common.message.event;

import java.util.Map;

/**
 * Broadcast whenever player positions on the flight board change.
 */
public class FlightPositionUpdateEvent extends AbstractEvent {
    private final Map<String, Integer> playerPositions;

    public FlightPositionUpdateEvent(String gameId, Map<String, Integer> playerPositions) {
        super(EventType.FLIGHT_POSITION_UPDATE, gameId, null);
        this.playerPositions = Map.copyOf(playerPositions);
    }

    public Map<String, Integer> getPlayerPositions() {
        return playerPositions;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        // Client updates its local model and refreshes the flight board UI.
        // context.getClientState().updateFlightPositions(playerPositions);
        // context.getGameUI().refreshFlightBoard();
    }
}