package it.polimi.ingsw.common.message;

import it.polimi.ingsw.common.message.event.Event;

/**
 * Interface for publishing events to clients.
 */
public interface EventPublisher {
    /**
     * Publishes an event to all relevant clients.
     * @param event The event to publish
     */
    void publishEvent(Event event);
    
    /**
     * Publishes an event to a specific client.
     * @param event The event to publish
     * @param clientId The target client ID
     */
    void publishEventToClient(Event event, String clientId);
    
    /**
     * Publishes an event to all clients in a game.
     * @param event The event to publish
     * @param gameId The game ID
     */
    void publishEventToGame(Event event, String gameId);
}