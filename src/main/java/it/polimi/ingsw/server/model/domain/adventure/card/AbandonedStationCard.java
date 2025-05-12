package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.resource.GoodType;

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
    boolean isVisited;

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
        this.isVisited = false;

    }

    public int getMinCrewRequired() {
        return minCrewRequired;
    }
    public int getLostDays() { return lostDays; }
    public Map<GoodType,Integer> getGoodQuantities() {
        return this.goodQuantities;
    }
    public boolean isVisited() { return isVisited; }
    public void setVisited() { isVisited = true; }

    /**
     * Accepts a visitor to perform an operation on this adventure card.
     * The visitor pattern allows operations to be applied to the card without modifying its class.
     * @param visitor The visitor that will process this card.
     * @param state The current game state.
     * @return The result of the visitor's operation on this card.
     */
    public boolean accept(AdventureCardVisitor visitor, GameModel state){
        return visitor.visitAbandonedStationCard(this, state);
    }
}