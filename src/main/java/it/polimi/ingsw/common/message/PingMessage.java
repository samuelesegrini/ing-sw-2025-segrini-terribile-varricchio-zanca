package it.polimi.ingsw.common.message;

import java.util.logging.Logger;

/**
 * Ping message for keep-alive.
 */
public class PingMessage extends AbstractMessage {
    private static final Logger LOGGER = Logger.getLogger(PingMessage.class.getName());

    /**
     *
     * @param context The client message context providing access to client resources
     */

    @Override
    public void handleOnClient(ClientMessageContext context) {
        LOGGER.finer("Received ping from server: " + getTimestamp());
        // Unified ping-pong handling for both Socket and RMI
        context.getNetworkClient().sendMessage(new PongMessage());
    }

    /**
     *
     * @return PING
     */

    @Override
    public String toString() {
        return "PING";
    }
}

