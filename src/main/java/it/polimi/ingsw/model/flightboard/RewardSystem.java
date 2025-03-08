package it.polimi.ingsw.model.flightboard;

import it.polimi.ingsw.model.ship.Ship;

import java.util.List;
import java.util.Map;

public class RewardSystem {
    private Map<Integer,Integer> positionRewards;
    private Map<Integer, List<GoodType>> resourceBonus;

    //defined by user constructor
    public RewardSystem(GameLevel level, int playerCount){}
    //the farther ahead you are at the end of the flight, the more cosmic credits you earn.
    public int getCreditsForPosition (Integer position){
        return 0;
    }

    //you can sell the goods you collected during the flight for cosmic credits
    public int getCreditsForResources(List<GoodType>) {
        return 0;
    }

    //the player with the fewest exposed connectors earns additional cosmic credits
    public int calculateBeautyBonus(Ship ship) {
        return 0;
    }

    //if you lost ship components during the flight, you lose 1 cosmic credit per lost piece
    public int penaltiesLostComponents(Ship ship) {
        return 0;
    }
}
