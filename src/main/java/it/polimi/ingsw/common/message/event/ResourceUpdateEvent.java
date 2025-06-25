package it.polimi.ingsw.common.message.event;

import java.util.Map;

/**
 * Broadcast to update a player's resources (credits, crew, goods, batteries).
 */
public class ResourceUpdateEvent extends AbstractEvent {
    private final String playerId;
    private final Map<String, Integer> updatedResources;

    public ResourceUpdateEvent(String gameId, String playerId, Map<String, Integer> updatedResources) {
        super(EventType.RESOURCE_UPDATE, gameId, playerId);
        this.playerId = playerId;
        this.updatedResources = Map.copyOf(updatedResources);
    }

    public String getPlayerId() {
        return playerId;
    }

    public Map<String, Integer> getUpdatedResources() {
        return updatedResources;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        // Client updates the local model and UI for the specified player's resources.
        // e.g., context.getClientState().updatePlayerResources(playerId, updatedResources);
        //      context.getGameUI().refreshPlayerPanel(playerId);
    }
}