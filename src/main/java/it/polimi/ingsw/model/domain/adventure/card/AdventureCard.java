package it.polimi.ingsw.model.domain.adventure.card;

import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

/**
 * Represents a generic adventure card used in the game.
 * The specific behavior and effects of the card depend on its type and level.
 */
public abstract class AdventureCard {
    private String id;
    private CardLevel level;
    private String description;
    private AdventureType type;

    /**
     * Constructs a new AdventureCard.
     * This constructor initializes the card with its unique ID, level, description, and type.
     *
     * @param id The unique identifier for the card.
     * @param level The level of the card.
     * @param description The description explaining the card's effect.
     * @param type The type of the adventure card.
     */
    public AdventureCard(String id, CardLevel level, String description, AdventureType type){
        this.id = id;
        this.level = level;
        this.description = description;
        this.type = type;
    }
}
