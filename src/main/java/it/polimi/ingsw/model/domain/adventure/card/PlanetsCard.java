package it.polimi.ingsw.model.domain.adventure.card;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

public class PlanetsCard extends AdventureCard {

    private List<Planet> planets;
    private int lostDays;

    //constructor
    public PlanetsCard(String id, CardLevel level, String description, int lostDays, List<Planet> planets) {
        super(id, level, description, AdventureType.PLANETS);
        this.lostDays = lostDays;
        this.planets = new ArrayList<>(planets);
    }

    //get what planets are on this card
    public List<Planet> getPlanets(){ return planets; }

    //get what unvisited planets are on this card
    public List<Planet> getUnvisitedPlanets(){ return planets; }

    //get how many days players lose if they decide to land on a planet
    public int getLostDays(){ return lostDays; }

    //acceptor visitor
    public <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}