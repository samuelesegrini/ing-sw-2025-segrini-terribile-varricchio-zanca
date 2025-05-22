package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Command;

/**
 * Command sent by a client to request taking a component tile
 * from the central face-down pile during the building phase.
 */
public class RequestTakeComponentCommand extends BaseMessage implements Command {
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