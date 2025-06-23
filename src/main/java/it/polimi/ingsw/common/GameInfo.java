package it.polimi.ingsw.common;

import it.polimi.ingsw.server.model.enums.GameLevel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Information about a game.
 */
public class GameInfo implements Serializable {
    public final String gameId;
    public final String gameName;
    public final int maxPlayers;
    public final int currentPlayers;
    public final GameLevel gameLevel;
    public final List<PlayerInfo> players;

    public GameInfo(String gameId, String gameName, int maxPlayers, int currentPlayers,
                    GameLevel gameLevel, List<PlayerInfo> players) {
        this.gameId = gameId;
        this.gameName = gameName;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = currentPlayers;
        this.gameLevel = gameLevel;
        this.players = new ArrayList<>(players);
    }

    public String getGameId() {
        return  gameId;
    }

    public String getGameName() {
        return gameName;
    }

    public String getCreatorId() {
        if (players != null && !players.isEmpty()) {
            return players.get(0).getPlayerId();
        }
        return null;
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }

    public GameLevel getGameLevel() {
        return gameLevel;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
}

