package it.polimi.ingsw.model.domain.adventure.card;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

public class SlaversCard extends EnemyCard {
    private int creditReward;
    private int crewLossAmount;

    //constructor
    public SlaversCard(String id, CardLevel level, String description,
                     int powerLevel, int movementPenalty,
                     int creditReward, int crewLossAmount) {
        super(id, level, description, AdventureType.SLAVERS, powerLevel, movementPenalty);
        this.creditReward = creditReward;
        this.crewLossAmount = crewLossAmount;
    }

    //how many credits the players get in case of victory
    public int getCreditReward(){
        return 0;
    }

    //how many crew memebers the players lose if defeated
    public int getCrewLossAmount(){
        return 0;
    }

    // accept visitor
    public <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}
