package it.polimi.ingsw.server.model.board;

import it.polimi.ingsw.server.model.ship.Position;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The outline of a ship board: which cells exist and how the dice address them.
 *
 * <p>Both boards are five rows by seven columns, but the outline differs — the test
 * flight board leaves 18 cells usable and the level II board 27. Cells outside the
 * outline can never hold anything.
 *
 * <p>The numbers printed along the edges are what threats are rolled against: columns
 * run 4 to 10 and rows 5 to 9, so a roll of two dice can name a line that does not
 * exist, and that is a clean miss.
 *
 * @param rows                the number of rows
 * @param columns             the number of columns
 * @param firstPrintedRow     the label printed beside row 0
 * @param firstPrintedColumn  the label printed above column 0
 * @param startingCabin       where the starting cabin is fixed at setup
 * @param reservationSlots    how many components may be set aside, zero when reserving is not allowed
 * @param forbidden           the cells outside the ship outline
 */
public record ShipBoardSpec(int rows,
                            int columns,
                            int firstPrintedRow,
                            int firstPrintedColumn,
                            Position startingCabin,
                            int reservationSlots,
                            Set<Position> forbidden) {

    /**
     * Validates the outline and takes a defensive copy of the forbidden cells.
     *
     * @throws IllegalArgumentException if the grid is empty, a forbidden cell lies off
     *                                  the grid, or the starting cabin is not usable
     */
    public ShipBoardSpec {
        if (rows < 1 || columns < 1) {
            throw new IllegalArgumentException("a ship board needs a positive size, got " + rows + "x" + columns);
        }
        Set<Position> off = new LinkedHashSet<>(forbidden);
        for (Position cell : off) {
            if (!onGrid(cell, rows, columns)) {
                throw new IllegalArgumentException("forbidden cell " + cell + " is off a " + rows + "x" + columns + " grid");
            }
        }
        forbidden = Set.copyOf(off);
        if (!onGrid(startingCabin, rows, columns) || forbidden.contains(startingCabin)) {
            throw new IllegalArgumentException("the starting cabin at " + startingCabin + " is not a usable cell");
        }
        if (reservationSlots < 0) {
            throw new IllegalArgumentException("reservation slots cannot be negative, got " + reservationSlots);
        }
    }

    private static boolean onGrid(Position cell, int rows, int columns) {
        return cell.row() >= 0 && cell.row() < rows && cell.column() >= 0 && cell.column() < columns;
    }

    /**
     * Tells whether a component may ever occupy the given cell.
     *
     * @param cell the cell to test
     * @return {@code true} when the cell is on the grid and inside the ship outline
     */
    public boolean isUsable(Position cell) {
        return onGrid(cell, rows, columns) && !forbidden.contains(cell);
    }

    /**
     * Returns every cell inside the ship outline, top to bottom then left to right.
     *
     * @return the usable cells in reading order
     */
    public List<Position> usableCells() {
        return java.util.stream.IntStream.range(0, rows)
                .boxed()
                .flatMap(row -> java.util.stream.IntStream.range(0, columns)
                        .mapToObj(column -> new Position(row, column)))
                .filter(this::isUsable)
                .toList();
    }

    /**
     * Returns the row index a roll of two dice names.
     *
     * @param diceSum the sum of two dice, between 2 and 12
     * @return the row index, or empty when the roll names no row on this board
     */
    public OptionalInt rowForDiceSum(int diceSum) {
        int row = diceSum - firstPrintedRow;
        return row >= 0 && row < rows ? OptionalInt.of(row) : OptionalInt.empty();
    }

    /**
     * Returns the column index a roll of two dice names.
     *
     * @param diceSum the sum of two dice, between 2 and 12
     * @return the column index, or empty when the roll names no column on this board
     */
    public OptionalInt columnForDiceSum(int diceSum) {
        int column = diceSum - firstPrintedColumn;
        return column >= 0 && column < columns ? OptionalInt.of(column) : OptionalInt.empty();
    }

    /**
     * Returns the row label printed beside the given row.
     *
     * @param row the row index
     * @return the number printed on the board
     */
    public int printedRow(int row) {
        return row + firstPrintedRow;
    }

    /**
     * Returns the column label printed above the given column.
     *
     * @param column the column index
     * @return the number printed on the board
     */
    public int printedColumn(int column) {
        return column + firstPrintedColumn;
    }

    /**
     * Tells whether components may be set aside on this board.
     *
     * @return {@code true} when the level allows reserving
     */
    public boolean allowsReservation() {
        return reservationSlots > 0;
    }
}
