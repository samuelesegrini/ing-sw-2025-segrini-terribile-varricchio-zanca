package it.polimi.ingsw.model.domain.adventure.card;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

public abstract class EnemyCard extends AdventureCard {

    private int powerLevel;
    private int movementPenalty;

    //constructor
    public EnemyCard(String id, CardLevel level, String description, AdventureType type, int powerLevel, int movementPenalty) {
        super(id, level, description, type);
        this.powerLevel = powerLevel;
        this.movementPenalty = movementPenalty;
    }

    public int getPowerLevel(){
        return 0;
    }
    public int getMovementPenalty(){
        return 0;
    }
    public boolean canSkipMovementPenalty(){
        return false;
    }

    public abstract <T> T accept(AdventureCardVisitor<T> visitor, GameState state);
}
