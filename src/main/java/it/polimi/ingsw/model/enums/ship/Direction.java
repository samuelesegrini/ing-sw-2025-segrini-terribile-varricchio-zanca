package it.polimi.ingsw.model.enums.ship;

public enum Direction {
    UP, DOWN, LEFT, RIGHT;

    public int getRotationSteps(Direction from, Direction to) {return 0;}
    public Direction getOpposite() {return null;}
    public Direction rotateClockwise() {return null;}
    public Direction rotateCounterClockwise() {return null;}
}
