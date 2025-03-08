package it.polimi.ingsw.model.flightboard;

import java.util.List;
import java.util.Map;

public class Route {
    private int lenght;
    private List<Integer> startingPositions;
    private List<Integer> availableStartingPositions;
    private RewardSystem rewardSystem;

    /**
     * Constructor that initializes the Route based on the game level.
     * @param level game level
     */
    public void Route(GameLevel level){};

    /**
     * Returns the lenght of the route.
     * @return lenght of the route
     */
    public int getLenght() {
        return lenght;
    }

    /**
     * Provides a list of all players' starting positions from first to last.
     * @return players' starting positions
     */
    public List<Integer> getAllStartingPositions() {
        return startingPositions;
    }

    /**
     * Provides a list of available starting positions.
     * @return available starting positions
     */
    public List<Integer> getAvailableStartingPositions() {
        return availableStartingPositions;
    }

    /**
     * Assigns the player their starting position on the Route.
     * @return player's position
     */
    public int assignStartingPosition(){};

    /**
     * Normalize the position based on the number of laps completed by the player.
     * @param position player's position
     * @return player's normalized position
     */
    public int normalizePosition (int position){};

    /**
     * Indicates if the position is occupied by another player.
     * @param position player's position
     * @param playerData map that associates each player,
     * identified by the username string, with their flight data.
     * @return true if the position is already occupied by another player, otherwise returns false.
     */
    public boolean isPositionOccupied (int position, Map<String, PlayerFlightData> playerData){};

}

