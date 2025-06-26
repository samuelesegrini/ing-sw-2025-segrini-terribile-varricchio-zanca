package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.EventPublisher;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.network.ServerNetworkManager;
import it.polimi.ingsw.common.message.event.Event;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

/**
 * Context for executing requests on the server.
 * Provides access to server-side resources and services.
 */
public interface RequestContext {
    /**
     * Gets the network client ID of the sender.
     * @return The sender's network ID
     */
    String getSenderId();

    /**
     * Gets the player ID if the sender is authenticated.
     * @return The player ID or null if not authenticated
     */
    PlayerId getPlayerId();
    
    /**
     * Gets the player ID as string if the sender is authenticated (legacy compatibility).
     * @return The player ID string or null if not authenticated
     */
    default String getPlayerIdString() {
        PlayerId playerId = getPlayerId();
        return playerId != null ? playerId.toString() : null;
    }

    /**
     * Gets the game session manager.
     * @return The session manager
     */
    GameSessionManager getSessionManager();

    /**
     * Gets the player session registry.
     * @return The player registry
     */
    PlayerSessionRegistry getPlayerRegistry();

    /**
     * Gets the event publisher for broadcasting events.
     * @return The event publisher
     */
    EventPublisher getEventPublisher();

    /**
     * Gets the server network manager.
     * @return The network manager
     */
    ServerNetworkManager getNetworkManager();

    /**
     * Publishes an event to relevant clients.
     * @param event The event to publish
     */
    void publishEvent(Event event);

    /**
     * Gets the game session for the current player.
     * @return The game session or null if not in a game
     */
    GameSession getGameSession();    
    /**
     * Gets the game ID for the current player.
     * @return The game ID or null if not in a game
     */
    String getGameId();
    
    /**
     * Gets the player nickname.
     * @return The player nickname or null if not authenticated
     */
    String getPlayerNickname();
}