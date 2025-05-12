package it.polimi.ingsw.common.dto;

import java.io.Serializable;
import it.polimi.ingsw.server.model.enums.GameLevel;

/**
 * Data Transfer Object for game settings when creating a new game.
 */
public class GameSettingsDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String gameName;
    private final int maxPlayers;
    private final GameLevel gameLevel;
    
    /**
     * Creates a new game settings object.
     * 
     * @param gameName The name of the game
     * @param maxPlayers The maximum number of players (2-4)
     * @param gameLevel The game difficulty level (BEGINNER, STANDARD, ADVANCED)
     */
    public GameSettingsDTO(String gameName, int maxPlayers, GameLevel gameLevel) {
        this.gameName = gameName;
        this.maxPlayers = maxPlayers;
        this.gameLevel = gameLevel;
    }
    
    /**
     * Gets the game name.
     * 
     * @return The game name
     */
    public String getGameName() {
        return gameName;
    }
    
    /**
     * Gets the maximum number of players.
     * 
     * @return The maximum player count
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    /**
     * Gets the game difficulty level.
     * 
     * @return The game level
     */
    public GameLevel getGameLevel() {
        return gameLevel;
    }
    
    @Override
    public String toString() {
        return "GameSettingsDTO{" +
                "gameName='" + gameName + '\'' +
                ", maxPlayers=" + maxPlayers +
                ", gameLevel='" + gameLevel + '\'' +
                '}';
    }
}