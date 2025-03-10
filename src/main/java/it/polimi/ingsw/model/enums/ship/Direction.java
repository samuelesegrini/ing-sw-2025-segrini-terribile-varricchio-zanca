package it.polimi.ingsw.model.enums.ship;

public enum Direction {
    UP, DOWN, LEFT, RIGHT;

    public int getRotationSteps(Direction from, Direction to) {return 0;}
    public Direction getOpposite() {return null;}

    // rotates direction 90° clockwise and returns it
    public Direction rotateClockwise() {return null;}

    // rotates direction 90° counterclockwise and returns it
    public Direction rotateCounterClockwise() {return null;}
}
