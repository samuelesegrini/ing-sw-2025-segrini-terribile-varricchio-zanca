package it.polimi.ingsw.model.adventure;

import java.util.List;

public class PlanetsCard {

    private List<Planet> planets;
    private int lostDays;

    //constructor
    public PlanetsCard(int lostDays, List<Planet> planets) {
        this.lostDays = lostDays;
        this.planets = planets;
    }

    //get what planets are on this card
    public List<Planet> getPlanets(){}

    //get what unvisited planets are on this card
    public List<Planet> getUnvisitedPlanets(){}

    //get how many days players lose if they decide to land on a planet
    public int getLostDays(){}

    //acceptor visitor
    public <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}