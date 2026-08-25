package it.polimi.ingsw.server.model.ship;

import it.polimi.ingsw.server.model.component.CannonComponent;
import it.polimi.ingsw.server.model.component.EngineComponent;
import it.polimi.ingsw.server.model.component.ShipComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The application's stand-in for the players who would check each other's ships.
 *
 * <p>In the physical game the checking is done by eye, by everyone else at the table
 * (manual p.8). The project requirements move that job here: the application decides
 * whether a ship is legal, and the manual's own remedy applies — remove components
 * until it is, and count them as lost along the route.
 *
 * <p>There is exactly one validator. The previous implementation had two,
 * {@code ShipValidationService} and {@code UnifiedShipValidationService}, and they
 * disagreed; a rule with two implementations has no implementation.
 */
final class ShipValidator {

    private final ShipGrid grid;

    /**
     * Creates a validator for one ship.
     *
     * @param grid the ship to check
     */
    ShipValidator(ShipGrid grid) {
        this.grid = grid;
    }

    /**
     * Runs every assembly check and reports what is broken.
     *
     * <p>All checks run: the report lists everything wrong rather than stopping at the
     * first problem, because a player fixing a ship wants to see the whole job before
     * choosing what to sacrifice.
     *
     * @return the violations found, empty when the ship may fly as it stands
     */
    ValidationReport validate() {
        List<ShipViolation> violations = new ArrayList<>();
        checkJoints(violations);
        checkEngines(violations);
        checkCannons(violations);
        checkConnectivity(violations);
        return new ValidationReport(violations);
    }

    /**
     * Checks every pair of touching sides.
     *
     * <p>Two smooth sides touching is legal and produces nothing. Anything else that
     * does not weld is a broken joint, reported as one of two kinds so that the message
     * can say which mistake was made.
     *
     * <p>Each pair is examined once, from the north and west sides only, so a mismatch
     * is not reported twice from both of its ends.
     */
    private void checkJoints(List<ShipViolation> violations) {
        for (Map.Entry<Position, ShipComponent> entry : grid.occupied().entrySet()) {
            Position cell = entry.getKey();
            for (Direction side : List.of(Direction.NORTH, Direction.WEST)) {
                Position neighbour = cell.neighbour(side);
                if (!grid.isOccupied(neighbour)) {
                    continue;
                }
                Connector here = entry.getValue().connectorFacing(side);
                Connector there = grid.at(neighbour).orElseThrow().connectorFacing(side.opposite());

                if (!here.isConnector() && !there.isConnector()) {
                    continue;
                }
                if (here.joinsTo(there)) {
                    continue;
                }
                violations.add(jointViolation(cell, neighbour, here, there));
            }
        }
    }

    private ShipViolation jointViolation(Position cell, Position neighbour, Connector here, Connector there) {
        boolean againstSmoothSide = !here.isConnector() || !there.isConnector();
        ViolationKind kind = againstSmoothSide
                ? ViolationKind.CONNECTOR_MEETS_SMOOTH_SIDE
                : ViolationKind.INCOMPATIBLE_CONNECTORS;
        String description = againstSmoothSide
                ? describe(cell) + " presses a connector against the smooth side of " + describe(neighbour)
                : describe(cell) + " has a " + here + " connector facing the " + there
                        + " connector of " + describe(neighbour);
        return new ShipViolation(kind, Set.of(cell, neighbour), description);
    }

    /**
     * Checks that every engine fires at the stern and into open space.
     *
     * <p>An engine turned any other way is illegal outright (manual p.6), and even a
     * correctly turned one is illegal if anything sits in the cell it fires into. Both
     * are reported when both apply: a sideways engine with something in its way is two
     * separate mistakes, and fixing only one leaves the ship illegal.
     */
    private void checkEngines(List<ShipViolation> violations) {
        forEachComponent(EngineComponent.class, (cell, engine) -> {
            Direction exhaust = engine.exhaustDirection();
            if (exhaust != Direction.SOUTH) {
                violations.add(new ShipViolation(ViolationKind.ENGINE_NOT_FACING_STERN, Set.of(cell),
                        describe(cell) + " fires " + exhaust + " instead of at the stern"));
            }
            Position behind = cell.neighbour(exhaust);
            if (grid.isOccupied(behind)) {
                violations.add(new ShipViolation(ViolationKind.BLOCKED_ENGINE_EXHAUST, Set.of(cell, behind),
                        describe(cell) + " fires straight into " + describe(behind)));
            }
        });
    }

    /**
     * Checks that no cannon has anything in front of its muzzle.
     *
     * <p>Unlike engines, cannons may point any way they like — forward is simply worth
     * more (manual p.6). What they may not have is a component in the cell they fire
     * into.
     */
    private void checkCannons(List<ShipViolation> violations) {
        forEachComponent(CannonComponent.class, (cell, cannon) -> {
            Position inFront = cell.neighbour(cannon.muzzleDirection());
            if (grid.isOccupied(inFront)) {
                violations.add(new ShipViolation(ViolationKind.BLOCKED_CANNON_MUZZLE, Set.of(cell, inFront),
                        describe(cell) + " fires straight into " + describe(inFront)));
            }
        });
    }

    /**
     * Checks that the ship is a single welded piece.
     *
     * <p>Every piece after the largest is reported separately, so that a player looking
     * at the report can see each stranded fragment rather than one undifferentiated
     * blob. Pieces come back largest first, which makes the largest the one presumed
     * worth keeping.
     */
    private void checkConnectivity(List<ShipViolation> violations) {
        List<Set<Position>> pieces = grid.pieces();
        for (int i = 1; i < pieces.size(); i++) {
            Set<Position> stranded = pieces.get(i);
            violations.add(new ShipViolation(ViolationKind.DISCONNECTED, stranded,
                    stranded.size() == 1
                            ? describe(stranded.iterator().next()) + " is welded to nothing"
                            : stranded.size() + " components are welded only to each other"));
        }
    }

    private <T extends ShipComponent> void forEachComponent(Class<T> type, CellAction<T> action) {
        grid.occupied().forEach((cell, component) -> {
            if (type.isInstance(component)) {
                action.accept(cell, type.cast(component));
            }
        });
    }

    private String describe(Position cell) {
        return grid.at(cell)
                .map(component -> component.id() + " at " + printed(cell))
                .orElseGet(() -> "the cell at " + printed(cell));
    }

    private String printed(Position cell) {
        return "row " + grid.spec().printedRow(cell.row())
                + ", column " + grid.spec().printedColumn(cell.column());
    }

    @FunctionalInterface
    private interface CellAction<T> {
        void accept(Position cell, T component);
    }
}
