package it.polimi.ingsw.common.message.event.flight;

import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Broadcast to update a player's resources (credits, crew, goods, batteries).
 */
public class ResourceUpdateEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(ResourceUpdateEvent.class.getName());
    private final String playerId;
    private final Map<String, Integer> updatedResources;

    /**
     * constructor
     *
     * @param gameId the game ID
     * @param playerId the player ID
     * @param updatedResources the quantity of every resource
     */

    public ResourceUpdateEvent(String gameId, String playerId, Map<String, Integer> updatedResources) {
        super(EventType.RESOURCE_UPDATE, gameId, PlayerId.fromString(playerId));
        this.playerId = playerId;
        this.updatedResources = Map.copyOf(updatedResources);
        LOGGER.fine("ResourceUpdateEvent instantiated for game: " + gameId + ", player: " + playerId + ", resources: " + updatedResources);
    }

    /**
     *
     * @return  the game ID
     */

    public String getPlayerId() {
        return playerId;
    }

    /**
     *
     * @return the quantity of every resource
     */

    public Map<String, Integer> getUpdatedResources() {
        return updatedResources;
    }

    /**
     *
     * @param context The client event context
     */

    @Override
    public void handleOnClient(ClientEventContext context) {
        LOGGER.fine("Handling ResourceUpdateEvent for game: " + gameId + ", player: " + playerId + ", resources: " + updatedResources);
        // Client updates the local model and UI for the specified player's resources.
        // e.g., context.getClientState().updatePlayerResources(playerId, updatedResources);
        //      context.getGameUI().refreshPlayerPanel(playerId);
    }
}