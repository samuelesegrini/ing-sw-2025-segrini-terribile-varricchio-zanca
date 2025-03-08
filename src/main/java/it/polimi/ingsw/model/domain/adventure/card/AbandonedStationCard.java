package it.polimi.ingsw.model.domain.adventure.card;

import it.polimi.ingsw.model.gamestate.GameState;

public class AbandonedStationCard {

    private Map<GoodType, Integer> goodQuantities;
    private int minCrewRequired;
    private int lostDays;

    //constructor
    public AbandonedStationCard(int minCrewRequired, int lostDays, Map<GoodType, Integer> goodQuantities) {
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