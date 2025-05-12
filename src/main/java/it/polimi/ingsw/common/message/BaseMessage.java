package it.polimi.ingsw.common.message;

import java.io.Serializable;

/**
 * A base class for messages providing common functionality like a timestamp
 * and ensuring serializability.
 */
public abstract class BaseMessage implements Message, Serializable {
    // The serialVersionUID is important for versioning Serializable classes.
    private static final long serialVersionUID = 1L;

    private final long timestamp;

    protected BaseMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }
}