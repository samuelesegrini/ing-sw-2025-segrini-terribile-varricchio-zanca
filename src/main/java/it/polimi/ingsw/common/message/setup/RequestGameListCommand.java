package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Command;

/**
 * Command sent by a client to request the current list of joinable and running games.
 */
public class RequestGameListCommand extends BaseMessage implements Command {
    private static final long serialVersionUID = 1L;

    public RequestGameListCommand() {
        super();
    }

    @Override
    public String toString() {
        return "RequestGameListCommand{" +
                "timestamp=" + getTimestamp() +
                '}';
    }
}