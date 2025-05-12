package it.polimi.ingsw.common.dto;

import it.polimi.ingsw.common.model.GameSessionState;
import it.polimi.ingsw.server.model.enums.GameLevel;

import java.io.Serializable;
import java.util.Objects;

public class GameLobbyInfoDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final String gameName;
    private final int currentPlayerCount;
    private final int maxPlayers;
    private final GameSessionState gameSessionState; // Using the common enum
    private final GameLevel gameLevel;

    public GameLobbyInfoDTO(String sessionId, String gameName, int currentPlayerCount, int maxPlayers,
                            GameSessionState gameSessionState, GameLevel gameLevel) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.gameName = Objects.requireNonNull(gameName);
        this.currentPlayerCount = currentPlayerCount;
        this.maxPlayers = maxPlayers;
        this.gameSessionState = Objects.requireNonNull(gameSessionState);
        this.gameLevel = Objects.requireNonNull(gameLevel);
    }

    // --- Getters ---
    public String getSessionId() { return sessionId; }
    public String getGameName() { return gameName; }
    public int getCurrentPlayerCount() { return currentPlayerCount; }
    public int getMaxPlayers() { return maxPlayers; }
    public GameSessionState getGameSessionState() { return gameSessionState; }
    public GameLevel getGameLevel() { return gameLevel; }

    @Override
    public String toString() {
        return "GameLobbyInfoDTO{" +
                "sessionId='" + sessionId + '\'' +
                ", gameName='" + gameName + '\'' +
                ", players=" + currentPlayerCount + "/" + maxPlayers +
                ", state=" + gameSessionState +
                ", level=" + gameLevel +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameLobbyInfoDTO that = (GameLobbyInfoDTO) o;
        return sessionId.equals(that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }
}