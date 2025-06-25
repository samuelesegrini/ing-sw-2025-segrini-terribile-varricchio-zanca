package it.polimi.ingsw.server.model.domain.ship;

import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.io.Serializable;
import java.util.Objects;

public class Position implements Serializable {
    private final static long serialVersionUID = 1L;

    private final int row;
    private final int col;


    /**
     * Constructor defined by user.
     * @param row row coordinate (y-coordinate).
     * @param col column coordinate (x-coordinate).
     */
    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }


    /**
     * Returns the row (y-coordinate).
     * @return The row (y-coordinate).
     */
    public int getRow() {
        return  row;
    }

    /**
     * Returns the column (x-coordinate).
     * @return The column (x-coordinate).
     */
    public int getCol() {
        return col;
    }

    /**
     * Returns a new Position that is offset from the current position in the specified direction.
     *
     * In our coordinate system:
     * - The origin (0,0) is in the top-left corner of the board
     * - row increases as we move down
     * - col increases as we move right

     * Therefore:
     * - UP -> row-1
     * - DOWN: row+1
     * - LEFT: col-1
     * - RIGHT: col+1
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
            case UP -> new Position(row - 1, col);
            case DOWN -> new Position(row + 1, col);
            case LEFT -> new Position(row, col - 1);
            case RIGHT -> new Position(row, col + 1);
        };
    }

    /**
     * Returns a hash code value for the Position.
     * @return The hash code of the Position.
     */
    public int hashCode() {
        // Combines col, row in a single and unique value that contains both the information about col and row
        return Objects.hash(row, col);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Position other = (Position) obj;
        if (row != other.row)
            return false;
        if (col != other.col)
            return false;
        return true;
    }
}
