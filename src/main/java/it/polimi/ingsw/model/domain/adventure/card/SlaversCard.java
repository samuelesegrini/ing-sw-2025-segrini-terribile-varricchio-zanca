package it.polimi.ingsw.model.domain.adventure.card;

public class SlaversCard {
    private int creditReward;
    private int crewLossAmount;

    //constructor
    public SlaversCard(int creditReward, int crewLossAmount) {
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
