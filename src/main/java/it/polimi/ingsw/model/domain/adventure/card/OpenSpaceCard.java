package it.polimi.ingsw.model.domain.adventure.card;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;
/**
 * Represents the Open Space event in the game.
 * In this event, each player will declare their engine strength and move their rocket marker forward by that number of empty spaces.
 * The player decides whether to use battery tokens on double engines to enhance their engine strength for the race.
 */
public class OpenSpaceCard extends AdventureCard {

    /**
     * Constructs a new Open Space event card with the specified details.
     *
     * @param id The unique identifier for the Open Space card.
     * @param level The level of the card.
     * @param description A description explaining the event and its effects on the player's movement.
     */
    public OpenSpaceCard(String id, CardLevel level, String description) {
        super(id, level, description, AdventureType.OPEN_SPACE);
    }

    /**
     * Accepts a visitor to process this OpenSpaceCard.
     *
     * @param visitor The visitor which will perform actions on the card.
     * @param state The current game state, which may affect how the visitor interacts with the OpenSpaceCard.
     * @param <T> The return type of the visitor's action.
     * @return The result of the visitor's action on the OpenSpaceCard.
     */
    private <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}
