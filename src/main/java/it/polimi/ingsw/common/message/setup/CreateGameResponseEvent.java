package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.dto.GameLobbyInfoDTO;
import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Event sent by the server in response to a CreateGameRequestCommand.
 * Indicates whether the game creation was successful and provides details if so.
 */
public class CreateGameResponseEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String sessionId; // Null if success is false
    private final String errorMessage; // Null if success is true
    private final GameLobbyInfoDTO newGameInfo; // Null if success is false
    private final List<PlayerInfoDTO> playersInLobby; // Null if success is false

    /**
     * Constructor for a successful game creation.
     * @param sessionId The ID of the newly created game session.
     * @param newGameInfo DTO containing information about the new game lobby.
     */
    public CreateGameResponseEvent(String sessionId, GameLobbyInfoDTO newGameInfo) {
        this(sessionId, newGameInfo, new ArrayList<>());
    }
    
    /**
     * Constructor for a successful game creation with player information.
     * @param sessionId The ID of the newly created game session.
     * @param newGameInfo DTO containing information about the new game lobby.
     * @param playersInLobby List of players currently in the lobby (including creator).
     */
    public CreateGameResponseEvent(String sessionId, GameLobbyInfoDTO newGameInfo, List<PlayerInfoDTO> playersInLobby) {
        super();
        this.success = true;
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId cannot be null for a successful creation");
        this.newGameInfo = Objects.requireNonNull(newGameInfo, "newGameInfo cannot be null for a successful creation");
        this.playersInLobby = new ArrayList<>(Objects.requireNonNull(playersInLobby, "playersInLobby cannot be null, use empty list if none"));
        this.errorMessage = null;
    }

    /**
     * Constructor for a failed game creation.
     * @param errorMessage A message explaining why the creation failed.
     */
    public CreateGameResponseEvent(String errorMessage) {
        super();
        this.success = false;
        this.sessionId = null;
        this.newGameInfo = null;
        this.playersInLobby = null;
        this.errorMessage = Objects.requireNonNull(errorMessage, "errorMessage cannot be null for a failed creation");
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

    public GameLobbyInfoDTO getNewGameInfo() {
        return newGameInfo;
    }
    
    public List<PlayerInfoDTO> getPlayersInLobby() {
        return playersInLobby != null ? new ArrayList<>(playersInLobby) : null;
    }

    @Override
    public String toString() {
        return "CreateGameResponseEvent{" +
                "success=" + success +
                ", sessionId='" + sessionId + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", newGameInfo=" + newGameInfo +
                ", playersCount=" + (playersInLobby != null ? playersInLobby.size() : 0) +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}