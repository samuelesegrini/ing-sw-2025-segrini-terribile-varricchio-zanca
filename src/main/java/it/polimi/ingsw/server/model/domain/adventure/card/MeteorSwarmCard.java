package it.polimi.ingsw.server.model.domain.adventure.card;

import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.server.model.domain.adventure.entity.Meteor;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
/**
 * Represents a Meteor Swarm event card in the game. When drawn, this card triggers a meteor shower that
 * affects the player's ship. The card contains a pattern of meteors, which define the locations and intensity of
 * the meteor strike on the ship. Players must defend their ship from the meteors or suffer damage.
 */
public class MeteorSwarmCard extends AdventureCard {

    private final List<Meteor> meteorPattern;

    public List<Meteor> getMeteorPattern() {
        return meteorPattern;
    }

    /**
     * Constructs a new Meteor Swarm event card with the specified details.
     *
     * @param id The unique identifier for the Meteor Swarm card.
     * @param level The level of the card.
     * @param description A description of the event and its effects on the ship.
     * @param meteorPattern A list of meteors that represent the pattern of meteor strikes on the player's ship.
     */
    public MeteorSwarmCard(String id, CardLevel level, String description, List<Meteor> meteorPattern) {
        super(id, level, description, AdventureType.METEOR_SWARM);
        this.meteorPattern = new ArrayList<>(meteorPattern);
    }

    /**
     * Accepts a visitor to process this MeteorSwarmCard.
     *
     * @param visitor The visitor which will perform actions on the card.
     * @param state The current game state.
     * @return The result of the visitor's action on the MeteorSwarmCard.
     */
    public boolean accept(AdventureCardVisitor visitor, GameModel state){
        return visitor.visitMeteorSwarmCard(this, state);
    }
}
