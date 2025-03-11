package it.polimi.ingsw.model.domain.flight;

import it.polimi.ingsw.model.domain.flight.PlayerFlightData;
import it.polimi.ingsw.model.enums.GameLevel;

import java.util.List;
import java.util.Map;

public class Route {
    private int length;
    private List<Integer> startingPositions;
    private List<Integer> availableStartingPositions;
    private RewardSystem rewardSystem;

    /**
     * Constructor that initializes the Route based on the game level.
     * @param level game level
     */
    public Route(GameLevel level, int length, List<Integer> startingPositions, RewardSystem rewardSystem) {
        this.length = length;
        this.startingPositions = startingPositions;
        this.availableStartingPositions = startingPositions;
        this.rewardSystem = rewardSystem;
    }

    /**
     * Returns the length of the route.
     * @return length of the route
     */
    public int getLength() { return length; }

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
    public int assignStartingPosition(){
        Integer startingPosition = availableStartingPositions.get(0);
        availableStartingPositions.remove(startingPosition);
        return startingPosition;
    }

    /**
     * Normalize the position based on the number of laps completed by the player.
     * @param position player's position
     * @return player's normalized position
     */
    public int normalizePosition (int position){ return position; }

    /**
     * Indicates if the position is occupied by another player.
     * @param position player's position
     * @param playerData map that associates each player,
     * identified by the username string, with their flight data.
     * @return {@code true} if the position is already occupied by another player, {@code false} otherwise.
     */
    public boolean isPositionOccupied (int position, Map<String, PlayerFlightData> playerData){ return false; }
}

