package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.EventPublisher;
import it.polimi.ingsw.common.message.event.Event;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.network.ServerNetworkManager;

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

    @Override
    public String getSenderId() {
        return senderId;
    }

    @Override
    public String getPlayerId() {
        return playerRegistry.getPlayerIdForClient(senderId);
    }

    @Override
    public GameSessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    public PlayerSessionRegistry getPlayerRegistry() {
        return playerRegistry;
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventPublisher;
    }

    @Override
    public ServerNetworkManager getNetworkManager() {
        return networkManager;
    }

    @Override
    public void publishEvent(Event event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public GameSession getGameSession() {
        String playerId = getPlayerId();
        if (playerId == null) {
            return null;
        }
        return sessionManager.getGameSessionForPlayer(playerId);
    }
}