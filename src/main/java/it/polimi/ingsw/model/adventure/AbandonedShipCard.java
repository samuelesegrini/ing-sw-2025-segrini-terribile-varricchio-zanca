package it.polimi.ingsw.model.adventure;

public class AbandonedShipCard {

    private int crewLost;
    private int creditsGained;
    private int lostDays;

    //constructor
    public AbandonedShipCard(int crewLost, int creditsGained, int lostDays) {
        this.crewLost = crewLost;
        this.creditsGained = creditsGained;
        this.lostDays = lostDays;
    }

    //
    public int getCrewLost(){}

    //
    public int getCreditsGained(){}

    //
    public int getLostDays(){}

    // accept visitor
    public <T> T accept(AdventureCardVisitor<T> visitor, GameState state){}
}
