package it.polimi.ingsw.common;

import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Information about a game.
 */
public class GameInfo implements Serializable {
    public final String gameId;
    public final String gameName;
    public final String creatorId;
    public final int maxPlayers;
    public final int currentPlayers;
    public final GameLevel gameLevel;
    public final GamePhase currentPhase;
    public final List<PlayerInfo> players;

    public GameInfo(String gameId, String gameName, String creatorId, int maxPlayers, int currentPlayers,
                    GameLevel gameLevel, GamePhase currentPhase, List<PlayerInfo> players) {
        this.gameId = gameId;
        this.gameName = gameName;
        this.creatorId = creatorId;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = currentPlayers;
        this.gameLevel = gameLevel;
        this.currentPhase = currentPhase;
        this.players = new ArrayList<>(players);
    }

    public String getGameId() {
        return  gameId;
    }

    public String getGameName() {
        return gameName;
    }

    public String getCreatorId() {
        return creatorId;
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

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }
}

