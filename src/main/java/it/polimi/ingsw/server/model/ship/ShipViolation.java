package it.polimi.ingsw.server.model.ship;

import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.ViolationKind;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One assembly rule a ship breaks, and the cells involved in breaking it.
 *
 * <p>The cells matter as much as the rule. A player fixing an illegal ship removes
 * components of their own choosing until it is legal (manual p.8), so what they need
 * from a validator is not "this ship is wrong" but "these pieces are what makes it
 * wrong".
 *
 * @param kind        which rule is broken
 * @param cells       the cells involved, at least one
 * @param description a sentence naming the pieces at fault
 */
public record ShipViolation(ViolationKind kind, Set<Position> cells, String description) {

    /**
     * Validates the violation and takes a defensive copy of its cells.
     *
     * @throws IllegalArgumentException if no cell is named
     * @throws NullPointerException     if the kind or the description is {@code null}
     */
    public ShipViolation {
        if (kind == null) {
            throw new NullPointerException("a violation needs a kind");
        }
        if (description == null) {
            throw new NullPointerException("a violation needs a description");
        }
        Set<Position> copy = new LinkedHashSet<>(cells);
        if (copy.isEmpty()) {
            throw new IllegalArgumentException(kind + ": a violation has to name at least one cell");
        }
        cells = Set.copyOf(copy);
    }
}
