package it.polimi.ingsw.model.adventure;

import java.util.List;

public class PiratesCard {

    private int creditReward;
    private List<CannonFire> attackPattern;

    //constructor
    public PiratesCard(int creditReward, List<CannonFire> attackPattern) {
        this.creditReward = creditReward;
        this.attackPattern = attackPattern;
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
