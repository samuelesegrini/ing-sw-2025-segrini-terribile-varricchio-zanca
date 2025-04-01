package it.polimi.ingsw.model.domain.ship;

import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.Objects;

public class Position {
    private int x;
    private int y;

    /**
     * Constructor defined by user.
     * @param x row coordinate.
     * @param y column coordinate.
     */
    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the vertical (row) coordinate.
     * @return The x-coordinate.
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the horizontal (column) coordinate.
     * @return The y-coordinate.
     */
    public int getY() {
        return  y;
    }

    /**
     * Returns a new Position that is offset from the current position in the specified direction.
     *
     * In our coordinate system:
     * - The origin (0,0) is in the top-left corner of the board
     * - The x-coordinate represents rows and increases as we move down
     * - The y-coordinate represents columns and increases as we move right
     *
     * This implementation follows standard 2D array indexing in Java where:
     * - The first index [x] represents the row (vertical position)
     * - The second index [y] represents the column (horizontal position)
     *
     * Therefore:
     * - UP decreases the row index (x-1)
     * - DOWN increases the row index (x+1)
     * - LEFT decreases the column index (y-1)
     * - RIGHT increases the column index (y+1)
     *
     * @param direction the direction to offset in (UP, DOWN, LEFT, or RIGHT)
     * @return a new Position that is one unit away from the current position in the specified direction
     * @throws IllegalArgumentException if the direction is null
     */
    public Position offsetBy(Direction direction) throws IllegalArgumentException {
        if (direction == null) {
            throw new IllegalArgumentException("Not a valid direction");
        }

        return switch (direction) {
            case UP -> new Position(x - 1, y);
            case DOWN -> new Position(x + 1, y);
            case LEFT -> new Position(x, y - 1);
            case RIGHT -> new Position(x, y + 1);
        };
    }

    /**
     * Returns a hash code value for the Position.
     * @return The hash code of the Position.
     */
    public int hashCode() {
        // Combines x, y in a single and unique value that contains both the information about x and y
        return Objects.hash(x, y);
    }
}
