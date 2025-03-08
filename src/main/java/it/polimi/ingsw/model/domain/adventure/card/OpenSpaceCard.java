package it.polimi.ingsw.model.domain.adventure.card;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

public class OpenSpaceCard extends AdventureCard {

    public OpenSpaceCard(String id, CardLevel level, String description) {
        super(id, level, description, AdventureType.OPEN_SPACE);
    }
    
    // accept visitor
    private <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}
