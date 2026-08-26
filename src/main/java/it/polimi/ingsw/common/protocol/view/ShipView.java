package it.polimi.ingsw.common.protocol.view;

import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.ShipAttributes;
import it.polimi.ingsw.common.game.ValidationReport;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A ship, as everyone at the table can see it.
 *
 * <p>Ships are public in this game: requirement G6, and the manual, both have players
 * looking at each other's work. So there is one ship view and every player gets the same
 * one, which removes a whole class of bug where a projection leaks by being built
 * differently for different recipients.
 *
 * <p>The outline is sent because a view cannot otherwise tell an empty buildable square
 * from a hole in the printed board, and the difference decides where a tile may go.
 *
 * <p>The printed offsets are here because the board's own edges are numbered — rows 5 to 9,
 * columns 4 to 10 — and those printed numbers are what the dice address. A view that only knew
 * the indices could draw a grid nobody could read a meteor roll against.
 *
 * @param rows            how tall the board is
 * @param columns         how wide
 * @param firstPrintedRow what the top row is called along the edge of the board
 * @param firstPrintedColumn what the leftmost column is called
 * @param outline         the cells that may hold a component; everything else is off the board
 * @param cells           what is welded where
 * @param reserved        tiles set aside and not welded, which count as lost at the end (p.7)
 * @param lostComponents  how many components this ship has already lost
 * @param attributes      firepower, engine power and crew with no double component powered
 * @param validation      what is wrong with the ship, empty when nothing is
 */
public record ShipView(int rows, int columns, int firstPrintedRow, int firstPrintedColumn,
                       Set<Position> outline, Map<Position, CellView> cells,
                       List<TileView> reserved, int lostComponents, ShipAttributes attributes,
                       ValidationReport validation) implements Serializable {

    /**
     * Takes defensive copies of the three collections.
     *
     * @throws NullPointerException     if the attributes or the report are {@code null}
     * @throws IllegalArgumentException if the board has no size, or a cell sits off the outline
     */
    public ShipView {
        if (attributes == null || validation == null) {
            throw new NullPointerException("a ship view needs its attributes and its report");
        }
        if (rows < 1 || columns < 1) {
            throw new IllegalArgumentException("a ship board needs a positive size, got " + rows + "x" + columns);
        }
        if (lostComponents < 0) {
            throw new IllegalArgumentException("a ship cannot have lost " + lostComponents + " components");
        }
        outline = Set.copyOf(outline);
        cells = Map.copyOf(cells);
        reserved = List.copyOf(reserved);
        if (!outline.containsAll(cells.keySet())) {
            throw new IllegalArgumentException("this ship has components welded outside its own outline");
        }
    }

    /**
     * Returns what a row is called along the edge of the board.
     *
     * @param row the row index, 0 at the top
     * @return the printed number
     */
    public int printedRow(int row) {
        return firstPrintedRow + row;
    }

    /**
     * Returns what a column is called along the edge of the board.
     *
     * @param column the column index, 0 at the left
     * @return the printed number
     */
    public int printedColumn(int column) {
        return firstPrintedColumn + column;
    }
}
