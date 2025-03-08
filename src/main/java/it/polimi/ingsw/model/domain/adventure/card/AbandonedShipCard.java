package it.polimi.ingsw.model.domain.adventure.card;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

public class AbandonedShipCard extends AdventureCard {
    private int crewLost;
    private int creditsGained;
    private int lostDays;

    //constructor
    public AbandonedShipCard(String id, CardLevel level, String description, int crewLost, int creditsGained, int lostDays) {
        super(id, level, description, AdventureType.ABANDONED_SHIP);
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
