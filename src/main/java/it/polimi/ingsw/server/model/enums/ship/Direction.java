package it.polimi.ingsw.server.model.enums.ship;

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
    public int getRotationSteps(Direction from, Direction to) {
        if (from == to) {
            return 0;
        }
        else if (from == to.getOpposite()) {
            return 2;
        }
        else {
            return 1;
        }
    }

    /**
     * Gets the opposite direction to the given one.
     * @return The opposite direction
     */
    public Direction getOpposite() {
        return switch (this) {
            case UP -> DOWN;
            case RIGHT -> LEFT;
            case DOWN -> UP;
            case LEFT -> RIGHT;
        };
    }

    /**
     * Rotates direction 90° clockwise and returns the resulting direction.
     * @return The direction after the 90° clockwise rotation
     */
    public Direction rotateClockwise() {
        return switch (this) {
            case UP -> RIGHT;
            case RIGHT -> DOWN;
            case DOWN -> LEFT;
            case LEFT -> UP;
        };
    }

    /**
     * Rotates direction 90° counterclockwise and returns the resulting direction.
     * @return The direction after the 90° counterclockwise rotation
     */
    public Direction rotateCounterClockwise() {
        return switch (this) {
            case UP -> LEFT;
            case RIGHT -> UP;
            case DOWN -> RIGHT;
            case LEFT -> DOWN;
        };
    }
}
