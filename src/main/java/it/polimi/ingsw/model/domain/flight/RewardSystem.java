package it.polimi.ingsw.model.domain.flight;

import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.model.enums.resource.GoodType;
import it.polimi.ingsw.model.domain.ship.Ship;

import java.util.List;
import java.util.Map;

public class RewardSystem {
    private Map<Integer,Integer> positionRewards;
    private Map<Integer, List<GoodType>> resourceBonus;

    /**
     * Constructor that initializes the game level and the number of players.
     * @param level game's level
     * @param playerCount number of players
     */
    public RewardSystem(GameLevel level, int playerCount){}

    /**
     * Grants extra credits based on the final position. The farther ahead a player is
     * at the end of the flight, the more cosmic credits he earns.
     * @param position position at the end of the flight
     * @return number of extra credits earned
     */
    public int getCreditsForPosition (int position){ return position; }

    /**
     * Calculates the total credits earned by selling the resources collected during the flight.
     * @param goods types of resources to sell.
     * @return total credits earned by the player
     */
    public int getCreditsForResources(List<GoodType> goods){ return 0; }

    /**
     * Calculates extra credits for players with the fewest exposed connectors.
     * @param ship player's ship
     * @return extra credits earned
     */
    public int calculateBeautyBonus (Ship ship){ return 0; }

    /**
     * Calculates credits lost as a penalty for ship components lost during the flight,
     * subtracting one credit per lost piece.
     * @param ship player's ship
     * @return number of lost credits
     */
    public int penaltiesLostComponents (Ship ship){ return 0; }
}

