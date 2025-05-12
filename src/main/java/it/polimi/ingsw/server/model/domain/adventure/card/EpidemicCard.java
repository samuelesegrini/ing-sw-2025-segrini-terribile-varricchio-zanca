package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;

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
     * @param description A brief description of the Epidemic card effects and consequences..
     */
    public EpidemicCard(String id, CardLevel level, String description) {
        super(id, level, description, AdventureType.EPIDEMIC);
    }

    /**
     * Accepts a visitor that performs some action on the Epidemic card.
     *
     * @param visitor The visitor to accept.
     * @param state The current game state.
     * @return The result of the visitor's action on the Epidemic card.
     */
    public boolean accept(AdventureCardVisitor visitor, GameModel state){
        return visitor.visitEpidemicCard(this, state);
    }
}
