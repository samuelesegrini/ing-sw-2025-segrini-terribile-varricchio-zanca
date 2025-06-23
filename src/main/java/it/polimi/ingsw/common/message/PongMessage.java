package it.polimi.ingsw.common.message;

/**
 * Pong response message for keep-alive communication.
 * Sent by clients in response to server ping messages.
 */
public class PongMessage extends AbstractMessage {

    @Override
    public void handleOnClient(ClientMessageContext context) {
        // No client-side handling needed for pong messages
        // This message is typically sent TO the server, not processed BY the client
    }

    @Override
    public String toString() {
        return "PONG";
    }
}
