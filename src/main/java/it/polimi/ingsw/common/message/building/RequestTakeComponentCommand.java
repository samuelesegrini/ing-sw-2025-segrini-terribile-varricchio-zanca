package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.message.BaseMessage;

/**
 * Command sent by a client to request taking a component tile
 * from the central face-down pile during the building phase.
 */
public class RequestTakeComponentCommand extends BaseMessage {
    private static final long serialVersionUID = 1L;

    public RequestTakeComponentCommand() {
        super();
    }

    @Override
    public String toString() {
        return "RequestTakeComponentCommand{" +
                "timestamp=" + getTimestamp() +
                '}';
    }
}