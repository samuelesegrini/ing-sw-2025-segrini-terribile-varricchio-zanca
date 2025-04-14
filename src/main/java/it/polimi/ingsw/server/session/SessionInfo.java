package it.polimi.ingsw.server.session;

import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.enums.flight.FlightStatus;

import java.util.Objects;
import java.util.UUID;

/**
 * Holds information about a player's session.
 * Includes the session token, player ID, game ID, last activity timestamp, and connection status.
 */
public class SessionInfo {

    private final PlayerId playerId;
    private final String gameId;
    private final String sessionToken;
    private volatile long lastActivity;
    private volatile FlightStatus status;

    /**
     * Constructs a new SessionInfo instance.
     *
     * @param playerId The player's unique identifier (as PlayerId object).
     * @param gameId   The identifier of the game the player is associated with.
     */
    public SessionInfo(PlayerId playerId, String gameId) {
        this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
        this.gameId = Objects.requireNonNull(gameId, "Game ID cannot be null");
        this.sessionToken = UUID.randomUUID().toString(); // Generate a unique token
        this.status = FlightStatus.RACING; // Start as connected
        this.lastActivity = System.currentTimeMillis();
    }

    /**
     * Updates the last activity timestamp to the current time.
     */
    public void updateLastActivity() {
        this.lastActivity = System.currentTimeMillis();
    }

    // --- Getters ---

    public PlayerId getPlayerId() {
        return playerId;
    }

    public String getGameId() {
        return gameId;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public FlightStatus getStatus() {
        return status;
    }

    // --- Setters ---

    public void setStatus(FlightStatus status) {
        this.status = Objects.requireNonNull(status, "ConnectionStatus cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionInfo that = (SessionInfo) o;
        return Objects.equals(playerId, that.playerId) && Objects.equals(gameId, that.gameId) && Objects.equals(sessionToken, that.sessionToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, gameId, sessionToken);
    }

    @Override
    public String toString() {
        return "SessionInfo{" +
                "playerId=" + playerId +
                ", gameId='" + gameId + '\'' +
                ", status=" + status +
                ", lastActivity=" + lastActivity +
                '}';
    }
}