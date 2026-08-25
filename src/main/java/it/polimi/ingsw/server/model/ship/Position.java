package it.polimi.ingsw.server.model.ship;

/**
 * A cell of a ship board, indexed from the top left corner.
 *
 * <p>These are internal indices. The boards print different numbers along their edges
 * — rows 5 to 9 and columns 4 to 10 — and those printed labels are what the dice
 * address. The translation lives in the board specification, not here, so that a
 * position stays a plain pair of indices.
 *
 * @param row    the row index, 0 at the top
 * @param column the column index, 0 at the left
 */
public record Position(int row, int column) {

    /**
     * Returns the cell immediately next to this one in the given direction.
     *
     * <p>The result may lie outside the board; callers check that against the board
     * specification.
     *
     * @param direction the side to step across, never {@code null}
     * @return the neighbouring cell
     */
    public Position neighbour(Direction direction) {
        return switch (direction) {
            case NORTH -> new Position(row - 1, column);
            case SOUTH -> new Position(row + 1, column);
            case WEST -> new Position(row, column - 1);
            case EAST -> new Position(row, column + 1);
        };
    }
}
