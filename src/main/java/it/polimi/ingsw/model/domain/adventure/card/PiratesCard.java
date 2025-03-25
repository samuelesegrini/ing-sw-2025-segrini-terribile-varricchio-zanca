package it.polimi.ingsw.model.domain.adventure.card;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.model.domain.general.GameModel;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.domain.adventure.entity.CannonFire;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

public class PiratesCard extends EnemyCard {

    private int creditReward;
    private List<CannonFire> attackPattern;

    /**
     * Constructs a new Pirates encounter card with the specified attributes.
     *
     * @param id The unique identifier for the Pirates card.
     * @param level The card's level, which may affect the strength or difficulty of the encounter.
     * @param description A brief description of the Pirates' card effects.
     * @param powerLevel The power level of the Pirates' attack, determining the difficulty of the encounter.
     * @param movementPenalty The penalty to movement (flight days) the player will face if defeated by the Pirates.
     * @param creditReward The amount of credits the player will receive for defeating the Pirates.
     * @param attackPattern A list of {@link CannonFire} instances representing the direction and intensity of the cannon
     * fire the player faces when attacked by the Pirates.
     */
    public PiratesCard(String id, CardLevel level, String description, int powerLevel, int movementPenalty, int creditReward, List<CannonFire> attackPattern) {
        super(id, level, description, AdventureType.PIRATES, powerLevel, movementPenalty);
        this.creditReward = creditReward;
        this.attackPattern = new ArrayList<>(attackPattern);
    }

    public int getCreditReward() {
        return this.creditReward;
    }

    public List<CannonFire> getAttackPattern(){
        return this.attackPattern;
    }

    /**
     * Accepts a visitor that performs some action on the Pirates card.
     *
     * @param visitor The visitor to accept.
     * @param state The current game state.
     * @param <T> The type of the result returned by the visitor.
     * @return The result of the visitor's action on the Pirates card.
     */
    public <T> T accept(AdventureCardVisitor<T> visitor, GameModel state){
        return visitor.visitPiratesCard(this, state);
    }
}
