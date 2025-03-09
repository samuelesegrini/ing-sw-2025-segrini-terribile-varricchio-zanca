package it.polimi.ingsw.model.domain.ship;

import java.util.Map;
import java.util.Set;

import it.polimi.ingsw.model.enums.ship.Direction;

public class ShipBoardLayout {
    private Set<Position> validPositions;
    private Position centerPosition;
    private Map<Direction, Position> adjacentPositionCache;

    public ShipBoardLayout(ShipBoardLayout level) {
        this.validPositions = level.validPositions;
        this.centerPosition = level.centerPosition;
        this.adjacentPositionCache = level.adjacentPositionCache;
    }

    // returns true if given position is valid, false otherwise
    public boolean isValidPosition(Position position) {return false;}

    // returns ship's center position (starting cabin)
    public Position getCenterPosition() {return centerPosition;}

    // returns a set with adjacent positions to the given one
    public Set<Position> getAdjacentPositions(Position position) {return null;}

    // initializes positions
    public void initializePositions() {}
}
