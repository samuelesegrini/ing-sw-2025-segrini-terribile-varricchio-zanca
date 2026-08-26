package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.common.protocol.view.CellView;
import it.polimi.ingsw.common.protocol.view.TileView;

/**
 * Three characters for every square of a ship, chosen so a player can read a board at a glance.
 *
 * <p>Three, because two is not enough to say both what a component is and what is in it, and
 * four makes a seven-column board wider than a comfortable terminal. Within three there is room
 * for a letter, a state, and — where it decides anything — a direction.
 *
 * <p>The letters are picked to be unambiguous rather than mnemonic. A cabin and a double cannon
 * would both want {@code C}, so cannons are {@code x} and {@code X}; a cargo hold and a shield
 * would both want {@code s}, so holds are {@code g} and {@code G}. Guessing wrong about a
 * square during the building phase costs a component.
 */
public final class Glyphs {

    /** How wide every square is. Anything narrower cannot say what is inside a cabin. */
    public static final int CELL_WIDTH = 3;

    /** A square that may be built on but is empty. */
    public static final String EMPTY = " . ";

    /** A square that is not part of the printed board at all. */
    public static final String OFF_BOARD = "   ";

    private Glyphs() {
    }

    /**
     * Describes one occupied square.
     *
     * @param cell what is welded there and what is in it
     * @return exactly {@value #CELL_WIDTH} characters
     */
    public static String of(CellView cell) {
        TileView tile = cell.tile();
        return pad(switch (tile.kind()) {
            case STRUCTURAL_MODULE -> "###";
            case STARTING_CABIN -> "@" + occupancy(cell);
            case CABIN -> "C" + occupancy(cell);
            case BATTERY -> "B" + (cell.batteries() == 0 ? "." : String.valueOf(cell.batteries()));
            case CARGO_HOLD -> "g" + cargo(cell);
            case SPECIAL_CARGO_HOLD -> "G" + cargo(cell);
            case SHIELD -> "s" + sides(tile.rotation());
            case PURPLE_LIFE_SUPPORT -> "LP";
            case BROWN_LIFE_SUPPORT -> "LB";
            case SINGLE_CANNON -> "x" + arrow(Direction.NORTH.rotatedBy(tile.rotation()));
            case DOUBLE_CANNON -> "X" + arrow(Direction.NORTH.rotatedBy(tile.rotation()));
            case SINGLE_ENGINE -> "e" + arrow(Direction.SOUTH.rotatedBy(tile.rotation()));
            case DOUBLE_ENGINE -> "E" + arrow(Direction.SOUTH.rotatedBy(tile.rotation()));
        });
    }

    /**
     * Names a component in words, for a legend or an inspection.
     *
     * @param kind what it is
     * @return its name, in lower case
     */
    public static String nameOf(ComponentKind kind) {
        return kind.name().toLowerCase().replace('_', ' ');
    }

    /**
     * Describes one connector.
     *
     * @param connector what a side offers
     * @return a single character: {@code U}, {@code 1}, {@code 2} or {@code .}
     */
    public static char of(Connector connector) {
        return switch (connector) {
            case UNIVERSAL -> 'U';
            case SINGLE -> '1';
            case DOUBLE -> '2';
            case PLAIN -> '.';
        };
    }

    /**
     * Describes a direction as an arrow.
     *
     * @param direction which way
     * @return one of {@code ^ > v <}
     */
    public static char arrow(Direction direction) {
        return switch (direction) {
            case NORTH -> '^';
            case EAST -> '>';
            case SOUTH -> 'v';
            case WEST -> '<';
        };
    }

    /**
     * Abbreviates a player's colour to one letter.
     *
     * @param colour whose markers
     * @return the first letter of the colour
     */
    public static char of(PlayerColor colour) {
        return colour.name().charAt(0);
    }

    /**
     * Abbreviates a cube's colour to one letter.
     *
     * @param colour which cube
     * @return the first letter of the colour
     */
    public static char of(GoodColor colour) {
        return colour.name().charAt(0);
    }

    private static String occupancy(CellView cell) {
        return cell.alienIfAny()
                .map(alien -> String.valueOf(alien == AlienColor.PURPLE ? 'P' : 'B'))
                .orElseGet(() -> cell.humans() == 0 ? "." : String.valueOf(cell.humans()));
    }

    private static String cargo(CellView cell) {
        return cell.cargo().isEmpty() ? "." : String.valueOf(cell.cargo().size());
    }

    /**
     * Names the two sides a shield covers.
     *
     * <p>A shield protects the two sides drawn on it, which are north and east before it is
     * turned. Which two it ends up covering is the only thing that decides whether it can stop
     * a particular shot, so it has to be legible from the board rather than by inspecting.
     */
    private static String sides(Rotation rotation) {
        Direction first = Direction.NORTH.rotatedBy(rotation);
        Direction second = Direction.EAST.rotatedBy(rotation);
        return "" + first.name().charAt(0) + second.name().charAt(0);
    }

    private static String pad(String glyph) {
        return glyph.length() >= CELL_WIDTH ? glyph.substring(0, CELL_WIDTH)
                : glyph + " ".repeat(CELL_WIDTH - glyph.length());
    }
}
