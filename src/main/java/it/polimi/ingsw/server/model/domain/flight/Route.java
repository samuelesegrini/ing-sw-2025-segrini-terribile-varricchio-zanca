package it.polimi.ingsw.server.model.domain.flight;

import it.polimi.ingsw.server.model.enums.GameLevel;

import java.io.Serializable;
import java.util.List;

public class Route implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int length;
    private final List<Integer> startingPositions;
    private List<Integer> availableStartingPositions;
    private final RewardSystem rewardSystem;

    /**
     * Constructs a new Route for the given game level, length, starting positions, and reward system.
     * @param level The game level.
     * @param length The length of the route.
     * @param startingPositions The list of players' starting positions.
     * @param rewardSystem The reward system associated with the route.
     */
    public Route(GameLevel level, int length, List<Integer> startingPositions, RewardSystem rewardSystem ) {
        this.length = length;
        this.startingPositions = startingPositions;
        this.availableStartingPositions = startingPositions;
        this.rewardSystem = rewardSystem;
    }

    public int getLength() { return length; }

    public List<Integer> getStartingPositions() {
        return startingPositions;
    }

    public List<Integer> getAllAvailableStartingPositions() {
        return availableStartingPositions;
    }
    public RewardSystem getRewardSystem() {
        return rewardSystem;
    }
    public int getFirstAvailableStartingPosition(){
        Integer startingPosition = availableStartingPositions.getFirst();
        availableStartingPositions.remove(startingPosition);
        return startingPosition;
    }
}

