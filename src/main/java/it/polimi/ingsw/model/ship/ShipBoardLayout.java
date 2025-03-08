package it.polimi.ingsw.model.ship;

import java.util.Map;
import java.util.Set;

public class ShipBoardLayout {
    private Set<Position> validPositions;
    private Position centerPosition;
    private Map<Direction, Position> adjacentPositionCache;

    public ShipBoardLayout(ShipBoardLayout level) {
        this.validPositions = level.validPositions;
        this.centerPosition = level.centerPosition;
        this.adjacentPositionCache = level.adjacentPositionCache;
    }


    public boolean isValidPosition(Position position) {return false;}
    public Position getCenterPosition() {return centerPosition;}
    public Set<Position> getAdjacentPositions(Position position) {return null;}
    public void initializePositions() {}
}
