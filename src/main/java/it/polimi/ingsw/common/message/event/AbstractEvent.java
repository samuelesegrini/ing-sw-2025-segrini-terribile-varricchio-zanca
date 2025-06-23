package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.common.message.AbstractMessage;

/**
 * Abstract base class for events.
 */
public abstract class AbstractEvent extends AbstractMessage implements Event {
    private final EventType eventType;
    protected final String gameId;
    protected final String sourcePlayerId;
    
    protected AbstractEvent(EventType eventType, String gameId, String sourcePlayerId) {
        super();
        this.eventType = eventType;
        this.gameId = gameId;
        this.sourcePlayerId = sourcePlayerId;
    }
    
    @Override
    public EventType getEventType() {
        return eventType;
    }
    
    @Override
    public String getGameId() {
        return gameId;
    }
    
    @Override
    public String getSourcePlayerId() {
        return sourcePlayerId;
    }
    
    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Default implementation - send to all clients in the game
        if (gameId != null) {
            return context.isClientInGame(clientId, gameId);
        }
        return true; // Global events go to everyone
    }
}