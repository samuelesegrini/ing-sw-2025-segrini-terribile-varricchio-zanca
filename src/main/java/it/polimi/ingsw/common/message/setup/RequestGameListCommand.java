package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.message.BaseMessage;

/**
 * Command sent by a client to request the current list of joinable and running games.
 */
public class RequestGameListCommand extends BaseMessage {
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