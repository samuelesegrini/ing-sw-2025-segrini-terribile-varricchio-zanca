package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.common.message.AbstractMessage;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.logging.Logger;

/**
 * Abstract base class for events.
 */
public abstract class AbstractEvent extends AbstractMessage implements Event {
    private static final Logger LOGGER = Logger.getLogger(AbstractEvent.class.getName());

    private final EventType eventType;
    protected final String gameId;
    protected final PlayerId sourcePlayerId;
    
    protected AbstractEvent(EventType eventType, String gameId, PlayerId sourcePlayerId) {
        super();
        this.eventType = eventType;
        this.gameId = gameId;
        this.sourcePlayerId = sourcePlayerId;
        LOGGER.fine("Created event: " + eventType + " for game: " + gameId + " from player: " + sourcePlayerId);
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
    public PlayerId getSourcePlayerId() {
        return sourcePlayerId;
    }
    
    @Override
    public void updateClientState(it.polimi.ingsw.client.core.ClientState clientState) {
        // Default implementation - subclasses should override for specific state updates
        LOGGER.fine("Default updateClientState for event: " + eventType);
    }
    
    @Override
    public void handleOnClient(ClientEventContext context) {
        // Update client state first
        updateClientState(context.getClientState());
        
        // Then trigger UI update - default implementation logs
        LOGGER.fine("Default handleOnClient for event: " + eventType);
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