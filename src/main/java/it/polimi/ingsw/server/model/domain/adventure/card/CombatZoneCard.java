package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.server.model.domain.adventure.entity.CombatCheck;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;

import java.util.ArrayList;
import java.util.List;

public class CombatZoneCard extends AdventureCard {
    private static final long serialVersionUID = 1L;
    
    private List<CombatCheck> combatChecks;

    /**
     * Constructs a CombatZone card with a unique ID, level, and description.
     *
     * @param id The unique identifier of the card.
     * @param level The difficulty level of the card.
     * @param description A brief description of the card.
     */
    public CombatZoneCard(String id, CardLevel level, String description) {
        super(id, level, description, AdventureType.COMBAT_ZONE);
        this.combatChecks = new ArrayList<>();
    }

    public List<CombatCheck> getCombatChecks() { return combatChecks; }
    /**
     * Adds a CombatCheck to this CombatZone.
     * Each CombatCheck defines a comparison between players based on a specific attribute.
     *
     * @param check The CombatCheck to be added.
     */
    public void addCombatCheck(CombatCheck check){
        this.combatChecks.add(check);
    }

    /**
     * Accepts a visitor to process this CombatZone card according to the visitor pattern.
     *
     * @param visitor The visitor handling the card logic.
     * @param state The current game state.
     *
     */
    public void accept(AdventureCardVisitor visitor, GameModel state){
         visitor.visitCombatZoneCard(this, state);
    }
}
