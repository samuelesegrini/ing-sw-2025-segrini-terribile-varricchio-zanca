package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Command;

/**
 * Command sent by a client to signal that they have finished building their ship.
 */
public class FinishBuildingCommand extends BaseMessage implements Command {
    private static final long serialVersionUID = 1L;

    public FinishBuildingCommand() {
        super();
    }

    @Override
    public String toString() {
        return "FinishBuildingCommand{" +
                "timestamp=" + getTimestamp() +
                '}';
    }
}