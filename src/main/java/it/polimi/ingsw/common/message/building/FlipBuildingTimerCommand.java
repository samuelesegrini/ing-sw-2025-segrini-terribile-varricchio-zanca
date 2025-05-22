package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Command;

/**
 * Command sent by a client (any player) to flip the building timer
 * to its next stage during the building phase (for Level II+ games).
 */
public class FlipBuildingTimerCommand extends BaseMessage implements Command {
    private static final long serialVersionUID = 1L;

    public FlipBuildingTimerCommand() {
        super();
    }

    @Override
    public String toString() {
        return "FlipBuildingTimerCommand{" +
                "timestamp=" + getTimestamp() +
                '}';
    }
}