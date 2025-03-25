package it.polimi.ingsw.model.domain.adventure.card;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

public abstract class EnemyCard extends AdventureCard {

    private int powerLevel;
    private int movementPenalty;
    private boolean defeated;

    /**
     * Constructs a new enemy card with the specified details.
     *
     * @param id The unique identifier for the enemy card.
     * @param level The level of the card.
     * @param description A description that explains the effects and consequences of the encounter with the enemy.
     * @param type The type of adventure associated with this enemy card.
     * @param powerLevel The power level of the enemy's attack, which determines how challenging the encounter will be.
     * @param movementPenalty The penalty to the player's movement (flight days) if they are defeated by the enemy.
     */
    public EnemyCard(String id, CardLevel level, String description, AdventureType type, int powerLevel, int movementPenalty) {
        super(id, level, description, type);
        this.powerLevel = powerLevel;
        this.movementPenalty = movementPenalty;
        this.defeated = false;
    }

    public int getMovementPenalty() {
        return this.movementPenalty;
    }
    public int getPowerLevel() {
        return this.powerLevel;
    }

    /**
     * Checks if the Enemy has been defeated.
     * This method returns true if a player successfully defeats the Enemy, and false otherwise.
     *
     * @return {@code true} if the Enemy has been defeated, {@code false} otherwise.
     */
    public boolean isDefeated(){
        return defeated;
    }
    public void setDefeated(){
        this.defeated = true;
    }

    /**
     * Accepts a visitor to process this EnemyCard according to the visitor pattern.
     *
     * @param visitor The visitor handling the card logic.
     * @param state The current game state.
     * @return The result of the visitor's processing.
     */
    public abstract boolean accept(AdventureCardVisitor visitor, GameState state);
}
