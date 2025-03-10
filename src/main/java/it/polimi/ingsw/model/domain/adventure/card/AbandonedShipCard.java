package it.polimi.ingsw.model.domain.adventure.card;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;
/**
 * Represents an adventure card for an abandoned ship. A player can choose to repair the abandoned
 * ship, losing crew, gaining cosmic credits, and spending flight days.
 * Only one player can use this opportunity, starting with the leader.
 * Once a player repairs the ship, no other players can take the opportunity.
 */
public class AbandonedShipCard extends AdventureCard {
    private int crewLost;
    private int creditsGained;
    private int lostDays;

    /**
     * Constructs a new AbandonedShipCard with the specified details.
     *
     * @param id The unique identifier for the card.
     * @param level The level of the card.
     * @param description A description of the effect of the abandoned ship card.
     * @param crewLost The number of crew figures lost.
     * @param creditsGained The number of cosmic credits rewarded for fixing the ship.
     * @param lostDays The number of flight days lost to fix the ship.
     */
    public AbandonedShipCard(String id, CardLevel level, String description, int crewLost, int creditsGained, int lostDays) {
        super(id, level, description, AdventureType.ABANDONED_SHIP);
        this.crewLost = crewLost;
        this.creditsGained = creditsGained;
        this.lostDays = lostDays;
    }

    /**
     * Returns the number of crew members that must be lost to repair the abandoned ship.
     *
     * @return The number of crew members to be lost.
     */
    public int getCrewLost(){
        return 0;
    }

    /**
     * Returns the number of cosmic credits a player gains by repairing the abandoned ship.
     *
     * @return The number of credits gained.
     */
    public int getCreditsGained(){
        return 0;
    }

    /**
     * Returns the number of flight days a player loses when repairing the abandoned ship.
     *
     * @return The number of flight days spent on the repair.
     */
    public int getLostDays(){
        return 0;
    }

    /**
    * Accepts a visitor to perform some operation on this adventure card.
    * This is part of the Visitor design pattern, where specific logic can be applied to
    * different types of adventure cards.
    *
    * @param visitor The visitor that performs an operation on this card.
    * @param state The current state of the game.
     * @param <T> The type of result returned by the visitor
     * @return The result of applying the visitor to this card
     */
    public <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}
