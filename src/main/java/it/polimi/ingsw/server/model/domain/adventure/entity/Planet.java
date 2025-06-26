package it.polimi.ingsw.server.model.domain.adventure.entity;

import it.polimi.ingsw.server.model.enums.resource.GoodType;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.io.Serializable;
import java.util.Map;
/**
 * Represents a planet in the game. A planet can be visited by a player during a Planets event.
 * Each planet has a list of goods available for collection, and players can land on the planet to pick them up.
 * The planet tracks whether it has been visited and allows querying the quantities of specific goods.
 */
public class Planet implements Serializable {
    private final static long serialVersionUID = 1L;

    private int number;
    private Map<GoodType, Integer> goodQuantities;
    private boolean visited;
    private PlayerId claimedBy;

    /**
     * Constructs a new Planet with the specified quantities of goods.
     *
     * @param goodQuantities A map of {@link GoodType} to their corresponding quantities available on the planet.
     */
    public Planet(int number, Map<GoodType, Integer> goodQuantities) {
        this.number = number;
        this.goodQuantities = goodQuantities;
        visited = false;
        claimedBy = null;
    }
    /**
     * Gets the unique number of the planet.
     *
     * @return The number of the planet.
     */
    public int getNumber() {
        return number;
    }

    /**
     * Gets the map of goods available on the planet, with their respective quantities.
     *
     * @return A map where the keys are {@link GoodType} objects and the values are the quantities of each good.
     */
    public Map<GoodType, Integer> getGoodQuantities(){
        return goodQuantities;
    }

    /**
     * Gets the quantity of goods of a specific type available on this planet.
     *
     * @param type The type of good.
     * @return The quantity of the specified good type, or 0 if the good type is not available on the planet.
     */
    public int getQuantityByType(GoodType type){
        return goodQuantities.get(type);
    }

    /**
     * Gets the sum of the quantities of each type of good on the planet.
     *
     * @return The total quantity of all goods available on the planet.
     */
    public int getTotalGoodsQuantity(){
        int total = 0;
        for (Map.Entry<GoodType, Integer> entry : goodQuantities.entrySet()){
            total += entry.getValue();
        }
        return total;
    }

    /**
     * Returns whether the planet has already been visited by a player.
     * A planet is considered visited once a player has landed on it and taken the goods.
     *
     * @return {@code true} if the planet has been visited, {@code false} otherwise.
     */
    public boolean isVisited(){
        return visited;
    }

    public void setVisited(){
        visited = true;
    }
    
    /**
     * Claims this planet for a specific player.
     * @param playerId The player who claims this planet
     */
    public void claimPlanet(PlayerId playerId) {
        this.claimedBy = playerId;
        this.visited = true;
    }
    
    /**
     * Gets the player who claimed this planet.
     * @return The PlayerId of the player who claimed this planet, or null if unclaimed
     */
    public PlayerId getClaimedBy() {
        return claimedBy;
    }
    
    /**
     * Checks if this planet has been claimed by a specific player.
     * @param playerId The player to check
     * @return true if this player claimed the planet
     */
    public boolean isClaimedBy(PlayerId playerId) {
        return claimedBy != null && claimedBy.equals(playerId);
    }
    
    /**
     * Calculates the total value of goods on this planet for goods shortage priority.
     * RED=4, YELLOW=3, GREEN=2, BLUE=1 credits per good.
     * @return The total credit value of all goods on this planet
     */
    public int calculateTotalValue() {
        int totalValue = 0;
        totalValue += goodQuantities.getOrDefault(GoodType.RED, 0) * 4;
        totalValue += goodQuantities.getOrDefault(GoodType.YELLOW, 0) * 3;
        totalValue += goodQuantities.getOrDefault(GoodType.GREEN, 0) * 2;
        totalValue += goodQuantities.getOrDefault(GoodType.BLUE, 0) * 1;
        return totalValue;
    }
    
    /**
     * Checks if this planet requires special cargo holds (contains RED goods).
     * @return true if this planet has RED goods requiring special cargo
     */
    public boolean requiresSpecialCargo() {
        return goodQuantities.getOrDefault(GoodType.RED, 0) > 0;
    }
}
