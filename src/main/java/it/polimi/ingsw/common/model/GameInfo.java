package it.polimi.ingsw.common.model;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;

import java.io.Serializable;

/**
 * Lightweight game information for lobby display purposes.
 * Contains only essential data needed for game list and lobby UI,
 * without the full GameModel complexity.
 */
public class GameInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String gameId;
    private final String gameName;
    private final GameLevel gameLevel;
    private final int currentPlayerCount;
    private final int maxPlayers;
    private final GamePhase currentPhase;
    private final boolean isStarted;
    private final boolean isJoinable;

    public GameInfo(String gameId, String gameName, GameLevel gameLevel,
                    int currentPlayerCount, int maxPlayers, GamePhase currentPhase,
                    boolean isStarted, boolean isJoinable) {
        this.gameId = gameId;
        this.gameName = gameName;
        this.gameLevel = gameLevel;
        this.currentPlayerCount = currentPlayerCount;
        this.maxPlayers = maxPlayers;
        this.currentPhase = currentPhase;
        this.isStarted = isStarted;
        this.isJoinable = isJoinable;
    }

    public String getGameId() {
        return gameId;
    }

    public String getGameName() {
        return gameName;
    }

    public GameLevel getGameLevel() {
        return gameLevel;
    }

    public int getCurrentPlayerCount() {
        return currentPlayerCount;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public boolean isJoinable() {
        return isJoinable;
    }

    /**
     * Creates a GameInfo from a GameModel for lobby display purposes.
     */
    public static GameInfo fromGameModel(GameModel gameModel) {
        if (gameModel == null) {
            return null;
        }

        boolean isStarted = gameModel.getCurrentPhase() != GamePhase.SETUP;
        boolean isJoinable = !isStarted && gameModel.getPlayers().size() < gameModel.getMaxPlayers();

        return new GameInfo(
                gameModel.getGameId(),
                gameModel.getGameName(),
                gameModel.getGameLevel(),
                gameModel.getPlayers().size(),
                gameModel.getMaxPlayers(),
                gameModel.getCurrentPhase(),
                isStarted,
                isJoinable
        );
    }

    @Override
    public String toString() {
        return String.format("GameInfo{id='%s', name='%s', level=%s, players=%d/%d, phase=%s, started=%s, joinable=%s}",
                gameId, gameName, gameLevel, currentPlayerCount, maxPlayers, currentPhase, isStarted, isJoinable);
    }
}