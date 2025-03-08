package it.polimi.ingsw.model.flightboard;

import it.polimi.ingsw.model.player.PlayerFlightData;

import java.util.List;
import java.util.Map;

public class Route {
    private int length;
    private List<Integer> startingPositions;
    private List<Integer> availableStartingPositions;
    private RewardSystem rewardSystem;

    //constructor defined by user
    public Route(GameLevel level, int length){
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public List<Integer> getAllStartingPositions() {
        return startingPositions;
    }

    public List<Integer> getAvailableStartingPositions() {
        return availableStartingPositions;
    }

    public int assignStartingPosition() {
        return 0;
    }

    //normalize position based on the number of laps completed by the player
    public int normalizePosition(int position) {
        return 0;
    }

    public boolean isPositionOccupied (int position, Map<String, PlayerFlightData> playerData){
        return false;
    };

}

