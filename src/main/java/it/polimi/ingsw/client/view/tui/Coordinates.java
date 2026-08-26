package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.protocol.view.ShipView;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Translating between the numbers on the board and the numbers in the model.
 *
 * <p>A player types what is printed along the edge — rows 5 to 9, columns 4 to 10 — because
 * that is what they can see, and it is what a meteor roll names. The model counts from zero.
 * Somebody has to subtract, and doing it here once is better than doing it at every command.
 *
 * <p>Getting this backwards is not a crash. It is a tile welded three squares from where the
 * player meant, discovered during validation, and paid for with a component.
 */
public final class Coordinates {

    private Coordinates() {
    }

    /**
     * Turns a printed row and column into a cell.
     *
     * @param ship   whose board, for the numbers printed on it
     * @param row    the row as printed
     * @param column the column as printed
     * @return the cell, or empty if those numbers are not on this board
     */
    public static Optional<Position> on(ShipView ship, OptionalInt row, OptionalInt column) {
        if (row.isEmpty() || column.isEmpty()) {
            return Optional.empty();
        }
        int down = row.getAsInt() - ship.firstPrintedRow();
        int across = column.getAsInt() - ship.firstPrintedColumn();
        if (down < 0 || down >= ship.rows() || across < 0 || across >= ship.columns()) {
            return Optional.empty();
        }
        return Optional.of(new Position(down, across));
    }

    /**
     * Says where a cell is, the way the board is labelled.
     *
     * @param ship whose board
     * @param cell which square
     * @return the printed row and column, as a player would say them
     */
    public static String printed(ShipView ship, Position cell) {
        return ship.printedRow(cell.row()) + "," + ship.printedColumn(cell.column());
    }

    /**
     * Says which squares a player could have meant.
     *
     * @param ship whose board
     * @return the range of printed numbers, for when somebody names one that is not there
     */
    public static String range(ShipView ship) {
        return "rows " + ship.firstPrintedRow() + "-" + ship.printedRow(ship.rows() - 1)
                + ", columns " + ship.firstPrintedColumn() + "-"
                + ship.printedColumn(ship.columns() - 1);
    }
}
