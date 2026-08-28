package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.common.game.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a tile dropped on a square lands on the square somebody was aiming at.
 *
 * <p>This is the whole of drag and drop that can go wrong silently. A mapping that is one cell
 * out puts a component next to where it was meant to go, which is legal, which means the server
 * accepts it and the player finds out during validation — the same failure the text client's
 * coordinates were written to avoid, arriving by a different route.
 *
 * <p>No screen needed: it is arithmetic over a rectangle.
 *
 * <p>Components involved: {@link BoardGeometry}, {@link Position}.
 */
class BoardGeometryTest {

    /** The artwork's own size, where a pixel here is a pixel there. */
    private static final double W = 937;
    private static final double H = 679;

    private static final int ROWS = 5;
    private static final int COLUMNS = 7;

    private final BoardGeometry board = new BoardGeometry(W, H, ROWS, COLUMNS);

    @Test
    @DisplayName("the grid lines measured from the artwork are where the squares begin")
    void theMeasuredOrigin() {
        // The first dark column is at x=32 and the first dark row at y=27. A point just inside
        // that corner is the top-left square and nothing else.
        assertEquals(Optional.of(new Position(0, 0)), board.cellAt(33, 28));
        assertTrue(board.cellAt(31, 26).isEmpty(), "just outside is off the board");
    }

    @Test
    @DisplayName("every square maps to a point that maps back to it")
    void roundTrip() {
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                Position cell = new Position(row, column);
                double[] bounds = board.boundsOf(cell);
                double middleX = bounds[0] + bounds[2] / 2;
                double middleY = bounds[1] + bounds[3] / 2;

                assertEquals(Optional.of(cell), board.cellAt(middleX, middleY),
                        "the middle of " + cell + " is not " + cell);
            }
        }
    }

    @Test
    @DisplayName("neighbouring squares touch, with nothing between them to drop a tile into")
    void noGaps() {
        for (int column = 0; column + 1 < COLUMNS; column++) {
            double[] left = board.boundsOf(new Position(0, column));
            double[] right = board.boundsOf(new Position(0, column + 1));

            assertEquals(left[0] + left[2], right[0], 0.0001,
                    "a gap between columns " + column + " and " + (column + 1));
        }
        for (int row = 0; row + 1 < ROWS; row++) {
            double[] above = board.boundsOf(new Position(row, 0));
            double[] below = board.boundsOf(new Position(row + 1, 0));

            assertEquals(above[1] + above[3], below[1], 0.0001,
                    "a gap between rows " + row + " and " + (row + 1));
        }
    }

    @Test
    @DisplayName("the whole grid fits on the board it was measured from")
    void insideTheArtwork() {
        double[] last = board.boundsOf(new Position(ROWS - 1, COLUMNS - 1));

        assertTrue(last[0] + last[2] <= W, "the grid runs off the right edge");
        assertTrue(last[1] + last[3] <= H, "the grid runs off the bottom");
        // And it very nearly fills it: the last measured line is at x=901 of 937.
        assertEquals(901, last[0] + last[2], 1.0);
        assertEquals(647, last[1] + last[3], 1.0);
    }

    @Test
    @DisplayName("the mapping survives the board being drawn at any size")
    void scaling() {
        // A window is not 937 pixels wide. Half size, the same square is under half the point.
        BoardGeometry half = new BoardGeometry(W / 2, H / 2, ROWS, COLUMNS);
        Position cell = new Position(2, 3);
        double[] full = board.boundsOf(cell);

        assertEquals(Optional.of(cell), half.cellAt(full[0] / 2 + 1, full[1] / 2 + 1));
        assertEquals(full[2] / 2, half.cellWidth(), 0.0001);
    }

    @Test
    @DisplayName("a point in the margins is on no square rather than on the nearest one")
    void offTheGrid() {
        assertTrue(board.cellAt(0, 0).isEmpty(), "the top-left corner is board, not ship");
        assertTrue(board.cellAt(W - 1, H - 1).isEmpty(), "so is the bottom-right");
        assertTrue(board.cellAt(-5, 100).isEmpty());
        assertTrue(board.cellAt(100, -5).isEmpty());
    }

    @Test
    @DisplayName("a square that is not on the ship is refused rather than drawn off the edge")
    void offTheShip() {
        assertThrows(IllegalArgumentException.class,
                () -> board.boundsOf(new Position(ROWS, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> board.boundsOf(new Position(0, COLUMNS)));
        assertThrows(IllegalArgumentException.class,
                () -> board.boundsOf(new Position(-1, 0)));
    }

    @Test
    @DisplayName("a board with no size is refused, because every square would be at the origin")
    void nonsenseBoards() {
        assertThrows(IllegalArgumentException.class, () -> new BoardGeometry(0, H, ROWS, COLUMNS));
        assertThrows(IllegalArgumentException.class, () -> new BoardGeometry(W, 0, ROWS, COLUMNS));
        assertThrows(IllegalArgumentException.class, () -> new BoardGeometry(W, H, 0, COLUMNS));
        assertThrows(IllegalArgumentException.class, () -> new BoardGeometry(W, H, ROWS, 0));
    }
}
