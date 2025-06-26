package it.polimi.ingsw.server.core;

import it.polimi.ingsw.server.model.domain.player.PlayerId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Enhanced player session registry that manages player sessions.
 */
public class PlayerSessionRegistry {
    private static final Logger LOGGER = Logger.getLogger(PlayerSessionRegistry.class.getName());

    private final Map<String, PlayerSession> clientToPlayerMap; // clientId -> PlayerSession
    private final Map<PlayerId, String> playerToClientMap; // playerId -> clientId
    private final Map<PlayerId, PlayerSession> playerSessions; // playerId -> PlayerSession
    private final Set<String> activeNicknames;
    private final Map<PlayerId, String> reconnectTokens; // playerId -> token

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
    public boolean registerPlayer(String clientId, PlayerId playerId, String nickname) {
        if (clientId == null || clientId.isEmpty() || playerId == null || nickname == null || nickname.isEmpty()) {
            LOGGER.warning("registerPlayer called with null or empty parameters");
            return false;
        }
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
        if (clientId == null || clientId.isEmpty()) {
            LOGGER.warning("unregisterPlayer called with null or empty clientId");
            return;
        }
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
        if (nickname == null || nickname.isEmpty()) {
            LOGGER.warning("isNicknameInUse called with null or empty nickname");
            return false;
        }
        return activeNicknames.contains(nickname);
    }

    /**
     * Gets player ID for a client.
     */
    public PlayerId getPlayerIdForClient(String clientId) {
        if( clientId == null || clientId.isEmpty()) {
            LOGGER.warning("getPlayerIdForClient called with null or empty clientId");
            return null;
        }
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
    public String getClientIdForPlayer(PlayerId playerId) {
        if (playerId == null) {
            LOGGER.warning("getClientIdForPlayer called with null playerId");
            return null;
        }
        return playerToClientMap.get(playerId);
    }
    

    /**
     * Gets player nickname.
     */
    public String getPlayerNickname(PlayerId playerId) {
        if (playerId == null ) {
            LOGGER.warning("getPlayerNickname called with null playerId");
            return null;
        }
        PlayerSession session = playerSessions.get(playerId);
        return session != null ? session.nickname : null;
    }
    
    /**
     * Gets player nickname (legacy String overload).
     */
    public String getPlayerNickname(String playerIdString) {
        if (playerIdString == null || playerIdString.isEmpty()) {
            LOGGER.warning("getPlayerNickname called with null or empty playerIdString");
            return null;
        }
        PlayerId playerId = PlayerId.fromString(playerIdString);
        return getPlayerNickname(playerId);
    }

    /**
     * Validates reconnection attempt.
     */
    public boolean validateReconnection(PlayerId playerId, String token) {
        if (playerId == null || token == null || token.isEmpty()) {
            LOGGER.warning("validateReconnection called with null or empty parameters");
            return false;
        }
        String storedToken = reconnectTokens.get(playerId);
        return storedToken != null && storedToken.equals(token);
    }
    
    /**
     * Validates reconnection attempt (legacy String overload).
     */
    public boolean validateReconnection(String playerIdString, String token) {
        if (playerIdString == null || playerIdString.isEmpty() || token == null || token.isEmpty()) {
            LOGGER.warning("validateReconnection called with null or empty parameters");
            return false;
        }
        PlayerId playerId = PlayerId.fromString(playerIdString);
        return validateReconnection(playerId, token);
    }

    /**
     * Restores a player session after reconnection.
     */
    public void restoreSession(String newClientId, PlayerId playerId) {
        if (newClientId == null || newClientId.isEmpty() || playerId == null) {
            LOGGER.warning("restoreSession called with null or empty parameters");
            return;
        }
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
     * Restores a player session after reconnection (legacy String overload).
     */
    public void restoreSession(String newClientId, String playerIdString) {
        if (newClientId == null || newClientId.isEmpty() || playerIdString == null || playerIdString.isEmpty()) {
            LOGGER.warning("restoreSession called with null or empty parameters");
            return;
        }
        PlayerId playerId = PlayerId.fromString(playerIdString);
        restoreSession(newClientId, playerId);
    }

    /**
     * Checks if a player is registered.
     */
    public boolean isPlayerRegistered(String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            LOGGER.warning("isPlayerRegistered called with null or empty clientId");
            return false;
        }
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
        if (clientId == null || clientId.isEmpty()) {
            LOGGER.warning("getPlayerInfo called with null or empty clientId");
            return null;
        }
        PlayerSession session = clientToPlayerMap.get(clientId);
        if (session != null) {
            Map<String, String> info = new HashMap<>();
            info.put("playerId", session.playerId.toString());
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
        private final PlayerId playerId;
        private String clientId;
        private final String nickname;
        private volatile boolean isConnected;

        public PlayerSession(PlayerId playerId, String clientId, String nickname) {
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