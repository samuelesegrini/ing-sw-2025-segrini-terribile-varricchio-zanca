package it.polimi.ingsw.model.domain.adventure.card;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

public class EpidemicCard extends AdventureCard {

    public EpidemicCard(String id, CardLevel level, String description, AdventureType type) {
        super(id, level, description, type);
    }

    // accept visitor
    private <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}
