package it.polimi.ingsw.model.domain.adventure.card;

public class EpidemicCard extends AdventureCard {

    // accept visitor
    private <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}
