package it.polimi.ingsw.model.domain.adventure.card;

import java.util.HashMap;
import java.util.Map;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;
import it.polimi.ingsw.model.enums.resource.GoodType;

public class SmugglersCard extends EnemyCard {

    private Map<GoodType, Integer> availableGoods;
    private int goodsLostIfDefeated;
    private boolean isDefeated;

    //constructor
    public SmugglersCard(String id, CardLevel level, String description,
                        int powerLevel, int movementPenalty,
                        int goodsLostIfDefeated, Map<GoodType, Integer> availableGoods) {
        super(id, level, description, AdventureType.SMUGGLERS, powerLevel, movementPenalty);
        this.goodsLostIfDefeated = goodsLostIfDefeated;
        this.availableGoods = new HashMap<>(availableGoods);
        this.isDefeated = false;
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
