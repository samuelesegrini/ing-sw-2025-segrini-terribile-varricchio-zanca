package it.polimi.ingsw.common.message.setup;


import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.model.GameSessionState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameSessionStateChangedEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final String oldStateName; // String name of the old GameSessionState
    private final String newStateName; // String name of the new GameSessionState
    private final List<PlayerInfoDTO> playersInSession; // Current players at time of state change

    public GameSessionStateChangedEvent(String sessionId,
                                        GameSessionState oldState, // Use server's enum for construction
                                        GameSessionState newState, // Use server's enum for construction
                                        List<PlayerInfoDTO> playersInSession) {
        super();
        this.sessionId = Objects.requireNonNull(sessionId);
        this.oldStateName = (oldState != null) ? oldState.name() : null; // Handle initial transition from null
        this.newStateName = Objects.requireNonNull(newState).name();
        this.playersInSession = new ArrayList<>(Objects.requireNonNull(playersInSession));
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getOldStateName() {
        return oldStateName;
    }

    public String getNewStateName() {
        return newStateName;
    }

    public List<PlayerInfoDTO> getPlayersInSession() {
        return new ArrayList<>(playersInSession);
    }

    @Override
    public String toString() {
        return "GameSessionStateChangedEvent{" +
                "sessionId='" + sessionId + '\'' +
                ", oldStateName='" + oldStateName + '\'' +
                ", newStateName='" + newStateName + '\'' +
                ", playersInSession=" + playersInSession.size() +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}