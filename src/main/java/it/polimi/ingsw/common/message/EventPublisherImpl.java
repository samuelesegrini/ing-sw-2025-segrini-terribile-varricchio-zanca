package it.polimi.ingsw.common.message;

import it.polimi.ingsw.common.message.event.Event;
import it.polimi.ingsw.common.message.event.EventFilterContext;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.network.ServerNetworkManager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Event publisher implementation.
 */
public class EventPublisherImpl implements EventPublisher {
    private static final Logger LOGGER = Logger.getLogger(EventPublisherImpl.class.getName());

    private final ServerNetworkManager networkManager;
    private final PlayerSessionRegistry playerRegistry;
    private final GameSessionManager sessionManager;
    private final ExecutorService eventExecutor;
    private final EventFilterContextImpl filterContext;

    public EventPublisherImpl(ServerNetworkManager networkManager,
                              PlayerSessionRegistry playerRegistry,
                              GameSessionManager sessionManager) {
        this.networkManager = networkManager;
        this.playerRegistry = playerRegistry;
        this.sessionManager = sessionManager;
        this.eventExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("event-publisher-" + t.threadId());
            t.setDaemon(true);
            return t;
        });
        this.filterContext = new EventFilterContextImpl();
    }

    @Override
    public void publishEvent(Event event) {
        LOGGER.fine("Publishing event: " + event.getEventType() +
                " for game: " + event.getGameId());

        // Distribute asynchronously
        eventExecutor.submit(() -> distributeEvent(event));
    }

    @Override
    public void publishEventToClient(Event event, String clientId) {

    }

    @Override
    public void publishEventToGame(Event event, String gameId) {

    }

    private void distributeEvent(Event event) {
        Set<String> potentialRecipients = getPotentialRecipients(event);

        LOGGER.fine("Potential recipients for " + event.getEventType() +
                ": " + potentialRecipients.size());

        for (String clientId : potentialRecipients) {
            // Let the event decide if it should be sent to this client
            if (event.shouldSendTo(clientId, filterContext)) {
                boolean sent = networkManager.sendMessageToClient(clientId, event);
                if (sent) {
                    LOGGER.finer("Sent " + event.getEventType() + " to " + clientId);
                } else {
                    LOGGER.warning("Failed to send event to " + clientId);
                }
            }
        }
    }

    private Set<String> getPotentialRecipients(Event event) {
        if (event.getGameId() != null) {
            // Game-specific event
            return filterContext.getClientsInGame(event.getGameId());
        } else {
            // Global event
            return filterContext.getAllClients();
        }
    }

    /**
     * Event filter context implementation.
     */
    private class EventFilterContextImpl implements EventFilterContext {

        @Override
        public boolean isClientInGame(String clientId, String gameId) {
            String playerId = playerRegistry.getPlayerIdForClient(clientId);
            if (playerId == null) return false;

            GameSession session = sessionManager.getGameSessionForPlayer(playerId);
            return session != null && session.getGameId().equals(gameId);
        }

        @Override
        public String getPlayerIdForClient(String clientId) {
            return playerRegistry.getPlayerIdForClient(clientId);
        }

        @Override
        public Set<String> getClientsInGame(String gameId) {
            Set<String> clients = new HashSet<>();
            GameSession session = sessionManager.getGameSession(gameId);

            if (session != null) {
                for (String playerId : session.getPlayerIds()) {
                    String clientId = playerRegistry.getClientIdForPlayer(playerId);
                    if (clientId != null) {
                        clients.add(clientId);
                    }
                }
            }

            return clients;
        }

        @Override
        public Set<String> getAllClients() {
            return new HashSet<>(playerRegistry.getAllClientIds());
        }
    }
}