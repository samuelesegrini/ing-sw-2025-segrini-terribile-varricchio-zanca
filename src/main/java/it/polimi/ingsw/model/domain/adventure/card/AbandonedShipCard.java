package it.polimi.ingsw.model.domain.adventure.card;

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
    public int getCrewLost(){
        return 0;
    }

    //
    public int getCreditsGained(){
        return 0;
    }

    //
    public int getLostDays(){
        return 0;
    }

    // accept visitor
    public <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}
