package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Notification broadcast to all remaining players within a game session when a player leaves.
 */
public class PlayerLeftGameSessionNotification extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final String leftPlayerId;
    private final String leftPlayerNickname;
    private final List<PlayerInfoDTO> remainingPlayersInSession; // The list after the player has left
    private final boolean wasHost; // Indicates whether the leaving player was the host

    public PlayerLeftGameSessionNotification(String sessionId, String leftPlayerId, String leftPlayerNickname, 
                                           List<PlayerInfoDTO> remainingPlayersInSession, boolean wasHost) {
        super();
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId cannot be null");
        this.leftPlayerId = Objects.requireNonNull(leftPlayerId, "leftPlayerId cannot be null");
        this.leftPlayerNickname = Objects.requireNonNull(leftPlayerNickname, "leftPlayerNickname cannot be null");
        this.remainingPlayersInSession = new ArrayList<>(Objects.requireNonNull(remainingPlayersInSession, "remainingPlayersInSession cannot be null"));
        this.wasHost = wasHost;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getLeftPlayerId() {
        return leftPlayerId;
    }

    public String getLeftPlayerNickname() {
        return leftPlayerNickname;
    }

    public List<PlayerInfoDTO> getRemainingPlayersInSession() {
        return new ArrayList<>(remainingPlayersInSession);
    }
    
    /**
     * Returns whether the player who left was the host of the session.
     * @return true if the player was the host, false otherwise
     */
    public boolean wasHost() {
        return wasHost;
    }

    @Override
    public String toString() {
        return "PlayerLeftGameSessionNotification{" +
                "sessionId='" + sessionId + '\'' +
                ", leftPlayerNickname='" + leftPlayerNickname + '\'' +
                ", wasHost=" + wasHost +
                ", remainingPlayersInSessionCount=" + remainingPlayersInSession.size() +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}