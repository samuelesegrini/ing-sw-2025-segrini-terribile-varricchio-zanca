package it.polimi.ingsw.common.message;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base interface for all messages in the Galaxy Trucker messaging system.
 * All messages must be serializable for network transmission.
 */
public interface Message extends Serializable {
    /**
     * Gets the unique identifier for this message.
     * @return The message ID
     */
    UUID getMessageId();

    /**
     * Gets the timestamp when this message was created.
     * @return The creation timestamp
     */
    LocalDateTime getTimestamp();
    
    /**
     * Handles this message on the client side.
     * Each message implementation can define its own handling logic.
     * Default implementation does nothing (for messages that don't need client-side handling).
     * 
     * @param context The client message context providing access to client resources
     */
    default void handleOnClient(ClientMessageContext context) {
        // Default implementation does nothing
        // Messages that need client-side handling should override this method
    }
}