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

    protected AbstractMessage() {
        this.messageId = UUID.randomUUID();
        this.timestamp = LocalDateTime.now().toString();
    }

    @Override
    public UUID getMessageId() {
        return messageId;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return LocalDateTime.parse(timestamp);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + messageId + ", timestamp=" + timestamp + "}";
    }
}