package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Event sent by the server in response to a JoinGameRequestCommand.
 * Indicates success or failure and provides current lobby player list if successful.
 */
public class JoinGameResponseEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String sessionId; // Can be null if success is false and original request was malformed
    private final String errorMessage; // Null if success is true
    private final List<PlayerInfoDTO> playersInThisLobby; // Null if success is false

    /**
     * Constructor for a successful game join.
     * @param sessionId The ID of the game session joined.
     * @param playersInThisLobby The list of players currently in the lobby.
     */
    public JoinGameResponseEvent(String sessionId, List<PlayerInfoDTO> playersInThisLobby) {
        super();
        this.success = true;
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId cannot be null for a successful join");
        this.playersInThisLobby = new ArrayList<>(Objects.requireNonNull(playersInThisLobby, "playersInThisLobby cannot be null for a successful join"));
        this.errorMessage = null;
    }

    /**
     * Constructor for a failed game join.
     * @param sessionId The ID of the session attempted to join (can be null if original request was malformed).
     * @param errorMessage A message explaining why joining failed.
     */
    public JoinGameResponseEvent(String sessionId, String errorMessage) {
        super();
        this.success = false;
        this.sessionId = sessionId;
        this.playersInThisLobby = null;
        this.errorMessage = Objects.requireNonNull(errorMessage, "errorMessage cannot be null for a failed join");
    }

    public boolean isSuccess() {
        return success;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<PlayerInfoDTO> getPlayersInThisLobby() {
        if (playersInThisLobby == null) {
            return null;
        }
        return new ArrayList<>(playersInThisLobby);
    }

    @Override
    public String toString() {
        return "JoinGameResponseEvent{" +
                "success=" + success +
                ", sessionId='" + sessionId + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", playersInThisLobby=" + (playersInThisLobby != null ? playersInThisLobby.size() : "null") +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}