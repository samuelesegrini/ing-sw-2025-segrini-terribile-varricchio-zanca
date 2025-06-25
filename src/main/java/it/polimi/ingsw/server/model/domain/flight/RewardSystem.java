package it.polimi.ingsw.server.model.domain.flight;

import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.player.PlayerOrder;
import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RewardSystem {
    private final GameLevel level;
    private final Map<PlayerOrder,Integer> positionBonus;
    private final Map<GoodType, Integer> resourceBonus;
    private final int bestLookingShipBonus;
    private final int exposedConnectorsPenalty;

    public RewardSystem(GameLevel level, Map<PlayerOrder,Integer> positionBonus, Map<GoodType, Integer> resourceBonus, int bestLookingShipBonus, int exposedConnectorsPenalty) {
        this.level = level;
        this.positionBonus = positionBonus;
        this.resourceBonus = resourceBonus;
        this.bestLookingShipBonus = bestLookingShipBonus;
        this.exposedConnectorsPenalty = exposedConnectorsPenalty;
    }

    /**
     * Calculates the reward for a player based on their position in the finish order.
     * The position is determined by the index of the player in the finish order list.
     * @param finishOrder The list of players in the order they finished.
     * @param player The player for whom to calculate the reward.
     * @return The reward amount based on the player's position.
     */
    public int calculatePositionBonus (List<Player> finishOrder, Player player) {
        int position = finishOrder.indexOf(player);
        if(position < 0 || position >= positionBonus.size()){
            return 0; // Player not found or position out of bounds
        }
        return positionBonus.get(PlayerOrder.values()[position]);
    }

    /**
     * Calculates the bonus for resources on a ship based on the resource bonus map.
     * @param ship The ship to evaluate.
     * @return The total credits earned from resources on the ship.
     */
    public int calculateResourceBonus(Ship ship){
        Map<GoodType,Integer> resources = ship.getResources();
        int credits = 0;
        for(GoodType good : resources.keySet()){
            credits += (resources.get(good)) * (resourceBonus.get(good));
        }
        return credits;
    }

   /**
     * Calculates the bonus for the best looking ship based on the number of exposed connectors.
     * The ship with the least exposed connectors is considered the best looking.
     * @param players The list of players whose ships to evaluate.
     * @return A map containing the player with the best looking ship and the bonus amount.
     */
   public Map<Player,Integer> calculateBestLookingShipBonus(List<Player> players) {
       int exposedConnectors = Integer.MAX_VALUE;
       Player bestPlayer = null;
       for(Player player : players){
           Ship ship = player.getShip();
           if(ship.getExposedConnectors() < exposedConnectors){
               exposedConnectors = ship.getExposedConnectors();
               bestPlayer = player;
           }
       }
       return Map.of(bestPlayer, this.bestLookingShipBonus);
    }

    /**
     * Calculates the penalty for exposed connectors on a ship.
     * @param ship The ship to evaluate.
     * @return The penalty based on the number of exposed connectors.
     */
    public int calculateExposedConnectorsPenalty (Ship ship){
       return (ship.getExposedConnectors() * exposedConnectorsPenalty);
    }

    /**
     * Calculates the penalty for never used reserved components on a ship.
     * @param ship The ship to evaluate.
     * @return The penalty based on the number of reserved components.
     */
    public int calculateReservedComponentsPenalty(Ship ship) {
        return ship.getReservedComponents().size();
    }

    /**
     * Calculates the penalty for lost components on a ship.
     * @param ship The ship to evaluate.
     * @return The penalty based on the number of discarded components.
     */
    public int calculateLostComponentsPenalty(Ship ship) {
        return ship.getLostComponents().size();
    }

    public int calculateTotalReward(List<Player> finishOrder, Player player) {
        int totalReward = 0;
        totalReward += calculatePositionBonus(finishOrder, player);
        totalReward += calculateResourceBonus(player.getShip());
        totalReward += calculateBestLookingShipBonus(finishOrder).getOrDefault(player, 0);
        totalReward -= calculateExposedConnectorsPenalty(player.getShip());
        totalReward -= calculateLostComponentsPenalty(player.getShip());
        totalReward -= calculateReservedComponentsPenalty(player.getShip());
        return (totalReward);
    }

    public void calculateFinalScores (List<Player> finishOrder, GamePhase phase) {
        if (phase != GamePhase.END) {
            throw new IllegalArgumentException("Final scores can only be calculated at the end of the game.");
        }
        else{
            for (Player player : finishOrder) {
                int score = Math.max(0, calculateTotalReward(finishOrder, player) + player.getCredits() );
                player.setFinalScore(score);
            }
        }
    }
}


