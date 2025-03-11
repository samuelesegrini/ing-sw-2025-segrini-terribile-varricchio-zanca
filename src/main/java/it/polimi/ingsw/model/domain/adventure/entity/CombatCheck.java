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

    /**
     * Constructs a CombatCheck that applies a specific penalty based on a given attribute.
     *
     * @param attribute The attribute being evaluated (e.g., crew count, engine power, cannon strength).
     * @param penaltyType The type of penalty applied to the weakest player.
     * @param penaltyValue The intensity of the penalty (e.g., number of lost flight days or crew members).
     */
    public CombatCheck(CombatAttributeType attribute, PenaltyType penaltyType, int penaltyValue) {
        this.attribute = attribute;
        this.penaltyType = penaltyType;
        this.penaltyValue = penaltyValue;
        this.cannonFires = new ArrayList<>();
    }
    /**
     * Constructs a CombatCheck that involves cannon fire instead of a standard penalty.
     *
     * @param attribute The attribute being evaluated (typically cannon strength).
     * @param cannonFires A list of cannon fire instances affecting the weakest player.
     */
    public CombatCheck(CombatAttributeType attribute, List<CannonFire> cannonFires) {
        this.attribute = attribute;
        this.penaltyType = PenaltyType.CANNON_FIRE ;
        this.penaltyValue = 0;
        this.cannonFires = new ArrayList<>(cannonFires);
    }

    /**
     * Gets the attribute being evaluated in this CombatCheck.
     *
     * @return The attribute type (e.g., crew count, engine power, cannon strength).
     */
    public CombatAttributeType getAttribute() { return attribute;
    }

    /**
     * Gets the penalty type applied in this CombatCheck.
     *
     * @return The type of penalty (e.g., flight days loss, crew loss, cannon fire), or null if not applicable.
     */
    public PenaltyType getPenaltyType() {
        return penaltyType;
    }
    /**
     * Gets the penalty value of this CombatCheck.
     *
     * @return The intensity of the penalty,or 0 if not applicable.
     *
     */
    public int getPenaltyValue() {
        return penaltyValue;
    }

    /**
     * Gets the list of cannon fires applied in this CombatCheck.
     *
     * @return A list of cannon fire instances, or an empty list if no cannon fire is involved.
     */
    public List<CannonFire> getCannonFires() {
        return cannonFires;
    }

}
