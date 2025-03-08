package it.polimi.ingsw.model.domain.adventure.card;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.domain.adventure.entity.CannonFire;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

public class PiratesCard extends EnemyCard {

    private int creditReward;
    private List<CannonFire> attackPattern;

    //constructor
    public PiratesCard(String id, CardLevel level, String description, int powerLevel, int movementPenalty, int creditReward, List<CannonFire> attackPattern) {
        super(id, level, description, AdventureType.PIRATES, powerLevel, movementPenalty);
        this.creditReward = creditReward;
        this.attackPattern = new ArrayList<>(attackPattern);
    }

    //get how many credits the players get in case of victory
    public int getCreditReward() {
        return 0;
    }

    //get how many cannon fires the player will be attacked by
    public int getCannonFireCount(){
        return 0;
    }

    //get a list of the cannon fires attacking the player including direction and intensity
    public List<CannonFire> getAttackPattern(){
        return List.of();
    }

    // accept visitor
    public <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}
