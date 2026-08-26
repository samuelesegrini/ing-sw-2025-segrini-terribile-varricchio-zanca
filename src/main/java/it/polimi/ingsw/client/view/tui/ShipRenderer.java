package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.ShipViolation;
import it.polimi.ingsw.common.protocol.view.CellView;
import it.polimi.ingsw.common.protocol.view.ShipView;
import it.polimi.ingsw.common.protocol.view.TileView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A ship, drawn as the board it is.
 *
 * <p>Rows and columns are labelled with the numbers <em>printed on the board</em> — 5 to 9 and
 * 4 to 10 — rather than with indices. Those printed numbers are what a meteor roll names, so a
 * player who has just been told a seven is coming from the north has to be able to find column
 * seven without counting.
 *
 * <p>Everything here is a pure function of a {@link ShipView}. There is nothing to construct,
 * nothing to keep, and a test can hand it a projection and compare the lines.
 */
public final class ShipRenderer {

    private static final String LABEL_GUTTER = "    ";

    private ShipRenderer() {
    }

    /**
     * Draws a ship.
     *
     * @param ship what to draw
     * @return the lines, top row first, with a header of column numbers
     */
    public static List<String> render(ShipView ship) {
        List<String> lines = new ArrayList<>();
        lines.add(columnHeader(ship));
        for (int row = 0; row < ship.rows(); row++) {
            lines.add(row(ship, row));
        }
        return List.copyOf(lines);
    }

    /**
     * Draws a ship with everything worth knowing about it underneath.
     *
     * @param ship  what to draw
     * @param title what to call it — a player's name, usually
     * @return the board, then its attributes, then what is wrong with it
     */
    public static List<String> describe(ShipView ship, String title) {
        List<String> lines = new ArrayList<>();
        lines.add(title);
        lines.addAll(render(ship));
        lines.add("");
        lines.add(attributes(ship));
        if (!ship.reserved().isEmpty()) {
            lines.add("  set aside: " + ship.reserved().stream()
                    .map(TileView::tileId)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse(""));
        }
        lines.addAll(problems(ship));
        return List.copyOf(lines);
    }

    /**
     * Sums up what a ship can do.
     *
     * @param ship the ship
     * @return one line of firepower, engine power, crew and losses
     */
    public static String attributes(ShipView ship) {
        return "  firepower " + halves(ship.attributes().firepowerHalves())
                + "   engines " + ship.attributes().enginePower()
                + "   crew " + ship.attributes().crew()
                + "   lost " + ship.lostComponents();
    }

    /**
     * Writes a firepower in halves the way the manual does.
     *
     * <p>Never rounded. Four and a half loses to five and five and a half beats it, and a
     * display that showed both as "5" would be lying about the one number in this game that
     * decides fights.
     *
     * @param halves the count of halves
     * @return the number, with a half if there is one
     */
    public static String halves(int halves) {
        String whole = String.valueOf(halves / 2);
        return halves % 2 == 0 ? whole : whole + "½";
    }

    /**
     * Lists what is wrong with a ship.
     *
     * @param ship the ship
     * @return one line per violation, or nothing when there is nothing wrong
     */
    public static List<String> problems(ShipView ship) {
        if (ship.validation().isLegal()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        lines.add("  this ship cannot fly:");
        for (ShipViolation violation : ship.validation().violations()) {
            lines.add("    " + violation.description() + "  at " + cellsOf(ship, violation));
        }
        return List.copyOf(lines);
    }

    private static String cellsOf(ShipView ship, ShipViolation violation) {
        return violation.cells().stream()
                .map(cell -> ship.printedRow(cell.row()) + "," + ship.printedColumn(cell.column()))
                .reduce((left, right) -> left + " " + right)
                .orElse("nowhere");
    }

    private static String columnHeader(ShipView ship) {
        StringBuilder header = new StringBuilder(LABEL_GUTTER);
        for (int column = 0; column < ship.columns(); column++) {
            header.append(centred(String.valueOf(ship.printedColumn(column))));
        }
        return header.toString().stripTrailing();
    }

    private static String row(ShipView ship, int row) {
        StringBuilder line = new StringBuilder();
        line.append(String.format("%3d ", ship.printedRow(row)));
        for (int column = 0; column < ship.columns(); column++) {
            Position cell = new Position(row, column);
            Optional<CellView> welded = Optional.ofNullable(ship.cells().get(cell));
            String glyph = welded.map(Glyphs::of)
                    .orElse(ship.outline().contains(cell) ? Glyphs.EMPTY : Glyphs.OFF_BOARD);
            line.append(glyph).append(' ');
        }
        return line.toString().stripTrailing();
    }

    private static String centred(String label) {
        int padding = Glyphs.CELL_WIDTH + 1 - label.length();
        int left = padding / 2;
        return " ".repeat(left) + label + " ".repeat(padding - left);
    }
}
