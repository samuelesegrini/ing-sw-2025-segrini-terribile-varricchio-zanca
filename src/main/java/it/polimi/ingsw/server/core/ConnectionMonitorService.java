package it.polimi.ingsw.server.core;

import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.message.system.PingMessage;
import it.polimi.ingsw.server.network.ServerNetworkManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionMonitorService {
    private static final Logger LOGGER = Logger.getLogger(ConnectionMonitorService.class.getName());
    private static final long PING_INTERVAL_MS = 15000; // Send ping every 15 seconds
    private static final long PONG_TIMEOUT_MS = 10000;  // Wait 10 seconds for pong
    private static final int MAX_MISSED_PONGS = 3;      // Disconnect after 3 missed pongs

    private final ServerNetworkManager networkManager;
    private final EventBus serverEventBus;
    private final PlayerSessionRegistry playerSessionRegistry;
    // Map: networkClientId -> Last Ping Sent Timestamp
    private final Map<String, Long> outstandingPings = new ConcurrentHashMap<>();
    // Map: networkClientId -> Missed Pong Count
    private final Map<String, Integer> missedPongCounts = new ConcurrentHashMap<>();
    // Map: networkClientId -> Active GamePlayerId (for logging/events on disconnect)
    private final Map<String, String> networkIdToGamePlayerIdCache; // This comes from CommandContext / AuthController's maps
    private final GameSessionManager sessionManager;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r); t.setName("connection-monitor-scheduler"); t.setDaemon(true); return t;
    });

    public ConnectionMonitorService(ServerNetworkManager networkManager, EventBus serverEventBus,
                                    PlayerSessionRegistry playerSessionRegistry,
                                    GameSessionManager sessionManager, // << NEW PARAMETER
                                    Map<String, String> networkIdToGamePlayerIdCache) {
        this.networkManager = networkManager;
        this.serverEventBus = serverEventBus;
        this.playerSessionRegistry = playerSessionRegistry;
        this.sessionManager = sessionManager; // << ASSIGN
        this.networkIdToGamePlayerIdCache = networkIdToGamePlayerIdCache;
    }

    public void startMonitoring() {
        scheduler.scheduleAtFixedRate(this::checkConnectionsAndPing, 0, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
        LOGGER.info("ConnectionMonitorService started. Pinging clients every " + PING_INTERVAL_MS + "ms.");
    }

    public void stopMonitoring() {
        scheduler.shutdownNow();
        outstandingPings.clear();
        missedPongCounts.clear();
        LOGGER.info("ConnectionMonitorService stopped.");
    }

    private void checkConnectionsAndPing() {
        try {
            // Get all currently known network clients (those who have logged in)
            Set<String> currentlyKnownNetworkClients = networkIdToGamePlayerIdCache.keySet();

            long currentTime = System.currentTimeMillis();

            for (String networkClientId : currentlyKnownNetworkClients) {
                Long lastPingTime = outstandingPings.get(networkClientId);

                if (lastPingTime != null) { // Ping was sent, waiting for pong
                    if (currentTime - lastPingTime > PONG_TIMEOUT_MS) {
                        int missedCount = missedPongCounts.getOrDefault(networkClientId, 0) + 1;
                        missedPongCounts.put(networkClientId, missedCount);
                        outstandingPings.remove(networkClientId); // Allow new ping to be sent
                        LOGGER.warning("NetID: " + networkClientId + " missed pong. Strike " + missedCount + "/" + MAX_MISSED_PONGS);

                        if (missedCount >= MAX_MISSED_PONGS) {
                            handleUnresponsiveClient(networkClientId);
                            continue; // Skip sending new ping to this client
                        }
                    }
                }

                // If no outstanding ping for this client, send a new one
                if (!outstandingPings.containsKey(networkClientId)) {
                    if (networkManager.sendMessageToClient(networkClientId, new PingMessage(true))) {
                        outstandingPings.put(networkClientId, currentTime);
                        LOGGER.finer("Sent Ping to NetID: " + networkClientId);
                    } else {
                        // sendMessageToClient might return false if client is already known to be disconnected by adapter
                        LOGGER.warning("Failed to send Ping to NetID: " + networkClientId + ". Adapter might have already disconnected them.");
                        // The normal disconnect flow via SNM -> AuthController -> GameSession should handle this.
                        // We ensure maps here are cleaned by client no longer being in networkIdToGamePlayerIdCache.
                    }
                }
            }

            // Cleanup maps for clients no longer in networkIdToGamePlayerIdCache (disconnected)
            outstandingPings.keySet().retainAll(currentlyKnownNetworkClients);
            missedPongCounts.keySet().retainAll(currentlyKnownNetworkClients);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in ConnectionMonitorService checkConnectionsAndPing loop", e);
        }
    }

    public void recordPong(String networkClientId, long originalPingTimestamp) {
        if (outstandingPings.containsKey(networkClientId)) {
            // Check if pong is for the latest outstanding ping (optional, for stricter matching)
            // For simplicity, any pong resets the missed count for an active ping outstanding.
            outstandingPings.remove(networkClientId);
            missedPongCounts.put(networkClientId, 0); // Reset missed count
            long rtt = System.currentTimeMillis() - originalPingTimestamp;
            LOGGER.finer("Pong received from NetID: " + networkClientId + ". RTT: " + rtt + "ms. Outstanding ping cleared.");
        } else {
            LOGGER.finer("Received Pong from NetID: " + networkClientId + ", but no matching outstanding ping was recorded by monitor (or it timed out).");
        }
    }

    private void handleUnresponsiveClient(String networkClientId) {
        LOGGER.severe("NetID: " + networkClientId + " is unresponsive (max missed pongs). Marking as temporarily disconnected.");
        outstandingPings.remove(networkClientId);
        missedPongCounts.remove(networkClientId);

        String gamePlayerId = networkIdToGamePlayerIdCache.get(networkClientId);

        if (gamePlayerId != null) {
            String sessionId = playerSessionRegistry.getSessionIdForNetworkClient(networkClientId);
            GameSession session = (sessionId != null) ? this.sessionManager.getSession(sessionId) : null; // << USE INJECTED sessionManager

            if (session != null) {
                session.markPlayerAsTemporarilyDisconnected(gamePlayerId, networkClientId);
            } else {
                LOGGER.info("NetID " + networkClientId + " (PlayerID " + gamePlayerId + ") was unresponsive but not found in an active session via PSR for temporary disconnect event.");
            }
        } else {
            LOGGER.warning("NetID: " + networkClientId + " unresponsive, but no gamePlayerId mapping found. Removing from monitor.");
        }
        // The actual removal from global maps and triggering full disconnect flow happens via AuthController.handlePlayerDisconnect
        // which is called when the network adapter (e.g. socket) also physically detects the broken pipe.
        // This monitor primarily flags them in the GameSession for game logic purposes.
    }
}