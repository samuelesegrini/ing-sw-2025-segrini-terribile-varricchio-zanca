package it.polimi.ingsw.server.model.domain.adventure.entity;

import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.Map;
/**
 * Represents a planet in the game. A planet can be visited by a player during a Planets event.
 * Each planet has a list of goods available for collection, and players can land on the planet to pick them up.
 * The planet tracks whether it has been visited and allows querying the quantities of specific goods.
 */
public class Planet {
    private int number;
    private Map<GoodType, Integer> goodQuantities;
    private boolean visited;

    /**
     * Constructs a new Planet with the specified quantities of goods.
     *
     * @param goodQuantities A map of {@link GoodType} to their corresponding quantities available on the planet.
     */
    public Planet(int number, Map<GoodType, Integer> goodQuantities) {
        this.number = number;
        this.goodQuantities = goodQuantities;
        visited = false;
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
}
