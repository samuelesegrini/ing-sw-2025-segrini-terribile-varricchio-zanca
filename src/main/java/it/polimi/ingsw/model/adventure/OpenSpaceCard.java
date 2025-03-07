package it.polimi.ingsw.model.adventure;

public class OpenSpaceCard {

    // accept visitor
    private <T> T accept(AdventureCardVisitor<T> visitor, GameState state){}
}
