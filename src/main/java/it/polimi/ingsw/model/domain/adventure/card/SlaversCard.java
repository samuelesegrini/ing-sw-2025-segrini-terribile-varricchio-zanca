package it.polimi.ingsw.model.domain.adventure.card;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

public class SlaversCard extends EnemyCard {
    private int creditReward;
    private int crewLossAmount;

    /**
     * Constructs a new Slavers encounter card with the specified attributes.
     *
     * @param id The unique identifier for the Slavers card.
     * @param level The card's level.
     * @param description A brief description of the Slavers card's effect.
     * @param powerLevel The power level of the Slavers' attack, determining the challenge of the encounter.
     * @param movementPenalty The penalty to movement (flight days) that the player will face.
     * @param creditReward The amount of credits the player will earn if they defeat the Slavers.
     * @param crewLossAmount The number of crew members the player will lose if defeated by the Slavers.
     */
    public SlaversCard(String id, CardLevel level, String description,
                     int powerLevel, int movementPenalty,
                     int creditReward, int crewLossAmount) {
        super(id, level, description, AdventureType.SLAVERS, powerLevel, movementPenalty);
        this.creditReward = creditReward;
        this.crewLossAmount = crewLossAmount;
    }
    public int getCreditReward() {
        return this.creditReward;
    }
    public int getCrewLossAmount() {
        return this.crewLossAmount;
    }

    /**
     * Accepts a visitor to process this Slavers card according to the visitor pattern.
     *
     * @param visitor The visitor handling the card logic.
     * @param state The current game state.
     * @return The result of the visitor's processing.
     */
    public boolean accept(AdventureCardVisitor visitor, GameState state){
        return visitor.visitSlaversCard(this, state);
    }
}
