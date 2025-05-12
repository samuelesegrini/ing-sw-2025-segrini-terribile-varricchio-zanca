package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;

import java.util.List;
import java.util.ArrayList; // For defensive copy
import java.util.Objects;

public class LobbyStateUpdateEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final List<PlayerInfoDTO> playersInLobby;
    private final String gameSessionId; // To identify which lobby/game this update is for

    public LobbyStateUpdateEvent(String gameSessionId, List<PlayerInfoDTO> playersInLobby) {
        super();
        this.gameSessionId = gameSessionId;
        // Defensive copy to ensure immutability of the event's list content
        this.playersInLobby = new ArrayList<>(Objects.requireNonNull(playersInLobby));
    }

    public String getGameSessionId() {
        return gameSessionId;
    }

    public List<PlayerInfoDTO> getPlayersInLobby() {
        // Return a copy or an unmodifiable list to maintain encapsulation
        return new ArrayList<>(playersInLobby);
    }

    @Override
    public String toString() {
        return "LobbyStateUpdateEvent{" +
                "gameSessionId='" + gameSessionId + '\'' +
                ", playersInLobby=" + playersInLobby +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}