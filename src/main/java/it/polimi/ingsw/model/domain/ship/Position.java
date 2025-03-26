package it.polimi.ingsw.model.domain.ship;

import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.Objects;

public class Position {
    private int x;
    private int y;

    /**
     * Constructor defined by user.
     * @param x Horizontal coordinate.
     * @param y Vertical coordinate.
     */
    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the horizontal coordinate.
     * @return The x-coordinate.
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the vertical coordinate.
     * @return The y-coordinate.
     */
    public int getY() {
        return  y;
    }

    /**
     * Returns a new Position offset by the specified direction.
     * @param direction The direction to offset the position.
     * @return A new Position with the offset applied.
     * @throws IllegalArgumentException If the direction is {@code null}.
     */
    public Position offsetBy(Direction direction) throws IllegalArgumentException {
        if (direction == null) {
            throw new IllegalArgumentException("Not a valid direction");
        }

        return switch (direction) {
            case UP -> new Position(x, y + 1);
            case DOWN -> new Position(x, y - 1);
            case LEFT -> new Position(x - 1, y);
            case RIGHT -> new Position(x + 1, y);
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
