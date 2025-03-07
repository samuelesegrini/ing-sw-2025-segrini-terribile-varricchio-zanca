package it.polimi.ingsw.model.adventure;

public class SlaversCard {
    private int creditReward;
    private int crewLossAmount;

    //constructor
    public SlaversCard(int creditReward, int crewLossAmount) {
        this.creditReward = creditReward;
        this.crewLossAmount = crewLossAmount;
    }

    //how many credits the players get in case of victory
    public int getCreditReward() {}

    //how many crew memebers the players lose if defeated
    public int getCrewLossAmount(){}

    // accept visitor
    public <T> T accept(AdventureCardVisitor<T> visitor, GameState state){}
}
