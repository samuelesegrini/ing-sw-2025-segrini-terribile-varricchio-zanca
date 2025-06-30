package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.EventPublisher;
import it.polimi.ingsw.common.message.event.Event;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.network.ServerNetworkManager;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.Map;

/**
 * Implementation of RequestContext.
 */
public class RequestContextImpl implements RequestContext {
    private final String senderId;
    private final GameSessionManager sessionManager;
    private final PlayerSessionRegistry playerRegistry;
    private final EventPublisher eventPublisher;
    private final ServerNetworkManager networkManager;
    private final Map<String, String> networkClientToGamePlayerMap;

    /**
     * constructor
     *
     * @param senderId the sender ID
     * @param sessionManager the session manager
     * @param playerRegistry The player registry
     * @param eventPublisher The event publisher
     * @param networkManager The network manager
     * @param networkClientToGamePlayerMap
     */

    public RequestContextImpl(String senderId,
                              GameSessionManager sessionManager,
                              PlayerSessionRegistry playerRegistry,
                              EventPublisher eventPublisher,
                              ServerNetworkManager networkManager,
                              Map<String, String> networkClientToGamePlayerMap) {
        this.senderId = senderId;
        this.sessionManager = sessionManager;
        this.playerRegistry = playerRegistry;
        this.eventPublisher = eventPublisher;
        this.networkManager = networkManager;
        this.networkClientToGamePlayerMap = networkClientToGamePlayerMap;
    }

    /**
     *
     * @return the sender ID
     */

    @Override
    public String getSenderId() {
        return senderId;
    }

    /**
     *
     * @return the player ID for the sender ID
     */

    @Override
    public PlayerId getPlayerId() {
        return playerRegistry.getPlayerIdForClient(senderId);
    }

    /**
     *
     * @return the session manager
     */

    @Override
    public GameSessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     *
     * @return  The player registry
     */

    @Override
    public PlayerSessionRegistry getPlayerRegistry() {
        return playerRegistry;
    }

    /**
     *
     * @return the event publisher
     */

    @Override
    public EventPublisher getEventPublisher() {
        return eventPublisher;
    }

    /**
     *
     * @return the network manager
     */

    @Override
    public ServerNetworkManager getNetworkManager() {
        return networkManager;
    }

    /**
     *
     * @param event The event to publish
     */

    @Override
    public void publishEvent(Event event) {
        eventPublisher.publishEvent(event);
    }

    /**
     *
     * @return the game session
     */

    @Override
    public GameSession getGameSession() {
        PlayerId playerId = getPlayerId();
        if (playerId == null) {
            return null;
        }
        return sessionManager.getGameSessionForPlayer(playerId);
    }

    /**
     *
     * @return the game ID
     */

    @Override
    public String getGameId() {
        PlayerId playerId = getPlayerId();
        if (playerId == null) {
            return null;
        }
        return sessionManager.getPlayerGameId(playerId);
    }

    /**
     *
     * @return the player's nickname
     */

    @Override
    public String getPlayerNickname() {
        PlayerId playerId = getPlayerId();
        if (playerId == null) {
            return null;
        }
        return playerRegistry.getPlayerNickname(playerId);
    }
}