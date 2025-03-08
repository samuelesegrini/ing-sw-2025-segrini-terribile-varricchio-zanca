package it.polimi.ingsw.model.adventure;

public class SmugglersCard {
    private Map<GoodType, Integer> availableGoods;
    private int goodsLostIfDefeated;
    private boolean isDefeated;

    //constructor
    public SmugglersCard(int goodsLostIfDefeated, Map<GoodType, Integer> availableGoods) {
        this.goodsLostIfDefeated = goodsLostIfDefeated;
        this.availableGoods = availableGoods;
        isDefeated = false;
    }
    //have the smugglers been defeated
    public boolean isDefeated() {
        return isDefeated;
    }

    // which goods and in what quantity are available in case of victory
    public Map<GoodType, Integer> getAvailableGoods(){
        return null;
    }

    //how many goods the players lose if they lose
    public int getGoodsLostIfDefeated(){
        return 0;
    }

    // accept visitor
    public <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}
