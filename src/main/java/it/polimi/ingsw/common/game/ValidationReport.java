package it.polimi.ingsw.common.game;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What is wrong with a ship, if anything.
 *
 * <p>Returned rather than thrown. An illegal ship is a normal state of play — the
 * physical game expects players to build faster than they check — and the rules say
 * what to do about it: remove components until the ship is legal, and count them as
 * lost along the route (manual p.8, p.17).
 *
 * @param violations every broken rule found, in the order they were found
 */
public record ValidationReport(List<ShipViolation> violations) {

    /**
     * Takes a defensive copy of the violations.
     *
     * @throws NullPointerException if the list is {@code null}
     */
    public ValidationReport {
        violations = List.copyOf(violations);
    }

    /**
     * Returns a report for a ship that breaks no rule.
     *
     * @return an empty report
     */
    public static ValidationReport legal() {
        return new ValidationReport(List.of());
    }

    /**
     * Tells whether the ship may fly as it stands.
     *
     * @return {@code true} when no rule is broken
     */
    public boolean isLegal() {
        return violations.isEmpty();
    }

    /**
     * Returns every cell named by a violation.
     *
     * <p>This is the set a player picks from when fixing the ship: removing components
     * outside it can never make the ship legal.
     *
     * @return the cells involved in at least one violation
     */
    public Set<Position> offendingCells() {
        Set<Position> cells = new LinkedHashSet<>();
        violations.forEach(violation -> cells.addAll(violation.cells()));
        return Set.copyOf(cells);
    }

    /**
     * Tells whether any violation of the given kind was found.
     *
     * @param kind the rule to look for
     * @return {@code true} when at least one violation of that kind is present
     */
    public boolean hasViolationOf(ViolationKind kind) {
        return violations.stream().anyMatch(violation -> violation.kind() == kind);
    }
}
