package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Notification broadcast to all players within a game session when a new player joins.
 */
public class PlayerJoinedGameSessionNotification extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final PlayerInfoDTO joinedPlayer;
    private final List<PlayerInfoDTO> allPlayersInSession; // The complete list after the join

    public PlayerJoinedGameSessionNotification(String sessionId, PlayerInfoDTO joinedPlayer, List<PlayerInfoDTO> allPlayersInSession) {
        super();
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId cannot be null");
        this.joinedPlayer = Objects.requireNonNull(joinedPlayer, "joinedPlayer cannot be null");
        this.allPlayersInSession = new ArrayList<>(Objects.requireNonNull(allPlayersInSession, "allPlayersInSession cannot be null"));
    }

    public String getSessionId() {
        return sessionId;
    }

    public PlayerInfoDTO getJoinedPlayer() {
        return joinedPlayer; // PlayerInfoDTO is immutable for its core fields
    }

    public List<PlayerInfoDTO> getAllPlayersInSession() {
        return new ArrayList<>(allPlayersInSession);
    }

    @Override
    public String toString() {
        return "PlayerJoinedGameSessionNotification{" +
                "sessionId='" + sessionId + '\'' +
                ", joinedPlayer=" + joinedPlayer.getNickname() +
                ", allPlayersInSessionCount=" + allPlayersInSession.size() +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}