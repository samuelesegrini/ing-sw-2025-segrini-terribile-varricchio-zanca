package it.polimi.ingsw.common;

import java.io.Serializable; /**
 * Information about a player in a game.
 */
public class PlayerInfo implements Serializable {
    public final String playerId;
    public final String nickname;
    public final boolean isReady;

    public PlayerInfo(String playerId, String nickname, boolean isReady) {
        this.playerId = playerId;
        this.nickname = nickname;
        this.isReady = isReady;
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
}