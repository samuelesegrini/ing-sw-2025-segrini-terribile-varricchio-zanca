package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.common.message.Message;

import java.util.UUID;

/**
 * Interface for response messages sent from server to client.
 */
public interface Response extends Message {
    /**
     * Gets the correlation ID linking this response to its request.
     * @return The correlation ID
     */
    UUID getCorrelationId();
    
    /**
     * Indicates whether the request was successful.
     * @return true if successful, false otherwise
     */
    boolean isSuccess();
    
    /**
     * Gets an error message if the request failed.
     * @return The error message or null if successful
     */
    String getErrorMessage();
    
    /**
     * Gets an error code if the request failed.
     * @return The error code or null if successful
     */
    String getErrorCode();
    
    /**
     * Handles this response on the client side.
     * @param context The client context
     */
    void handleOnClient(ClientContext context);
    
    /**
     * Handles this response on the client side using the unified message context.
     * Default implementation delegates to the original handleOnClient method for backward compatibility.
     * @param context The client message context
     */
    default void handleOnClient(it.polimi.ingsw.common.message.ClientMessageContext context) {
        handleOnClient((ClientContext) context);
    }
}