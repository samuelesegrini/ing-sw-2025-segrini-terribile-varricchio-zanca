package it.polimi.ingsw.model.flightboard;

import java.util.List;
import java.util.Map;

public class Route {
    private int lenght;
    private List<Integer> startingPositions;
    private List<Integer> availableStartingPositions;
    private RewardSystem rewardSystem;

    //constructor defined by user
    public void Route(GameLevel level){};

    public int getLenght() {
        return lenght;
    }

    public List<Integer> getAllStartingPositions() {
        return startingPositions;
    }

    public List<Integer> getAvailableStartingPositions() {
        return availableStartingPositions;
    }

    public int assignStartingPosition(){};
    //normalize position based on the number of laps completed by the player
    public int normalizePosition (int position){};
    public boolean isPositionOccupied (int position, Map<String, PlayerFlightData> playerData){};

}

