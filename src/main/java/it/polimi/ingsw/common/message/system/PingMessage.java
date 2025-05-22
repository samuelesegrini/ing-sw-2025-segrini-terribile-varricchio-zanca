// --- START OF FILE PingMessage.java ---
package it.polimi.ingsw.common.message.system;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Command; // So client can send it as a command
import it.polimi.ingsw.common.message.Message;

/**
 * A message used for keep-alive checks. Can be sent by server or client.
 * When client sends it, it's a Command to solicit a Pong.
 * When server sends it, it's an Event to solicit a Pong.
 */
public class PingMessage extends BaseMessage implements Command, Message {
    private static final long serialVersionUID = 1L;
    private final boolean isRequest; // true if this Ping expects a Pong response

    public PingMessage(boolean isRequest) {
        super();
        this.isRequest = isRequest;
    }

    public PingMessage() {
        this(true);
    }

    public boolean isRequest() {
        return isRequest;
    }

    @Override
    public String toString() {
        return "PingMessage{isRequest=" + isRequest + ", timestamp=" + getTimestamp() + "}";
    }
}

