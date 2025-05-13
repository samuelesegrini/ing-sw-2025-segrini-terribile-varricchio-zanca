package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.message.BaseMessage;

/**
 * Event sent by the server to all players when the ship building phase has ended for everyone.
 * This signals the transition to the "Preparing for Launch" sub-phase or directly to flight.
 */
public class ShipBuildingPhaseEndedEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    // Could include final ship boards of all players if not sent by other means.
    // For now, it's just a signal.

    public ShipBuildingPhaseEndedEvent() {
        super();
    }

    @Override
    public String toString() {
        return "ShipBuildingPhaseEndedEvent{" +
                "timestamp=" + getTimestamp() +
                '}';
    }
}