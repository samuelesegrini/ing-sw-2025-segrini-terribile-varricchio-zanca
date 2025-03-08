package it.polimi.ingsw.model.domain.adventure.entity;

import it.polimi.ingsw.model.enums.adventure.CombatAttributeType;
import it.polimi.ingsw.model.enums.adventure.PenaltyType;

import java.util.ArrayList;
import java.util.List;

public class CombatCheck {

    private CombatAttributeType attribute;
    private PenaltyType penaltyType;
    private int penaltyValue;
    private List<CannonFire> cannonFires;

    public CombatCheck(CombatAttributeType attribute, PenaltyType penaltyType, int penaltyValue) {
        this.attribute = attribute;
        this.penaltyType = penaltyType;
        this.penaltyValue = penaltyValue;
        this.cannonFires = new ArrayList<>();
    }

    public CombatCheck(CombatAttributeType attribute, List<CannonFire> cannonFires) {
        this.attribute = attribute;
        this.penaltyType = null;
        this.penaltyValue = 0;
        this.cannonFires = new ArrayList<>(cannonFires);
    }
    
    //maybe add some getters
}
