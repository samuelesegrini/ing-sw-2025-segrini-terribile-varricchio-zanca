package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.common.message.Message;

/**
 * Interface for event messages that represent state changes.
 * Events are broadcast from server to clients.
 */
public interface Event extends Message {
    /**
     * Gets the type of this event.
     * @return The event type
     */
    EventType getEventType();
    
    /**
     * Gets the game ID this event is associated with.
     * @return The game ID or null for global events
     */
    String getGameId();
    
    /**
     * Gets the player ID who triggered this event.
     * @return The source player ID or null
     */
    String getSourcePlayerId();
    
    /**
     * Handles this event on the client side.
     * Each event knows how to update the client state and UI.
     * @param context The client event context
     */
    void handleOnClient(ClientEventContext context);
    
    /**
     * Handles this event on the client side using the unified message context.
     * Default implementation delegates to the original handleOnClient method for backward compatibility.
     * @param context The client message context
     */
    default void handleOnClient(it.polimi.ingsw.common.message.ClientMessageContext context) {
        handleOnClient((ClientEventContext) context);
    }
    
    /**
     * Determines if this event should be sent to a specific client.
     * Used for server-side filtering.
     * @param clientId The client to check
     * @param context The filter context
     * @return true if the event should be sent to this client
     */
    boolean shouldSendTo(String clientId, EventFilterContext context);
}