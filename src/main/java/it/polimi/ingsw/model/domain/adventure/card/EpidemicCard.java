package it.polimi.ingsw.model.domain.adventure.card;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

/**
 * Represents an Epidemic event card in the game. When an Epidemic occurs, the player must remove one crew member
 * (human or alien) from every occupied cabin that is joined to another occupied cabin.
 */
public class EpidemicCard extends AdventureCard {

    /**
     * Constructs a new Epidemic event card with the specified attributes.
     *
     * @param id The unique identifier for the Epidemic card.
     * @param level The card's level.
     * @param description A brief description of the Epidemic card effects and consequences.
     * @param type The type of adventure associated with this Epidemic event card.
     */
    public EpidemicCard(String id, CardLevel level, String description, AdventureType type) {
        super(id, level, description, type);
    }

    /**
     * Accepts a visitor that performs some action on the Epidemic card.
     *
     * @param visitor The visitor to accept.
     * @param state The current game state.
     * @param <T> The type of the result returned by the visitor.
     * @return The result of the visitor's action on the Epidemic card.
     */
    private <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}
