package it.polimi.ingsw.server.monitor;

import it.polimi.ingsw.common.message.PingMessage;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.network.ServerNetworkManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Service that monitors client connections and handles keep-alive.
 * On timeout, it invokes a callback to handle the disconnection.
 */
public class ConnectionMonitorService {
    private static final Logger LOGGER = Logger.getLogger(ConnectionMonitorService.class.getName());
    
    private final ConnectionConfig config;
    private final ServerNetworkManager networkManager;
    private final PlayerSessionRegistry playerRegistry;
    private final GameSessionManager sessionManager;
    private final Map<String, String> networkClientToGamePlayerMap;
    private final Consumer<String> onClientTimeoutCallback; // ADDED: Callback for handling timeouts
    private final Map<String, ConnectionStatus> connectionStatuses;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> monitoringTask;
    public ConnectionMonitorService(ServerNetworkManager networkManager,
                                    PlayerSessionRegistry playerRegistry,
                                    GameSessionManager sessionManager,
                                    Map<String, String> networkClientToGamePlayerMap,
                                    Consumer<String> onClientTimeoutCallback) {
        this(networkManager, playerRegistry, sessionManager, networkClientToGamePlayerMap, 
             onClientTimeoutCallback, new ConnectionConfig());
    }
    
    public ConnectionMonitorService(ServerNetworkManager networkManager,
                                    PlayerSessionRegistry playerRegistry,
                                    GameSessionManager sessionManager,
                                    Map<String, String> networkClientToGamePlayerMap,
                                    Consumer<String> onClientTimeoutCallback,
                                    ConnectionConfig config) {
        this.config = config;
        this.networkManager = networkManager;
        this.playerRegistry = playerRegistry;
        this.sessionManager = sessionManager;
        this.networkClientToGamePlayerMap = networkClientToGamePlayerMap;
        this.onClientTimeoutCallback = onClientTimeoutCallback;
        this.connectionStatuses = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r);
            t.setName("connection-monitor");
            t.setDaemon(true);
            return t;
        });
    }
    /**
     * Starts monitoring connections.
     */
    public void startMonitoring() {
        LOGGER.info("Starting connection monitoring service with config: " + config);
        monitoringTask = scheduler.scheduleAtFixedRate(
                this::checkConnections,
                config.getPingIntervalMs(),
                config.getPingIntervalMs(),
                TimeUnit.MILLISECONDS
        );
    }
    /**
     Stops monitoring connections.
     */
    public void stopMonitoring() {
        LOGGER.info("Stopping connection monitoring service");
        if (monitoringTask != null) {
            monitoringTask.cancel(false);
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    /**
     Checks all connections.
     */
    private void checkConnections() {
        // We need to check all network-level connections, not just authenticated players
        Set<String> clientIds = networkManager.getAllConnectedClientIds();
        if (clientIds.isEmpty()) {
            LOGGER.fine("No connected clients to check.");
            return;
        }

        LOGGER.finer("Checking connections for clients: " + clientIds);
        for (String clientId : clientIds) {
            checkClientConnection(clientId);
        }
    }
    /**
     Checks a single client connection.
     */
    private void checkClientConnection(String clientId) {
        ConnectionStatus status = connectionStatuses.computeIfAbsent(
                clientId,
                k -> new ConnectionStatus()
        );
        if (status.waitingForPong) {
            long timeSinceLastPing = System.currentTimeMillis() - status.lastPingTime;
            if (timeSinceLastPing > config.getPingTimeoutMs()) {
                LOGGER.warning("Client " + clientId + " timed out (no pong response after " + 
                             timeSinceLastPing + "ms)");
                handleClientTimeout(clientId);
                return;
            }
        }
        sendPing(clientId, status);
    }
    /**
     Sends a ping to a client.
     */
    private void sendPing(String clientId, ConnectionStatus status) {
        PingMessage ping = new PingMessage();
        boolean sent = networkManager.sendMessageToClient(clientId, ping);
        if (sent) {
            status.lastPingTime = System.currentTimeMillis();
            status.waitingForPong = true;
        } else {
            LOGGER.warning("Failed to send ping to client " + clientId + ". It might be disconnected.");
        }
    }
    /**
     Handles a pong response from a client.
     */
    public void handlePong(String clientId) {
        ConnectionStatus status = connectionStatuses.get(clientId);
        if (status != null) {
            status.waitingForPong = false;
            status.lastPongTime = System.currentTimeMillis();
        }
    }
    /**
     Handles a client timeout by invoking the disconnection callback.
     */
    private void handleClientTimeout(String clientId) {
        connectionStatuses.remove(clientId);
        onClientTimeoutCallback.accept(clientId);
    }
    /**
     Inner class to track connection status.
     */
    private static class ConnectionStatus {
        private long lastPingTime = 0;
        private long lastPongTime = 0;
        private boolean waitingForPong = false;
    }
}