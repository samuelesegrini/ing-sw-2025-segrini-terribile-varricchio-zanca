package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.common.protocol.view.TileView;

import java.util.List;

/**
 * One tile, with its connectors where a player can see them.
 *
 * <p>The board shows what a component <em>is</em>. This shows what it will <em>join to</em>,
 * which is the thing a player actually needs when deciding where to put the tile in their hand.
 * Guessing wrong is not fatal — the manual lets anybody build an illegal ship — but it costs a
 * component during validation, so it is worth three lines.
 *
 * <pre>
 *      U
 *    1 C2 2      cabin_UUUU · cabin · turned 90°
 *      .
 * </pre>
 */
public final class TileRenderer {

    private TileRenderer() {
    }

    /**
     * Draws a tile and what each of its sides offers.
     *
     * @param tile     what to draw
     * @param occupant the two characters to show in the middle, usually from the board
     * @return three lines
     */
    public static List<String> render(TileView tile, String occupant) {
        char north = Glyphs.of(tile.connectors().get(Direction.NORTH));
        char east = Glyphs.of(tile.connectors().get(Direction.EAST));
        char south = Glyphs.of(tile.connectors().get(Direction.SOUTH));
        char west = Glyphs.of(tile.connectors().get(Direction.WEST));

        return List.of(
                "     " + north,
                "   " + west + " " + occupant + " " + east + "      " + describe(tile),
                "     " + south);
    }

    /**
     * Draws a tile nobody has welded yet.
     *
     * @param tile what to draw
     * @return three lines
     */
    public static List<String> render(TileView tile) {
        return render(tile, Glyphs.of(tile).strip().isEmpty() ? "??" : Glyphs.of(tile).substring(0, 2));
    }

    /**
     * Names a tile in a way a player can type back.
     *
     * @param tile what to name
     * @return its identifier, what it is, and how far it has been turned
     */
    public static String describe(TileView tile) {
        String turned = tile.rotation() == Rotation.NONE
                ? "upright"
                : "turned " + tile.rotation().quarterTurns() * 90 + "°";
        return tile.tileId() + " · " + Glyphs.nameOf(tile.kind()) + " · " + turned;
    }
}
