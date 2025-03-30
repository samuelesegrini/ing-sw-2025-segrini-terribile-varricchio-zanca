package it.polimi.ingsw.model.domain.adventure.card;

import java.util.HashMap;
import java.util.Map;

import it.polimi.ingsw.model.domain.general.GameModel;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.domain.general.GameModel;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;
import it.polimi.ingsw.model.enums.resource.GoodType;

public class SmugglersCard extends EnemyCard {

    private Map<GoodType, Integer> availableGoods;
    private int goodsLostIfDefeated;

    /**
     * Constructs a new Smugglers encounter card with the specified attributes.
     *
     * @param id The unique identifier for the Smugglers card.
     * @param level The card's level.
     * @param description A brief description of the Smugglers' card effects.
     * @param powerLevel The power level of the Smugglers' attack.
     * @param movementPenalty The penalty to movement (flight days) if the player is defeated.
     * @param goodsLostIfDefeated The amount of goods the player will lose if they are defeated by the Smugglers.
     * @param availableGoods A map of the goods available to the player if they defeat the Smugglers.
     */
    public SmugglersCard(String id, CardLevel level, String description,
                        int powerLevel, int movementPenalty,
                        int goodsLostIfDefeated, Map<GoodType, Integer> availableGoods) {
        super(id, level, description, AdventureType.SMUGGLERS, powerLevel, movementPenalty);
        this.goodsLostIfDefeated = goodsLostIfDefeated;
        this.availableGoods = new HashMap<>(availableGoods);
    }

    public int getGoodsLostIfDefeated() {
        return this.goodsLostIfDefeated;
    }

    public Map<GoodType, Integer> getAvailableGoods() {
        return this.availableGoods;
    }

    /**
     * Accepts a visitor that performs some action on the Smugglers card.
     *
     * @param visitor The visitor handling the card logic.
     * @param state The current game state, which may affect the behavior of the visitor.
     * @return The result of the visitor's action on the Smugglers card.
     */
    public boolean accept(AdventureCardVisitor visitor, GameModel state){
        return visitor.visitSmugglersCard(this, state);
    }
}
