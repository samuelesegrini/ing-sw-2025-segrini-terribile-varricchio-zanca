package it.polimi.ingsw.server.core;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages the association between players (identified by their networkClientId or gamePlayerId)
 * and the game sessions they are currently part of.
 */
public class PlayerSessionRegistry {
    private static final Logger LOGGER = Logger.getLogger(PlayerSessionRegistry.class.getName());

    // Maps networkClientId to the sessionId they are currently in
    private final Map<String, String> networkClientToSessionMap = new ConcurrentHashMap<>();
    // Maps gamePlayerId to the sessionId they are currently in
    private final Map<String, String> gamePlayerToSessionMap = new ConcurrentHashMap<>();
    // Maps sessionId to a set of networkClientIds currently in that session
    private final Map<String, Set<String>> sessionToNetworkClientsMap = new ConcurrentHashMap<>();

    public PlayerSessionRegistry() {
        LOGGER.info("PlayerSessionRegistry initialized.");
    }

    /**
     * Registers a player (by networkClientId) into a specific game session.
     * Also maps the gamePlayerId if provided and distinct.
     *
     * @param networkClientId The player's network identifier.
     * @param gamePlayerId    The player's game-specific identifier (can be same as network ID initially).
     * @param sessionId       The ID of the session the player is joining.
     */
    public synchronized void registerPlayerInSession(String networkClientId, String gamePlayerId, String sessionId) {
        Objects.requireNonNull(networkClientId, "networkClientId cannot be null");
        Objects.requireNonNull(gamePlayerId, "gamePlayerId cannot be null");
        Objects.requireNonNull(sessionId, "sessionId cannot be null");

        // Clean up old session if player was in one
        removePlayerMappings(networkClientId, gamePlayerId);

        networkClientToSessionMap.put(networkClientId, sessionId);
        gamePlayerToSessionMap.put(gamePlayerId, sessionId);
        sessionToNetworkClientsMap.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(networkClientId);

        LOGGER.info("Player " + gamePlayerId + " (NetID: " + networkClientId + ") registered in session " + sessionId);
    }


    /**
     * Removes a player from any session they might be part of, using their networkClientId.
     *
     * @param networkClientId The network identifier of the player to remove.
     */
    public synchronized void removePlayerFromAnySessionByNetworkId(String networkClientId) {
        Objects.requireNonNull(networkClientId, "networkClientId cannot be null");
        String sessionId = networkClientToSessionMap.remove(networkClientId);
        if (sessionId != null) {
            Set<String> clientsInSession = sessionToNetworkClientsMap.get(sessionId);
            if (clientsInSession != null) {
                clientsInSession.remove(networkClientId);
                if (clientsInSession.isEmpty()) {
                    sessionToNetworkClientsMap.remove(sessionId);
                    LOGGER.finer("Session " + sessionId + " is now empty of network clients.");
                }
            }
            // Find and remove associated gamePlayerId mapping only if no other network client uses it for this session
            gamePlayerToSessionMap.entrySet().removeIf(entry ->
                    entry.getValue().equals(sessionId) &&
                            !isGamePlayerAssociatedWithOtherNetworkClientInSession(entry.getKey(), sessionId, networkClientId)
            );

            LOGGER.info("Player (NetID: " + networkClientId + ") removed from session " + sessionId);
        } else {
            LOGGER.finer("Player (NetID: " + networkClientId + ") was not found in any session via networkClientToSessionMap.");
        }
    }

    /**
     * Removes all mappings for a given network and game player ID.
     */
    private synchronized void removePlayerMappings(String networkClientId, String gamePlayerId) {
        String oldSessionNet = networkClientToSessionMap.remove(networkClientId);
        if (oldSessionNet != null) {
            Set<String> clientsInSession = sessionToNetworkClientsMap.get(oldSessionNet);
            if (clientsInSession != null) {
                clientsInSession.remove(networkClientId);
                if (clientsInSession.isEmpty()) sessionToNetworkClientsMap.remove(oldSessionNet);
            }
        }

        String oldSessionGame = gamePlayerToSessionMap.remove(gamePlayerId);
        if (oldSessionGame != null && (oldSessionNet == null || !oldSessionGame.equals(oldSessionNet))) {
            // If gamePlayer was in a different session, or network client wasn't mapped
            // but gamePlayer was, clean up that session's view of network clients
            // This scenario should be rare with proper login handling
            Set<String> clientsInGamePlayerSession = sessionToNetworkClientsMap.get(oldSessionGame);
            if (clientsInGamePlayerSession != null) {
                // Remove any networkClient that points to this gamePlayerId
                // This is complex: better to ensure removePlayerFromAllSessions(gamePlayerId) in GameSession clears this.
                // For now, this keeps it simple: assumes a gamePlayerId is uniquely tied or re-tied on login/join.
            }
        }
    }


    private boolean isGamePlayerAssociatedWithOtherNetworkClientInSession(String gamePlayerId, String sessionId, String excludedNetworkClientId) {
        // This helper is tricky. For simplicity, assume one gamePlayerId maps to one active networkClient
        // In a rejoin scenario, the old networkClientId would be dissociated first by the Login/Auth Controller.
        // Thus, if a gamePlayerId exists in gamePlayerToSessionMap, it's for the *current* network client.
        return false; // Simplified: assume for now a gamePlayerId has one net client active.
    }

    /**
     * Retrieves the session ID for a player, given their networkClientId.
     *
     * @param networkClientId The player's network identifier.
     * @return The session ID, or null if the player is not in any session.
     */
    public String getSessionIdForNetworkClient(String networkClientId) {
        return networkClientToSessionMap.get(networkClientId);
    }

    /**
     * Retrieves the session ID for a player, given their gamePlayerId.
     *
     * @param gamePlayerId The player's game-specific identifier.
     * @return The session ID, or null if the player is not in any session.
     */
    public String getSessionIdForGamePlayer(String gamePlayerId) {
        return gamePlayerToSessionMap.get(gamePlayerId);
    }

    /**
     * Gets all network client IDs associated with a given session.
     *
     * @param sessionId The ID of the session.
     * @return A new Set containing the network client IDs, or an empty set if the session has no players or is unknown.
     */
    public Set<String> getNetworkClientIdsForSession(String sessionId) {
        Set<String> clients = sessionToNetworkClientsMap.get(sessionId);
        return (clients != null) ? Set.copyOf(clients) : Set.of();
    }

    public synchronized void removeGamePlayerFromSessionMap(String gamePlayerId, String sessionId) {
        String currentSession = gamePlayerToSessionMap.get(gamePlayerId);
        if (Objects.equals(currentSession, sessionId)) {
            gamePlayerToSessionMap.remove(gamePlayerId);
            LOGGER.finer("Removed GamePlayerID " + gamePlayerId + " from session map for session " + sessionId);
        }
    }
}