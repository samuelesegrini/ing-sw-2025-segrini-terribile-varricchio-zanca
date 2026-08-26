package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.common.game.ShipAttribute;

/**
 * One line of a combat zone: an attribute to compare and what the weakest ship suffers.
 *
 * @param attribute what is being compared
 * @param penalty   what the ship with the lowest value pays
 */
public record CombatLine(ShipAttribute attribute, CombatPenalty penalty) {

    /**
     * Validates the line.
     *
     * @throws NullPointerException if either part is {@code null}
     */
    public CombatLine {
        if (attribute == null || penalty == null) {
            throw new NullPointerException("a combat line needs an attribute and a penalty");
        }
    }

    /**
     * Tells whether players have to declare anything for this line.
     *
     * <p>Crew is simply counted: nothing about it is optional, so there is no decision to
     * put to anyone. Firepower and engine power both involve choosing which doubles to pay
     * for, and the manual has players decide in route order (p.13).
     *
     * @return {@code true} when each player must be asked
     */
    public boolean needsDeclaring() {
        return attribute != ShipAttribute.CREW;
    }
}
