package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;

/**
 * Represents the Stardust event in the game, a special event that causes each player to lose flight days
 * for every exposed connector on their ship. The event is applied in reverse order, starting with the last player.
 */
public class StardustCard extends AdventureCard {

    /**
     * Constructs a new Stardust event card with the specified details.
     *
     * @param id The unique identifier for the Stardust card.
     * @param level The level of the card.
     * @param description A description explaining the event and its effects on the player's actions.
     */
    public StardustCard(String id, CardLevel level, String description) {
        super(id, level, description, AdventureType.STARDUST);
    }

    /**
     * Accepts a visitor to process this StardustCard.
     *
     * @param visitor The visitor which will perform actions on the card.
     * @param state The current game state.
     * @return The result of the visitor's action on the StardustCard.
     */
    public boolean accept(AdventureCardVisitor visitor, GameModel state){
        return visitor.visitStardustCard(this, state);
    }
}
