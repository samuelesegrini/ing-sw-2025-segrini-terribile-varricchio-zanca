package it.polimi.ingsw.common.message;

import java.util.logging.Logger;

/**
 * Pong response message for keep-alive communication.
 * Sent by clients in response to server ping messages.
 */
public class PongMessage extends AbstractMessage {
    private static final Logger LOGGER = Logger.getLogger(PongMessage.class.getName());

    @Override
    public void handleOnClient(ClientMessageContext context) {
        LOGGER.finer("Received pong from server: " + getTimestamp());
        // No client-side handling needed for pong messages
        // This message is typically sent TO the server, not processed BY the client
    }

    @Override
    public String toString() {
        return "PONG";
    }
}
