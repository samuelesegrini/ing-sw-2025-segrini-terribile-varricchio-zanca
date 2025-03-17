package it.polimi.ingsw.model.domain.flight;

import it.polimi.ingsw.model.domain.flight.PlayerFlightData;
import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.model.domain.flight.Route;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FlightBoard {
    private Route route;
    private Map <Player, PlayerFlightData> playerDataMap;
    private List <Player> finishOrder;

    /**
     * Constructs a new FlightBoard for the given game level and player count.
     * Initializes the route based on the level and player count.
     * @param level The game level.
     * @param playerCount The number of players participating in the game.
     */
    public FlightBoard(GameLevel level, Integer playerCount){}

    /**
     * Moves the player by a specified number of spaces along the flight route.
     * @param player The player to move.
     * @param spaces The number of spaces to move the player forward.
     */
    public void movePlayer(Player player, Integer spaces, boolean forward){}

    /**
     * Registers a new player on the flight board, initializing PlayerFlightData
     * and adding a record to PlayerDataMap.
     * @param player The ID of the player to register.
     */
    public void registerPlayer(Player player){}

    //thanks to the attribute playerDataMap i can get the position of the player on the flight route given the PlayerId
    /**
     * Returns the current position of the player on the flight route.
     * @param player The player whose position is being queried.
     * @return The position of the player on the flight route.
     */
    public int getPlayerPosition(Player player){
        return 0;
    }

    /**
     * Returns the flight data of the specified player, including their current flight state, completed laps, and position.
     * @param player The player whose flight data is being requested.
     * @return The flight data of the player.
     */
    public PlayerFlightData getPlayerData(Player player){
        return null;
    }

    /**
     * Returns a list of player names ordered from the first to the last based on their current position
     * on the flight route. The list is sorted such that the player furthest along the route comes first,
     * and the player furthest behind comes last.
     * @return A list of player names.
     */
    public List<Player> getPlayerOrderByPosition(){
        return List.of();
    }

    /**
     * Abandons a player from the flight, setting their flight status to ABANDONED.
     * @param player The ID of the player to abandon.
     */
    public void abandonPlayer(Player player){}

    /**
     * Checks if the flight is complete, meaning all players have either finished the flight or abandoned the race.
     * @return {@code true} if the flight is complete, {@code false} otherwise.
     */
    public boolean isFlightComplete(){
        return false;
    }
}





