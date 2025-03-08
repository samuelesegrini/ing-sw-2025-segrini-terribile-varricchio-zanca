package it.polimi.ingsw.model.domain.adventure.card;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;
import it.polimi.ingsw.model.enums.resource.GoodType;

import java.util.Map;

public class AbandonedStationCard extends AdventureCard {

    private Map<GoodType, Integer> goodQuantities;
    private int minCrewRequired;
    private int lostDays;

    //constructor
    public AbandonedStationCard(String id, CardLevel level, String description,int minCrewRequired, int lostDays, Map<GoodType, Integer> goodQuantities) {
        super(id, level, description, AdventureType.ABANDONED_STATION);
        this.minCrewRequired = minCrewRequired;
        this.lostDays = lostDays;
        this.goodQuantities = goodQuantities;
    }

    public Map<GoodType, Integer> getGoodQuantities(){
        return null;
    }
    public int getQuantityByType(GoodType type){
        return 0;
    }
    public int getMinCrewRequired(){
        return 0;
    }
    public int getLostDays(){
        return 0;
    }
    public int getTotalGoodsQuantity(){
        return 0;
    }
    public <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}