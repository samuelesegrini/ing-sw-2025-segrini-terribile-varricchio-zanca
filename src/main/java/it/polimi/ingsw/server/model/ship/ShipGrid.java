package it.polimi.ingsw.server.model.ship;

import it.polimi.ingsw.server.model.board.ShipBoardSpec;
import it.polimi.ingsw.server.model.component.ShipComponent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Which component sits in which cell of one ship, and how those cells relate.
 *
 * <p>Package-private on purpose. Every question a caller has about a ship — is it
 * legal, what is its firepower, what does this meteor hit — is answered by
 * {@code Ship}, which owns exactly one of these. Letting callers reach the cells
 * directly is how the previous implementation ended up with two validators
 * disagreeing about the same rules.
 *
 * <p>The distinction this class draws, and the one the rest of the model depends on,
 * is between <em>adjacent</em> and <em>interconnected</em>. Two cells are adjacent when
 * they touch. They are interconnected when the two sides facing each other form a legal
 * joint. A ship holds together through interconnection: a piece touching the hull only
 * along two smooth sides is not attached to anything, and flies off the moment its real
 * joints are cut.
 */
final class ShipGrid {

    private final ShipBoardSpec spec;
    private final Map<Position, ShipComponent> cells = new LinkedHashMap<>();

    /**
     * Creates an empty grid with the outline of the given board.
     *
     * @param spec the board outline, never {@code null}
     */
    ShipGrid(ShipBoardSpec spec) {
        this.spec = spec;
    }

    /**
     * Returns the outline this grid was built from.
     *
     * @return the board specification
     */
    ShipBoardSpec spec() {
        return spec;
    }

    // ---------------------------------------------------------------- occupancy

    /**
     * Returns the component in a cell.
     *
     * @param cell the cell to look at
     * @return the component there, or empty when the cell is free or off the ship
     */
    Optional<ShipComponent> at(Position cell) {
        return Optional.ofNullable(cells.get(cell));
    }

    /**
     * Tells whether a cell holds a component.
     *
     * @param cell the cell to look at
     * @return {@code true} when something is welded there
     */
    boolean isOccupied(Position cell) {
        return cells.containsKey(cell);
    }

    /**
     * Tells whether a component could ever occupy a cell.
     *
     * @param cell the cell to look at
     * @return {@code true} when the cell is inside the ship outline
     */
    boolean isUsable(Position cell) {
        return spec.isUsable(cell);
    }

    /**
     * Returns every occupied cell and what occupies it, in placement order.
     *
     * @return an unmodifiable view of the grid
     */
    Map<Position, ShipComponent> occupied() {
        return Collections.unmodifiableMap(cells);
    }

    /**
     * Returns how many components are welded to this ship.
     *
     * @return the number of occupied cells
     */
    int size() {
        return cells.size();
    }

    /**
     * Welds a component into a cell.
     *
     * @param cell      where it goes
     * @param component what goes there
     * @throws IllegalArgumentException if the cell is outside the ship outline
     * @throws IllegalStateException    if the cell is already taken
     */
    void put(Position cell, ShipComponent component) {
        if (!isUsable(cell)) {
            throw new IllegalArgumentException(cell + " is outside the ship outline");
        }
        if (isOccupied(cell)) {
            throw new IllegalStateException(cell + " already holds " + cells.get(cell).id());
        }
        cells.put(cell, component);
    }

    /**
     * Takes the component out of a cell.
     *
     * @param cell the cell to clear
     * @return what was there, or empty when the cell was already free
     */
    Optional<ShipComponent> remove(Position cell) {
        return Optional.ofNullable(cells.remove(cell));
    }

    // ---------------------------------------------------------------- joints

    /**
     * Tells whether two touching cells form a real joint.
     *
     * <p>Both cells must be occupied and the two facing sides must weld together. Two
     * smooth sides sitting next to each other is legal but is not a joint, which is
     * exactly the case that makes adjacency the wrong test for whether a ship holds
     * together.
     *
     * @param cell      one cell
     * @param direction the side of that cell to look across
     * @return {@code true} when the two pieces are welded to each other
     */
    boolean isJointAcross(Position cell, Direction direction) {
        Position neighbour = cell.neighbour(direction);
        Optional<ShipComponent> here = at(cell);
        Optional<ShipComponent> there = at(neighbour);
        if (here.isEmpty() || there.isEmpty()) {
            return false;
        }
        return here.get().connectorFacing(direction)
                .joinsTo(there.get().connectorFacing(direction.opposite()));
    }

    /**
     * Returns the cells welded directly to the given one.
     *
     * @param cell the cell to look around
     * @return its interconnected neighbours
     */
    Set<Position> jointNeighbours(Position cell) {
        Set<Position> joined = new HashSet<>();
        for (Direction direction : Direction.values()) {
            if (isJointAcross(cell, direction)) {
                joined.add(cell.neighbour(direction));
            }
        }
        return joined;
    }

    /**
     * Tells whether a cell has an occupied neighbour, whether or not they weld.
     *
     * <p>This is the placement-time test: a new piece must touch the ship somewhere.
     * Whether the joint is legal is settled at the end of building, the way the physical
     * game settles it by eye.
     *
     * @param cell the cell to look around
     * @return {@code true} when at least one neighbouring cell is occupied
     */
    boolean touchesShip(Position cell) {
        for (Direction direction : Direction.values()) {
            if (isOccupied(cell.neighbour(direction))) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- connectivity

    /**
     * Returns every cell reachable from the given one through joints.
     *
     * @param start where to start; need not be occupied
     * @return the piece of ship that cell belongs to, empty when the cell is free
     */
    Set<Position> pieceContaining(Position start) {
        Set<Position> reached = new HashSet<>();
        if (!isOccupied(start)) {
            return reached;
        }
        Deque<Position> frontier = new ArrayDeque<>();
        frontier.push(start);
        reached.add(start);
        while (!frontier.isEmpty()) {
            for (Position next : jointNeighbours(frontier.pop())) {
                if (reached.add(next)) {
                    frontier.push(next);
                }
            }
        }
        return reached;
    }

    /**
     * Returns the separate pieces this grid currently holds.
     *
     * <p>A legal ship is exactly one piece. More than one means the ship has broken up
     * and the player has to choose which piece to keep flying (manual p.10).
     *
     * @return one set of cells per piece, largest first
     */
    List<Set<Position>> pieces() {
        Set<Position> seen = new HashSet<>();
        List<Set<Position>> pieces = new ArrayList<>();
        for (Position cell : cells.keySet()) {
            if (seen.contains(cell)) {
                continue;
            }
            Set<Position> piece = pieceContaining(cell);
            seen.addAll(piece);
            pieces.add(piece);
        }
        pieces.sort(Collections.reverseOrder(java.util.Comparator.comparingInt(Set::size)));
        return pieces;
    }

    /**
     * Tells whether the ship is a single piece.
     *
     * @return {@code true} when every component is welded to every other, directly or not
     */
    boolean isWhole() {
        return pieces().size() <= 1;
    }

    // ---------------------------------------------------------------- lines of fire

    /**
     * Returns the first component a threat meets travelling along a line.
     *
     * <p>Meteors and cannon fire come from a direction and travel down a column or
     * along a row until they hit something (manual p.13). The line is named by a roll
     * of two dice, so it may not exist at all; that case is the caller's to handle.
     *
     * @param from the direction the threat arrives from
     * @param line the row index for a threat from east or west, the column index otherwise
     * @return the first occupied cell along that line, or empty when the line is clear
     */
    Optional<Position> firstInLine(Direction from, int line) {
        List<Position> path = new ArrayList<>();
        if (from.addressesColumn()) {
            for (int row = 0; row < spec.rows(); row++) {
                path.add(new Position(row, line));
            }
            if (from == Direction.SOUTH) {
                Collections.reverse(path);
            }
        } else {
            for (int column = 0; column < spec.columns(); column++) {
                path.add(new Position(line, column));
            }
            if (from == Direction.EAST) {
                Collections.reverse(path);
            }
        }
        return path.stream().filter(this::isOccupied).findFirst();
    }

    // ---------------------------------------------------------------- exposure

    /**
     * Tells whether a component shows a connector to open space on the given side.
     *
     * <p>An exposed connector is one with nothing next to it — off the board counts as
     * nothing. Smooth sides are never exposed connectors, however open they are, which
     * is why a well built ship shrugs off small meteors (manual p.8).
     *
     * @param cell the cell to look at
     * @param side the side to check
     * @return {@code true} when that side carries pipes and faces nothing
     */
    boolean isConnectorExposed(Position cell, Direction side) {
        return at(cell)
                .map(component -> component.connectorFacing(side).isConnector())
                .orElse(false)
                && !isOccupied(cell.neighbour(side));
    }

    /**
     * Counts the exposed connectors of the whole ship.
     *
     * <p>One per exposed side, whatever its pipe count. This is what Stardust charges a
     * flight day for and what the prettiest ship reward measures (manual p.8).
     *
     * @return the number of exposed connectors
     */
    int exposedConnectors() {
        int exposed = 0;
        for (Position cell : cells.keySet()) {
            for (Direction side : Direction.values()) {
                if (isConnectorExposed(cell, side)) {
                    exposed++;
                }
            }
        }
        return exposed;
    }
}
