package it.polimi.ingsw.model.ship;

public class ShipBoardLayout {
    private Set<Position> validPositions;
    private Position centerPosition;
    private Map<Direction, Position> adjacentPositionCache;

    public ShipBoardLayout(ShipBoardLayout level) {}


    public boolean isValidPosition(Position position) {return false;}
    public Position getCenterPosition() {return centerPosition;}
    public Set<Position> getAdjacentPositions(Position position) {return null;}
    public void initializePositions() {}
}
