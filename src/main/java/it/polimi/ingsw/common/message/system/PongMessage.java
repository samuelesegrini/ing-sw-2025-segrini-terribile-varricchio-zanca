package it.polimi.ingsw.common.message.system;

import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Command;
import it.polimi.ingsw.common.message.Message;

/**
 * A response to a PingMessage.
 */
public class PongMessage extends BaseMessage implements Command, Message { // Implements both
    private static final long serialVersionUID = 1L;
    private final long originalPingTimestamp;

    public PongMessage(long originalPingTimestamp) {
        super();
        this.originalPingTimestamp = originalPingTimestamp;
    }

    public long getOriginalPingTimestamp() {
        return originalPingTimestamp;
    }

    @Override
    public String toString() {
        return "PongMessage{originalPingTimestamp=" + originalPingTimestamp +
                ", rtt=" + (getTimestamp() - originalPingTimestamp) + "ms, timestamp=" + getTimestamp() + "}";
    }
}
