package it.polimi.ingsw.model.domain.adventure.card;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import it.polimi.ingsw.model.domain.general.GameModel;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

/**
 * Represents the Planets event in the game, where players can land on one of several planets to pick up goods.
 * Each player will decide whether to land on a planet, which costs a certain number of flight days.
 * Only one rocket is allowed per planet, and the leader chooses first, followed by other players in order.
 */
public class PlanetsCard extends AdventureCard {

    private List<Planet> planets;
    private int lostDays;

    /**
     * Constructs a new Planets event card with the specified details.
     *
     * @param id The unique identifier for the Planets card.
     * @param level The level of the card.
     * @param description A description explaining the event and its effects on the player's actions.
     * @param lostDays The number of flight days players lose when they land on a planet.
     * @param planets A list of planets that players can choose to land on to pick up goods.
     */
    public PlanetsCard(String id, CardLevel level, String description, int lostDays, List<Planet> planets) {
        super(id, level, description, AdventureType.PLANETS);
        this.lostDays = lostDays;
        this.planets = new ArrayList<>(planets);
    }
    public List<Planet> getPlanets(){
        return this.planets;
    }
    public int getLostDays(){
        return this.lostDays;
    }

    /**
     * Accepts a visitor to process this PlanetsCard.
     *
     * @param visitor The visitor implementing which will perform actions on the card.
     * @param state The current game state.
     * @param <T> The return type of the visitor's action.
     * @return The result of the visitor's action on the PlanetsCard.
     */
    public <T> T accept(AdventureCardVisitor<T> visitor, GameModel state){
        return visitor.visitPlanetsCard(this,state);
    }
}