package it.polimi.ingsw.model.domain.ship;

import java.util.Map;
import java.util.Set;
import it.polimi.ingsw.model.enums.ship.Direction;

public class ShipBoardLayout {
    private Set<Position> validPositions;
    private Position centerPosition;
    private Map<Direction, Position> adjacentPositionCache;

    /**
     * Creates a new ShipBoardLayout by copying the layout of an existing level.
     * Each level has a different flight board with distinct valid positions.
     * @param level The existing ShipBoardLayout to copy from.
     */
    public ShipBoardLayout(ShipBoardLayout level) {
        this.validPositions = level.validPositions;
        this.centerPosition = level.centerPosition;
        this.adjacentPositionCache = level.adjacentPositionCache;
    }

    /**
     * Returns a set of positions adjacent to the given position on the ship's board.
     * @param position The position for which to find adjacent positions.
     * @return A set of adjacent positions.
     */
    public boolean isValidPosition(Position position) {return false;}

    /**
     * Returns the center position of the ship's board (starting cabin).
     * @return The center position.
     */
    public Position getCenterPosition() {return centerPosition;}

    /**
     * Checks if the given position is valid on the ship's board.
     * A position is considered valid if it lies within the defined valid positions of the current level's layout.
     * @param position The position to check.
     * @return {@code true} if the position is valid, {@code false} otherwise.
     */
    public Set<Position> getAdjacentPositions(Position position) {return null;}

    /**
     * Initializes the valid positions on the ship's board according to the current level's layout.
     */
    public void initializePositions() {}
}
