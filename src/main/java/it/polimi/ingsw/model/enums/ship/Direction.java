package it.polimi.ingsw.model.enums.ship;

public enum Direction {
    /**
     * Direction is Up.
     */
    UP,

    /**
     * Direction is Down.
     */
    DOWN,

    /**
     * Direction is Left.
     */
    LEFT,

    /**
     * Direction is Right.
     */
    RIGHT;

    /**
     * Returns the number of 90° rotations needed to get from the initial direction (from) to the final direction (to).
     * @param from The initial direction
     * @param to The final direction
     * @return The number of steps needed
     */
    public int getRotationSteps(Direction from, Direction to) {return 0;}

    /**
     * Gets the opposite direction to the given one.
     * @return The opposite direction
     */
    public Direction getOpposite() {
        return null;
    }

    /**
     * Rotates direction 90° clockwise and returns the resulting direction.
     * @return The direction after the 90° clockwise rotation
     */
    public Direction rotateClockwise() {
        return null;
    }

    /**
     * Rotates direction 90° counterclockwise and returns the resulting direction.
     * @return The direction after the 90° counterclockwise rotation
     */
    public Direction rotateCounterClockwise() {
        return null;
    }
}
