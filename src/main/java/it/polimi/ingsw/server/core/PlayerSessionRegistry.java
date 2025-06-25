package it.polimi.ingsw.server.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Enhanced player session registry that manages player sessions.
 */
public class PlayerSessionRegistry {
    private static final Logger LOGGER = Logger.getLogger(PlayerSessionRegistry.class.getName());

    private final Map<String, PlayerSession> clientToPlayerMap; // clientId -> PlayerSession
    private final Map<String, String> playerToClientMap; // playerId -> clientId
    private final Map<String, PlayerSession> playerSessions; // playerId -> PlayerSession
    private final Set<String> activeNicknames;
    private final Map<String, String> reconnectTokens; // playerId -> token

    public PlayerSessionRegistry() {
        this.clientToPlayerMap = new ConcurrentHashMap<>();
        this.playerToClientMap = new ConcurrentHashMap<>();
        this.playerSessions = new ConcurrentHashMap<>();
        this.activeNicknames = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.reconnectTokens = new ConcurrentHashMap<>();
    }

    /**
     * Registers a new player.
     */
    public boolean registerPlayer(String clientId, String playerId, String nickname) {
        // Check if nickname is already in use
        if (!activeNicknames.add(nickname)) {
            LOGGER.warning("Nickname " + nickname + " already in use");
            return false;
        }

        PlayerSession session = new PlayerSession(playerId, clientId, nickname);
        clientToPlayerMap.put(clientId, session);
        playerToClientMap.put(playerId, clientId);
        playerSessions.put(playerId, session);

        // Generate reconnect token
        String token = UUID.randomUUID().toString();
        reconnectTokens.put(playerId, token);

        LOGGER.info("Registered player " + nickname + " (ID: " + playerId + ")");
        return true;
    }

    /**
     * Unregisters a player.
     */
    public void unregisterPlayer(String clientId) {
        PlayerSession session = clientToPlayerMap.remove(clientId);
        if (session != null) {
            playerToClientMap.remove(session.playerId);
            activeNicknames.remove(session.nickname);

            // Keep session for reconnection
            session.setConnected(false);

            LOGGER.info("Unregistered player " + session.nickname);
        }
    }

    /**
     * Checks if a nickname is in use.
     */
    public boolean isNicknameInUse(String nickname) {
        return activeNicknames.contains(nickname);
    }

    /**
     * Gets player ID for a client.
     */
    public String getPlayerIdForClient(String clientId) {
        PlayerSession session = clientToPlayerMap.get(clientId);
        System.out.println("[DEBUG] PlayerSessionRegistry.getPlayerIdForClient - Client ID: " + clientId);
        System.out.println("[DEBUG] PlayerSessionRegistry.getPlayerIdForClient - Session found: " + (session != null));
        if (session != null) {
            System.out.println("[DEBUG] PlayerSessionRegistry.getPlayerIdForClient - Player ID: " + session.playerId);
        } else {
            System.out.println("[DEBUG] PlayerSessionRegistry.getPlayerIdForClient - Available client mappings:");
            clientToPlayerMap.forEach((key, value) -> 
                System.out.println("[DEBUG]   - Client: " + key + " -> Player: " + value.playerId));
        }
        return session != null ? session.playerId : null;
    }

    /**
     * Gets client ID for a player.
     */
    public String getClientIdForPlayer(String playerId) {
        return playerToClientMap.get(playerId);
    }

    /**
     * Gets player nickname.
     */
    public String getPlayerNickname(String playerId) {
        PlayerSession session = playerSessions.get(playerId);
        return session != null ? session.nickname : null;
    }

    /**
     * Validates reconnection attempt.
     */
    public boolean validateReconnection(String playerId, String token) {
        String storedToken = reconnectTokens.get(playerId);
        return storedToken != null && storedToken.equals(token);
    }

    /**
     * Restores a player session after reconnection.
     */
    public void restoreSession(String newClientId, String playerId) {
        PlayerSession session = playerSessions.get(playerId);
        if (session != null) {
            // Remove old mapping if exists
            String oldClientId = playerToClientMap.get(playerId);
            if (oldClientId != null) {
                clientToPlayerMap.remove(oldClientId);
            }

            // Create new mapping
            session.clientId = newClientId;
            session.setConnected(true);
            clientToPlayerMap.put(newClientId, session);
            playerToClientMap.put(playerId, newClientId);

            LOGGER.info("Restored session for player " + session.nickname);
        }
    }

    /**
     * Checks if a player is registered.
     */
    public boolean isPlayerRegistered(String clientId) {
        return clientToPlayerMap.containsKey(clientId);
    }

    /**
     * Gets all client IDs.
     */
    public Set<String> getAllClientIds() {
        return new HashSet<>(clientToPlayerMap.keySet());
    }

    /**
     * Gets player info.
     */
    public Map<String, String> getPlayerInfo(String clientId) {
        PlayerSession session = clientToPlayerMap.get(clientId);
        if (session != null) {
            Map<String, String> info = new HashMap<>();
            info.put("playerId", session.playerId);
            info.put("nickname", session.nickname);
            info.put("connected", String.valueOf(session.isConnected));
            return info;
        }
        return null;
    }

    /**
     * Inner class representing a player session.
     */
    private static class PlayerSession {
        private final String playerId;
        private String clientId;
        private final String nickname;
        private volatile boolean isConnected;

        public PlayerSession(String playerId, String clientId, String nickname) {
            this.playerId = playerId;
            this.clientId = clientId;
            this.nickname = nickname;
            this.isConnected = true;
        }

        public void setConnected(boolean connected) {
            this.isConnected = connected;
        }
    }
}