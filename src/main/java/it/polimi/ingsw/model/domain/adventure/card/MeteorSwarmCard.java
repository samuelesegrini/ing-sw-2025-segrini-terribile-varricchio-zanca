package it.polimi.ingsw.model.domain.adventure.card;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.domain.adventure.entity.Meteor;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

public class MeteorSwarmCard extends AdventureCard {

    private List<Meteor> meteorPattern;

    //constructor
    public MeteorSwarmCard(String id, CardLevel level, String description, List<Meteor> meteorPattern) {
        super(id, level, description, AdventureType.METEOR_SWARM);
        this.meteorPattern = new ArrayList<>(meteorPattern);
    }

    //get the list of what meteors and their relative intensity and direction is the swarm made of
    public List<Meteor> getMeteorPattern(){
        return List.of();
    }

    // accept visitor
    public <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}
