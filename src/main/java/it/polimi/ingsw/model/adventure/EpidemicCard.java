package it.polimi.ingsw.model.adventure;

public class EpidemicCard {

    // accept visitor
    private <T> T accept(AdventureCardVisitor<T> visitor, GameState state){}
}
