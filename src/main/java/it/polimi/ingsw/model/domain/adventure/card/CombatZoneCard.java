package it.polimi.ingsw.model.domain.adventure.card;

import it.polimi.ingsw.model.domain.GameState;
import it.polimi.ingsw.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.model.domain.adventure.entity.CombatCheck;
import it.polimi.ingsw.model.enums.adventure.AdventureType;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

import java.util.ArrayList;
import java.util.List;

public class CombatZoneCard extends AdventureCard {
    private List<CombatCheck> combatChecks;

    public CombatZoneCard(String id, CardLevel level, String description) {
        super(id, level, description, AdventureType.WAR_ZONE);
        this.combatChecks = new ArrayList<>();
    }

    public void addCombatCheck(CombatCheck check){}
    private <T> T accept(AdventureCardVisitor<T> visitor, GameState state){
        return null;
    }
}
