package it.polimi.ingsw.common.message;

import it.polimi.ingsw.common.message.event.Event;
import it.polimi.ingsw.common.message.event.EventFilterContext;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.network.ServerNetworkManager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Event publisher implementation.
 */
public class EventPublisherImpl implements EventPublisher, PropertyChangeListener {
    private static final Logger LOGGER = Logger.getLogger(EventPublisherImpl.class.getName());

    private final ServerNetworkManager networkManager;
    private final PlayerSessionRegistry playerRegistry;
    private final GameSessionManager sessionManager;
    private final ExecutorService eventExecutor;
    private final EventFilterContextImpl filterContext;

    /**
     * constructor
     *
     * @param networkManager The network manager
     * @param playerRegistry The player registry
     * @param sessionManager The session manager
     */

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

    /**
     *
     * @param event The event to publish
     */

    @Override
    public void publishEvent(Event event) {
        LOGGER.fine("Publishing event: " + event.getEventType() +
                " for game: " + event.getGameId());

        // Distribute asynchronously
        eventExecutor.submit(() -> distributeEvent(event));
    }

    /**
     *
     * @param event The event to publish
     * @param clientId The target client ID
     */

    @Override
    public void publishEventToClient(Event event, String clientId) {

    }

    /**
     *
     * @param event The event to publish
     * @param gameId The game ID
     */

    @Override
    public void publishEventToGame(Event event, String gameId) {

    }

    /**
     * Handles property changes from various sources. If the new value of the
     * property change is an Event, it will be published.
     * @param evt The PropertyChangeEvent object.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() instanceof Event) {
            Event event = (Event) evt.getNewValue();
            LOGGER.fine("Received event from PropertyChange: " + event.getEventType() +
                    " from " + evt.getSource().getClass().getSimpleName());
            publishEvent(event);
        } else {
            // Log events ignored, in case of misconfiguration.
            LOGGER.finer("Ignoring property change '" + evt.getPropertyName() + "' as its new value is not an Event instance.");
        }
    }


    /**
     *
     * @param event The event to distribute
     */

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

    /**
     *
     * @param event
     * @return the potential recipients of the event
     */

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

        /**
         *
         * @param clientId The client ID
         * @param gameId The game ID
         * @return true if the client is in the game, false otherwise
         */

        @Override
        public boolean isClientInGame(String clientId, String gameId) {
            PlayerId playerId = playerRegistry.getPlayerIdForClient(clientId);
            if (playerId == null) return false;

            GameSession session = sessionManager.getGameSessionForPlayer(playerId);
            return session != null && session.getGameId().equals(gameId);
        }

        /**
         *
         * @param clientId The client ID
         * @return The player ID
         */

        @Override
        public PlayerId getPlayerIdForClient(String clientId) {
            return playerRegistry.getPlayerIdForClient(clientId);
        }

        /**
         *
         * @param gameId The game ID
         * @return a set of the clients in the game
         */

        @Override
        public Set<String> getClientsInGame(String gameId) {
            Set<String> clients = new HashSet<>();
            GameSession session = sessionManager.getGameSession(gameId);

            if (session != null) {
                for (PlayerId playerId : session.getPlayerIds()) {
                    String clientId = playerRegistry.getClientIdForPlayer(playerId);
                    if (clientId != null) {
                        clients.add(clientId);
                    }
                }
            }

            return clients;
        }

        /**
         *
         * @return a set containing all the clients
         */

        @Override
        public Set<String> getAllClients() {
            return new HashSet<>(playerRegistry.getAllClientIds());
        }
    }
}