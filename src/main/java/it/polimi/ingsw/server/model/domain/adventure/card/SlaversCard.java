package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;

/**
 * Represents a Slavers Card in the game.
 *
 * During the flight, players may encounter Slavers. If they defeat them:
 * - They gain cosmic credits instead of goods.
 * - Their ship moves back the indicated number of empty spaces.
 * - Alternatively, they can refuse the credits and remain in place.
 * - Once defeated, no other player can claim the reward.
 *
 * If the player loses to the Slavers:
 * - They must surrender part of their crew (humans or aliens) in exchange for their freedom.
 *
 */
 public class SlaversCard extends EnemyCard {
    private static final long serialVersionUID = 1L;
    
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
     *
     */
    public void accept(AdventureCardVisitor visitor, GameModel state){
        visitor.visitSlaversCard(this, state);
    }
}
