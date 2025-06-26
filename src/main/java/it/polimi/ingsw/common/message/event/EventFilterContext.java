package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.player.PlayerId;
import java.util.Set;

/**
 * Event filter context for server-side event filtering.
 */
public interface EventFilterContext {
    boolean isClientInGame(String clientId, String gameId);
    PlayerId getPlayerIdForClient(String clientId);
    Set<String> getClientsInGame(String gameId);
    Set<String> getAllClients();
    
}