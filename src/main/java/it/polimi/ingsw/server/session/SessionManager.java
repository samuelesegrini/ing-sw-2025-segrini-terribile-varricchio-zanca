package it.polimi.ingsw.server.session;

import java.util.List;
import java.util.Map; // Assuming PlayerId is in this package
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.enums.flight.FlightStatus;

/**
 * Manages player sessions globally across all games.
 * Tracks connection status, validates session tokens for reconnection,
 * and handles session timeouts. This class is thread-safe.
 */
public class SessionManager {

    private static final Logger logger = Logger.getLogger(SessionManager.class.getName());

    // Map: PlayerId -> SessionInfo
    private final Map<PlayerId, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    // Map: PlayerId -> GameId (Optional, useful for routing heartbeats/disconnects if needed outside GIC)
    private final Map<PlayerId, String> playerGameMap = new ConcurrentHashMap<>();
    // Callback to notify relevant component (e.g., MultiGameCoordinator) when a player session is definitively removed (e.g., due to timeout)
    private final Consumer<PlayerId> playerRemovalCallback;


    public SessionManager(Consumer<PlayerId> playerRemovalCallback) {
        this.playerRemovalCallback = playerRemovalCallback != null ? playerRemovalCallback : (pid) -> {}; // No-op callback if null
    }

    /**
     * Registers a new session for a player joining a specific game.
     * If the player already has a session, it logs a warning but proceeds (e.g., client error?).
     *
     * @param playerId The ID of the player.
     * @param gameId   The ID of the game the player is joining.
     * @return The newly created SessionInfo containing the session token.
     */
    public SessionInfo registerNewSession(PlayerId playerId, String gameId) {
        if (activeSessions.containsKey(playerId)) {
            logger.log(Level.WARNING, "Player {0} already has an active session. Overwriting for game {1}.", new Object[]{playerId, gameId});
            // Decide if old session needs cleanup/notification
        }
        SessionInfo newSession = new SessionInfo(playerId, gameId);
        activeSessions.put(playerId, newSession);
        playerGameMap.put(playerId, gameId);
        logger.log(Level.INFO, "Registered new session for Player {0} in Game {1}. Token: {2}", new Object[]{playerId, gameId, newSession.getSessionToken()});
        return newSession;
    }

    /**
     * Validates a player's session token, typically used during reconnection attempts.
     * Also checks if the player is currently marked as DISCONNECTED.
     *
     * @param playerId     The ID of the player.
     * @param sessionToken The session token provided by the client.
     * @return true if the session exists, the token matches, and the status is DISCONNECTED, false otherwise.
     */
    public boolean validateReconnectionAttempt(PlayerId playerId, String sessionToken) {
        SessionInfo session = activeSessions.get(playerId);
        if (session != null && session.getSessionToken().equals(sessionToken) && session.getStatus() == FlightStatus.ABANDONED) {
            logger.log(Level.INFO, "Session validated successfully for reconnection: Player {0}", playerId);
            return true;
        }
        logger.log(Level.WARNING, "Session validation failed for reconnection attempt: Player {0}. Token match: {1}, Status DISCONNECTED: {2}",
                new Object[]{playerId, session != null && session.getSessionToken().equals(sessionToken), session != null && session.getStatus() == FlightStatus.ABANDONED});
        return false;
    }

    /**
     * Validates if a session exists and the token matches, regardless of connection status.
     * Used for validating ongoing actions from a client claiming to be connected.
     *
     * @param playerId The ID of the player.
     * @param sessionToken The session token provided by the client for an action.
     * @return true if the session exists and the token matches, false otherwise.
     */
    public boolean validateSessionToken(PlayerId playerId, String sessionToken) {
        SessionInfo session = activeSessions.get(playerId);
        boolean isValid = session != null && session.getSessionToken().equals(sessionToken);
        if (!isValid) {
            logger.log(Level.FINE, "Session token validation failed for action: Player {0}", playerId);
        }
        return isValid;
    }


    /**
     * Marks a player's session as DISCONNECTED. Does not remove the session.
     *
     * @param playerId The ID of the player who disconnected.
     */
    public void registerDisconnection(PlayerId playerId) {
        SessionInfo session = activeSessions.get(playerId);
        if (session != null) {
            session.setStatus(FlightStatus.ABANDONED);
            logger.log(Level.INFO, "Player {0} marked as DISCONNECTED.", playerId);
        } else {
            logger.log(Level.WARNING, "Attempted to register disconnection for non-existent session: Player {0}", playerId);
        }
    }

    /**
     * Marks a player's session as CONNECTED. Typically called after successful validation/reconnection.
     * Also updates the last activity timestamp.
     *
     * @param playerId The ID of the player who connected/reconnected.
     */
    public void registerConnection(PlayerId playerId) {
        SessionInfo session = activeSessions.get(playerId);
        if (session != null) {
            session.setStatus(FlightStatus.RACING);
            session.updateLastActivity(); // Update activity on connect/reconnect
            logger.log(Level.INFO, "Player {0} marked as CONNECTED.", playerId);
        } else {
            logger.log(Level.WARNING, "Attempted to register connection for non-existent session: Player {0}", playerId);
        }
    }

    /**
     * Updates the last activity timestamp for a player's session, usually upon receiving a heartbeat or any valid message.
     *
     * @param playerId     The ID of the player.
     * @param sessionToken The session token provided (for validation).
     * @return true if the session was found, token matched, and activity was updated, false otherwise.
     */
    public boolean updateLastActivity(PlayerId playerId, String sessionToken) {
        SessionInfo session = activeSessions.get(playerId);
        if (session != null && session.getSessionToken().equals(sessionToken)) {
            session.updateLastActivity();
            return true;
        }
        // Log failure only if session exists but token is wrong, otherwise it might just be an old message
        if (session != null && !session.getSessionToken().equals(sessionToken)) {
            logger.log(Level.FINE, "Invalid session token provided for heartbeat/activity update for player {0}", playerId);
        }
        return false;
    }

    /**
     * Checks all active sessions for timeouts based on the last activity time.
     * Removes timed-out sessions and returns a list of PlayerIds that were removed.
     *
     * @param timeoutMillis The maximum inactivity time allowed in milliseconds.
     * @return A list of PlayerIds whose sessions were removed due to timeout.
     */
    public List<PlayerId> checkTimeouts(long timeoutMillis) {
        long now = System.currentTimeMillis();
        List<PlayerId> removedPlayers = activeSessions.values().stream()
                .filter(session -> (now - session.getLastActivity()) > timeoutMillis)
                .map(SessionInfo::getPlayerId)
                .collect(Collectors.toList());

        if (!removedPlayers.isEmpty()) {
            logger.log(Level.INFO, "Detected {0} timed-out player sessions.", removedPlayers.size());
            removedPlayers.forEach(this::removePlayerSessionInternal);
        }
        return removedPlayers;
    }

    /**
     * Completely removes a player's session information. Typically called when a game ends,
     * a player explicitly leaves, or after a timeout.
     *
     * @param playerId The ID of the player whose session should be removed.
     */
    public void removePlayerSession(PlayerId playerId) {
        removePlayerSessionInternal(playerId);
    }

    /**
     * Internal method to remove session and trigger callback.
     */
    private void removePlayerSessionInternal(PlayerId playerId) {
        SessionInfo removed = activeSessions.remove(playerId);
        playerGameMap.remove(playerId);
        if (removed != null) {
            logger.log(Level.INFO, "Removed session for Player {0} (was in game {1}).", new Object[]{playerId, removed.getGameId()});
            // Notify relevant component (e.g., MultiGameCoordinator) about the removal
            playerRemovalCallback.accept(playerId);
        } else {
            logger.log(Level.WARNING, "Attempted to remove non-existent session: Player {0}", playerId);
        }
    }

    /**
     * Retrieves the Game ID associated with a given Player ID.
     *
     * @param playerId The ID of the player.
     * @return The Game ID as a String, or null if the player is not found in the map.
     */
    public String getGameIdForPlayer(PlayerId playerId) {
        return playerGameMap.get(playerId);
    }

    /**
     * Gets the current connection status of a player.
     *
     * @param playerId The ID of the player.
     * @return The ConnectionStatus, or null if the player has no active session.
     */
    public FlightStatus getPlayerStatus(PlayerId playerId) {
        SessionInfo session = activeSessions.get(playerId);
        return (session != null) ? session.getStatus() : null;
    }

}