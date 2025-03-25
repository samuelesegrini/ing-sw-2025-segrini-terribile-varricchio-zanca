package it.polimi.ingsw.model.domain.ship;

import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.HashMap;
import java.util.Map;

public class Grid<T> {
    private Map<Position, T> grid;
    private int rows;
    private int cols;

    public Grid() {
        this.grid = new HashMap<Position, T>();
        this.rows = 0;
        this.cols = 0;
    }
    public Map<Position, T> getGrid() {
        return this.grid;
    }
    public boolean containsKey(Position p){
        return this.grid.containsKey(p);
    }


    /**
     * Finds the first non-empty component along a line specified by a fixed coordinate.
     * <p>
     * For UP and DOWN directions, the fixed coordinate represents the column index.
     * The method iterates through the rows (starting from the top for UP or the bottom for DOWN)
     * until it finds a non-empty component in that column.
     * </p>
     * <p>
     * For LEFT and RIGHT directions, the fixed coordinate represents the row index.
     * The method iterates through the columns (starting from the left for LEFT or the right for RIGHT)
     * until it finds a non-empty component in that row.
     * </p>
     *
     * @param direction  The direction.
     * @param fixedIndex The column index if the direction is UP or DOWN, or the row index if the direction is LEFT or RIGHT.
     * @return A map containing the position and the component that is hit, or an empty map if no component is found.
     */
    public Position findFirstComponent(Direction direction, int fixedIndex) {
        Position result = null;

        if (direction == Direction.UP) {
            // For a shot from the top, fixedIndex is the column.
            // Iterate rows from top (0) to bottom.
            for (int row = 0; row < rows; row++) {
                Position pos = new Position(row, fixedIndex);
                if (grid.containsKey(pos)) {
                    result = pos;
                    return result;
                }
            }
        } else if (direction == Direction.DOWN) {
            // For a shot from the bottom, fixedIndex is the column.
            // Iterate rows from bottom to top.
            for (int row = rows - 1; row >= 0; row--) {
                Position pos = new Position(row, fixedIndex);
                if (grid.containsKey(pos)) {
                    result=pos;
                    return result;
                }
            }
        } else if (direction == Direction.LEFT) {
            // For a shot from the left, fixedIndex is the row.
            // Iterate columns from left (0) to right.
            for (int col = 0; col < cols; col++) {
                Position pos = new Position(fixedIndex, col);
                if (grid.containsKey(pos)) {
                    result=pos;
                    return result;
                }
            }
        } else if (direction == Direction.RIGHT) {
            // For a shot from the right, fixedIndex is the row.
            // Iterate columns from right to left.
            for (int col = cols - 1; col >= 0; col--) {
                Position pos = new Position(fixedIndex, col);
                if (grid.containsKey(pos)) {
                    result=pos;
                    return result;
                }
            }
        }
        throw new NullPointerException("No element found.");
    }

    public boolean protectedByShield(Direction direction){
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Position pos = new Position(row, col);
                if (grid.containsKey(pos) && grid.get(pos)==ComponentType.SHIELD &&
                        grid.get(pos).getProtectedDirections().contaiins(direction)){
                    System.out.println("Shield found, check battery presence");
                    return true;
                }
            }
        }
    }

    /**
     * Checks if there is a cannon protecting the specified row or column.
     * <p>
     * For the directions {@code UP} or {@code DOWN}, the method checks the entire column (fixedIndex).
     * For the directions {@code LEFT} or {@code RIGHT}, the method checks the specified row (fixedIndex)
     * as well as the adjacent rows.
     * <p>
     * The method returns {@code true} as soon as a single cannon is found.
     * If no single cannon is found but a double cannon is detected,
     * it prints "Double cannon found, check battery presence" and returns {@code true}.
     * If no cannon is found, it returns {@code false}.
     *
     * @param direction  the direction of the shot (UP, DOWN, LEFT, or RIGHT)
     * @param fixedIndex the fixed index representing the column (for UP/DOWN) or row (for LEFT/RIGHT)
     * @return {@code true} if a cannon (single or double) is found; {@code false} otherwise
     */
    public boolean protectedByCannon(Direction direction, int fixedIndex) {
        boolean foundSingle = false;
        boolean foundDouble = false;
        if ((direction == Direction.UP) || (direction == Direction.DOWN)) {
            for (int row = 0; row < rows && !foundSingle; row++) {
                Position pos = new Position(row, fixedIndex);
                if (grid.containsKey(pos) && (grid.get(pos) == ComponentType.CANNON_SINGLE)){
                    foundSingle = true;
                }
                else if (grid.containsKey(pos) && grid.get(pos) == ComponentType.CANNON_DOUBLE){
                    foundDouble = true;
                }
            }
        } else if ((direction == Direction.LEFT) || (direction == Direction.RIGHT)) {
            int[] rowsToCheck = {fixedIndex, fixedIndex - 1, fixedIndex + 1};
            for (int r : rowsToCheck) {
                if (r >= 0 && r < rows) {
                    for (int col = 0; col < cols && !foundSingle; col++) {
                        Position pos = new Position(r, col);
                        if (grid.containsKey(pos) && (grid.get(pos) == ComponentType.CANNON_SINGLE)) {
                            foundSingle = true;
                        }else if (grid.containsKey(pos) && grid.get(pos) == ComponentType.CANNON_DOUBLE) {
                            foundDouble = true;
                        }
                    }
                }
            }
        }

        if (foundSingle) { return true; }
        else if (foundDouble) {
           System.out.println("Double cannon found, check battery presence");
           return true;
        }
        else{ return false; }
    }
}
