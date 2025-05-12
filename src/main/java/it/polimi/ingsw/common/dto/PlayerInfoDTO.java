package it.polimi.ingsw.common.dto;

import java.io.Serializable;
import java.util.Objects;

public class PlayerInfoDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String playerId;
    private final String nickname;
    private boolean isReady; // For lobby readiness
    private boolean isHost; // Whether this player is the host (creator) of the game

    public PlayerInfoDTO(String playerId, String nickname) {
        this(playerId, nickname, false, false);
    }

    public PlayerInfoDTO(String playerId, String nickname, boolean isReady) {
        this(playerId, nickname, isReady, false);
    }
    
    public PlayerInfoDTO(String playerId, String nickname, boolean isReady, boolean isHost) {
        this.playerId = Objects.requireNonNull(playerId);
        this.nickname = Objects.requireNonNull(nickname);
        this.isReady = isReady;
        this.isHost = isHost;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) { // DTOs are often mutable for convenience, or create new one
        isReady = ready;
    }
    
    public boolean isHost() {
        return isHost;
    }
    
    public void setHost(boolean host) {
        isHost = host;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerInfoDTO that = (PlayerInfoDTO) o;
        return playerId.equals(that.playerId); // PlayerId should be unique
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

    @Override
    public String toString() {
        return "PlayerInfoDTO{" +
                "playerId='" + playerId + '\'' +
                ", nickname='" + nickname + '\'' +
                ", isReady=" + isReady +
                ", isHost=" + isHost +
                '}';
    }
}