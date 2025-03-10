package it.polimi.ingsw.model.domain.adventure.card;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;
import it.polimi.ingsw.model.enums.resource.GoodType;

import java.util.Map;

/**
 * Represents an adventure card that allows a player to land on an abandoned station
 * to load valuable goods. The card requires the player to have a minimum number of crew members
 * to access the opportunity. Only one player can use this opportunity, and it is resolved in
 * turn order, starting with the leader.
 * The player must then load goods, which can be rearranged or discarded,
 * and lose the specified number of flight days.
 */
public class AbandonedStationCard extends AdventureCard {

    private Map<GoodType, Integer> goodQuantities;
    private int minCrewRequired;
    private int lostDays;

    /**
     * Constructs an AbandonedStationCard with the given details.
     * @param id The unique identifier for the card.
     * @param level The level of the card.
     * @param description A description of the effect of the abandoned station card.
     * @param minCrewRequired The minimum number of crew members required to dock at the abandoned station.
     * @param lostDays The number of flight days the player loses by docking at the station.
     * @param goodQuantities A map of goods available at the station, where the key is the type of good
     *                       and the value is the quantity of that good available.
     */
    public AbandonedStationCard(String id, CardLevel level, String description,int minCrewRequired, int lostDays, Map<GoodType, Integer> goodQuantities) {
        super(id, level, description, AdventureType.ABANDONED_STATION);
        this.minCrewRequired = minCrewRequired;
        this.lostDays = lostDays;
        this.goodQuantities = goodQuantities;
    }

    /**
     * Returns a map of all the goods available on the abandoned station.
     * The map's key is the type of good, and the value is the quantity of that good available.
     * @return A map of goods and their quantities.
     */
    public Map<GoodType, Integer> getGoodQuantities(){
        return null;
    }

    /**
     * Returns the quantity of a specific type of good available on the abandoned station.
     * @param type The type of good to check for quantity.
     * @return The quantity of the specified good type available.
     */
    public int getQuantityByType(GoodType type){
        return 0;
    }

    /**
     * Returns the minimum number of crew members required to land on the abandoned station.
     * @return The minimum crew required.
     */
    public int getMinCrewRequired(){
        return 0;
    }

    /**
     * Returns the number of flight days lost by landing on the abandoned station.
     * @return The number of flight days lost.
     */
    public int getLostDays(){
        return 0;
    }

    /**
     * Returns the sum of all goods available, regardless of type.
     * @return The total number of goods available on the station.
     */
    public int getTotalGoodsQuantity(){
        return 0;
    }

    /**
     * Accepts a visitor to perform an operation on this adventure card.
     * The visitor pattern allows operations to be applied to the card without modifying its class.
     * @param <T> The type of result returned by the visitor.
     * @param visitor The visitor that will process this card.
     * @param state The current game state.
     * @return The result of the visitor's operation on this card.
     */
    public <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}