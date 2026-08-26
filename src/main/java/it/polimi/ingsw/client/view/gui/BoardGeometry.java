package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.common.game.Position;

import java.util.Optional;

/**
 * Where a square of the ship sits on the picture of the board.
 *
 * <p>The board is a photograph of a printed thing, with a grid already on it. Nothing here
 * draws that grid; it works out where the printed one is, so that a tile dropped on a square
 * lands on the square a player was aiming at rather than near it.
 *
 * <p>The numbers were measured from the artwork rather than guessed: the grid lines are the
 * darkest columns and rows in the image, and they come out evenly spaced at a pitch of about
 * 124 pixels from an origin of (32, 27) on a 937 by 679 board. They are kept as fractions of
 * the image so that the mapping survives the board being drawn at any size — which it will be,
 * because a window is not 937 pixels wide.
 *
 * <p>Both levels are the same five by seven grid with the same starting cabin; they differ in
 * which squares are forbidden and how many tiles may be set aside, neither of which moves a
 * line. So there is one geometry and not one per level.
 */
public final class BoardGeometry {

    /** The artwork these fractions were measured from. */
    private static final double MEASURED_WIDTH = 937;
    private static final double MEASURED_HEIGHT = 679;

    /** The first grid line, across and down. */
    private static final double ORIGIN_X = 32 / MEASURED_WIDTH;
    private static final double ORIGIN_Y = 27 / MEASURED_HEIGHT;

    /** The distance between grid lines: 869 pixels over seven columns, 620 over five rows. */
    private static final double CELL_WIDTH = (869.0 / 7) / MEASURED_WIDTH;
    private static final double CELL_HEIGHT = (620.0 / 5) / MEASURED_HEIGHT;

    private final double width;
    private final double height;
    private final int rows;
    private final int columns;

    /**
     * Fits the grid to a board drawn at a particular size.
     *
     * @param width   how wide the board is on screen
     * @param height  how tall
     * @param rows    how many rows the ship has
     * @param columns how many columns
     * @throws IllegalArgumentException if the board has no size, or the ship no squares
     */
    public BoardGeometry(double width, double height, int rows, int columns) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("a board with no size has no squares on it");
        }
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("a ship needs at least one square");
        }
        this.width = width;
        this.height = height;
        this.rows = rows;
        this.columns = columns;
    }

    /**
     * Returns the square under a point.
     *
     * @param x across, from the left of the board
     * @param y down, from the top
     * @return the square, or empty when the point is off the grid — which is most of a window
     */
    public Optional<Position> cellAt(double x, double y) {
        double column = (x - originX()) / cellWidth();
        double row = (y - originY()) / cellHeight();
        if (column < 0 || row < 0 || column >= columns || row >= rows) {
            return Optional.empty();
        }
        return Optional.of(new Position((int) row, (int) column));
    }

    /**
     * Returns where a square is on the board.
     *
     * @param cell the square
     * @return its left edge, top edge, width and height, in that order
     * @throws IllegalArgumentException if the square is not on this ship
     */
    public double[] boundsOf(Position cell) {
        if (cell.row() < 0 || cell.row() >= rows
                || cell.column() < 0 || cell.column() >= columns) {
            throw new IllegalArgumentException(cell + " is not a square on this ship");
        }
        return new double[] {
                originX() + cell.column() * cellWidth(),
                originY() + cell.row() * cellHeight(),
                cellWidth(),
                cellHeight()
        };
    }

    /**
     * How wide a tile is drawn.
     *
     * @return the width of one square on screen
     */
    public double cellWidth() {
        return width * CELL_WIDTH;
    }

    /**
     * How tall a tile is drawn.
     *
     * @return the height of one square on screen
     */
    public double cellHeight() {
        return height * CELL_HEIGHT;
    }

    private double originX() {
        return width * ORIGIN_X;
    }

    private double originY() {
        return height * ORIGIN_Y;
    }
}
