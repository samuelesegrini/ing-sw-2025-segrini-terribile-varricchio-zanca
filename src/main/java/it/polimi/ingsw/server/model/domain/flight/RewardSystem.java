package it.polimi.ingsw.server.model.domain.flight;

import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.HashMap;
import java.util.Map;

public class RewardSystem {
    private Map<Integer,Integer> positionRewards;
    private Map<GoodType, Integer> resourceBonus;
    private int bestLookingShipBonus;

    public RewardSystem(GameLevel level){

        positionRewards = new HashMap<>();
        switch(level){
            case TEST_FLIGHT:
                positionRewards.put(0,4);
                positionRewards.put(1,3);
                positionRewards.put(2,2);
                positionRewards.put(3,1);
                bestLookingShipBonus=2;
            case LEVEL_II:
                positionRewards.put(0,8);
                positionRewards.put(1,6);
                positionRewards.put(2,4);
                positionRewards.put(3,2);
                bestLookingShipBonus=4;
        }

        resourceBonus = new HashMap<>();
        resourceBonus.put(GoodType.RED, 4);
        resourceBonus.put(GoodType.YELLOW, 3);
        resourceBonus.put(GoodType.GREEN, 2);
        resourceBonus.put(GoodType.BLUE, 1);
    }

    /**
     * Grants extra credits based on the final position. The farther ahead a player is
     * at the end of the flight, the more cosmic credits he earns.
     * @param position position at the end of the flight
     * @return number of extra credits earned
     */
    public int getCreditsForPosition (int position){
        return positionRewards.get(position);
    }

    /**
     * Calculates the credits earned by selling the resource collected during the flight.
     * @param good type of resource to sell.
     * @return total credits earned by the player
     */
    public int getCreditsForResource(GoodType good){
        return resourceBonus.get(good);
    }

    public int getCreditsForAllResources(Ship ship){
        Map<GoodType,Integer> resources = ship.getResources();
        int credits = 0;
        for(GoodType good : resources.keySet()){
            credits += resources.get(good)*resourceBonus.get(good);
        }
        return credits;
    }

    //serve in Ship metodo getExposedComponents() che calcoli il numero di componenti esposti
    //serve in Ship un riferimento al giocatore
    //public Map<Player,Integer> calculateBeautyBonus (List<Ship> ships){
    //    int exposedComponents = Integer.MAX_VALUE;
    //    Player player = null;
    //    for(Ship ship : ships){
    //        if(ship.getExposedComponents() < exposedComponents){
    //            exposedComponents = ship.getExposedComponents();
    //            player = ship.getPlayer();
    //        }
    //    }
    //    return Map.of(player, bestLookingShipBonus);
    //}

    /**
     * Calculates credits lost as a penalty for ship components lost during the flight,
     * subtracting one credit per lost piece.
     * @param ship player's ship
     * @return number of lost credits
     */
    //public int penaltiesLostComponents (Ship ship){
    //    return ship.getExposedComponents();
    //}
}

