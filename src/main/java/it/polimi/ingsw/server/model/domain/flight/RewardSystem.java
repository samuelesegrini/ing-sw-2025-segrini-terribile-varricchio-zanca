package it.polimi.ingsw.server.model.domain.flight;

import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.player.PlayerOrder;
import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            credits += resources.get(good)*resourceBonus.get(good);
        }
        return credits;
    }

   /**
     * Calculates the bonus for the best looking ship based on the number of exposed connectors.
     * The ship with the least exposed connectors is considered the best looking.
     * @param ships The list of ships to evaluate.
     * @return A map containing the player with the best looking ship and the bonus amount.
     */
   public Map<Player,Integer> calculateBestLookingShipBonus(List<Ship> ships) {
       int exposedConnectors = Integer.MAX_VALUE;
       Player player = null;
       for(Ship ship : ships){
           if(ship.getExposedConnectors() < exposedConnectors){
               exposedConnectors = ship.getExposedConnectors();
               player = ship.getPlayer();
           }
       }
       return Map.of(player, this.bestLookingShipBonus);
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
     * Gets the position bonus for a specific finish position.
     * @param finishPosition The position (0 = first place, 1 = second place, etc.)
     * @return The bonus points for that position
     */
    public int getPositionBonus(int finishPosition) {
        if (finishPosition < 0 || finishPosition >= positionBonus.size()) {
            return 0; // Position out of bounds
        }
        
        PlayerOrder[] orders = PlayerOrder.values();
        if (finishPosition < orders.length) {
            return positionBonus.getOrDefault(orders[finishPosition], 0);
        }
        
        return 0;
    }
}

