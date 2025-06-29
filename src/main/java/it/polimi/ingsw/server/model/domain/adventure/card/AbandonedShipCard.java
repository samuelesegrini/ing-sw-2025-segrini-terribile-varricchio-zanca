package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
/**
 * Represents an adventure card for an abandoned ship. A player can choose to repair the abandoned
 * ship, losing crew, gaining cosmic credits, and spending flight days.
 * Only one player can use this opportunity, starting with the leader.
 * Once a player repairs the ship, no other players can take the opportunity.
 */
public class AbandonedShipCard extends AdventureCard {
    private static final long serialVersionUID = 1L;
    
    private int crewLost;
    private int creditsGained;
    private int lostDays;
    private boolean isVisited;

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
        this.isVisited = false;
    }

    public int getLostDays() { return this.lostDays; }
    public int getCrewLost() {
        return crewLost;
    }
    public int getCreditsGained() {
        return creditsGained;
    }
    public boolean isVisited() { return isVisited; }
    public void setVisited() {
        isVisited = true;
    }


    /**
    * Accepts a visitor to perform some operation on this adventure card.
    * This is part of the Visitor design pattern, where specific logic can be applied to
    * different types of adventure cards.
    *
    * @param visitor The visitor that performs an operation on this card.
    * @param state The current state of the game.
     * @return The result of applying the visitor to this card
     */
    public void accept(AdventureCardVisitor visitor, GameModel state){
        visitor.visitAbandonedShipCard(this, state);
    }

}
