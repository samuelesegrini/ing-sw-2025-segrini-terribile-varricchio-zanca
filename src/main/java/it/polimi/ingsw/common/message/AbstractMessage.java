package it.polimi.ingsw.common.message;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abstract base class for all messages providing common functionality.
 */
public abstract class AbstractMessage implements Message, Serializable {
    private static final long serialVersionUID = 1L;
    private final UUID messageId;
    private final String timestamp; // Use String instead of LocalDateTime for simplicity

    /**
     * constructor
     * creates a new random ID for the message
     */

    protected AbstractMessage() {
        this.messageId = UUID.randomUUID();
        this.timestamp = LocalDateTime.now().toString();
    }

    /**
     *
     * @return the message ID
     */

    @Override
    public UUID getMessageId() {
        return messageId;
    }

    /**
     *
     * @return the time stamp
     */

    @Override
    public LocalDateTime getTimestamp() {
        return LocalDateTime.parse(timestamp);
    }

    /**
     *
     * @return the abstract message as a string: {id= .. , timestamp= .. }
     */

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + messageId + ", timestamp=" + timestamp + "}";
    }
}