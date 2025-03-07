package it.polimi.ingsw.model.adventure;

public class MeteorSwarmCard {

    private List<Meteor> meteorPattern;

    //constructor
    public MeteorSwarmCard(List<Meteor> meteorPattern) {
        this.meteorPattern = meteorPattern;
    }

    //get the list of what meteors and their relative intensity and direction is the swarm made of
    public List<Meteor> getMeteorPattern(){}

    // accept visitor
    public <T> T accept(AdventureCardVisitor<T> visitor, GameState state){}
}
