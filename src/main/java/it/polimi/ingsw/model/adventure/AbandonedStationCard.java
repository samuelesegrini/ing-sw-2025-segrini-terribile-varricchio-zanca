package it.polimi.ingsw.model.adventure;

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

    public Map<GoodType, Integer> getGoodQuantities(){}
    public int getQuantityByType(GoodType type){}
    public int getMinCrewRequired()
    public int getLostDays(){}
    public int getTotalGoodsQuantity(){}
    public <T> T accept(AdventureCardVisitor<T> visitor, GameState state){}
}
