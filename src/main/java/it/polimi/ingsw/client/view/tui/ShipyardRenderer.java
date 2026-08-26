package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.view.BuildingView;
import it.polimi.ingsw.common.protocol.view.TileView;

import java.util.ArrayList;
import java.util.List;

/**
 * The shipyard: the heap, the discard pile, the hourglass, and who has stopped building.
 *
 * <p>Almost everything here is public, and has to be. The face-up pile is what everybody else
 * threw away and anybody may take; how many players have finished is how much time is left in
 * practice; the free start spaces are what a player is racing for. A screen that showed only
 * your own ship would be a screen you could not play from.
 *
 * <p>Two things are yours alone — the tile in your hand and the cards you peeked at — and the
 * server builds this view per recipient so that they only ever reach you.
 */
public final class ShipyardRenderer {

    private ShipyardRenderer() {
    }

    /**
     * Draws the shipyard.
     *
     * @param yard what the server says is on the table
     * @return the lines to print
     */
    public static List<String> render(BuildingView yard) {
        List<String> lines = new ArrayList<>();
        lines.add("Shipyard    " + yard.faceDownRemaining() + " face down    " + hourglass(yard));
        lines.add("  face up:  " + (yard.faceUpPile().isEmpty()
                ? "nothing yet"
                : names(yard.faceUpPile())));
        lines.add("  finished: " + finished(yard)
                + "    free start spaces: " + spaces(yard));
        lines.addAll(hand(yard));
        lines.addAll(peeked(yard));
        return List.copyOf(lines);
    }

    /**
     * Says whether a tile is down but not yet fixed.
     *
     * <p>A board cannot show this by itself — a tile put down and a tile welded look the same
     * once they are in a square — and the difference is whether it can still be moved for
     * nothing or only thrown away.
     *
     * @param yard the shipyard
     * @param ship whose board, for the numbers printed along its edges
     * @return one line, or nothing when everything is fixed
     */
    public static List<String> loose(BuildingView yard,
                                     it.polimi.ingsw.common.protocol.view.ShipView ship) {
        return yard.unweldedIfAny()
                .map(cell -> List.of("  not welded yet: " + Coordinates.printed(ship, cell)
                        + " — 'turn' to move it, 'weld' to fix it"))
                .orElseGet(List::of);
    }

    /**
     * Draws the tile in a player's hand, with its connectors.
     *
     * <p>The connectors are the point. The board says what a component is; a player deciding
     * where to put one needs to know what it will join to.
     *
     * @param yard the shipyard, which carries this player's hand and nobody else's
     * @return a heading and three lines, or nothing when their hands are empty
     */
    public static List<String> hand(BuildingView yard) {
        return yard.handIfAny()
                .map(tile -> {
                    List<String> lines = new ArrayList<>();
                    lines.add("");
                    lines.add("in hand");
                    lines.addAll(TileRenderer.render(tile));
                    return List.copyOf(lines);
                })
                .orElseGet(List::of);
    }

    private static List<String> peeked(BuildingView yard) {
        if (yard.scouted().isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("the pile in your hands");
        yard.scouted().forEach(card -> lines.add("  " + describe(card)));
        return List.copyOf(lines);
    }

    private static String describe(AdventureCardIdentity card) {
        return words(card.type().name()) + "  (" + words(card.level().name()) + ")";
    }

    /**
     * Says where the hourglass is and how long is left.
     *
     * <p>Level II only. The test flight has no timer at all, and saying "hourglass 0/0" would be
     * inventing something to worry about.
     */
    private static String hourglass(BuildingView yard) {
        if (yard.hourglassSpaces() == 0) {
            return "no hourglass";
        }
        // The model counts spaces from zero and calls the state before building -1. A player
        // reading a board sees three spaces and the sand in the first of them.
        String where = yard.hourglassSpace() == null || yard.hourglassSpace() < 0
                ? "not started"
                : (yard.hourglassSpace() + 1) + "/" + yard.hourglassSpaces();
        return "hourglass " + where
                + (yard.secondsRemaining() > 0 ? ", " + yard.secondsRemaining() + "s left" : "");
    }

    private static String finished(BuildingView yard) {
        return yard.finished().isEmpty() ? "nobody" : yard.finished().stream()
                .map(PlayerColor::name)
                .sorted()
                .reduce((left, right) -> left + " " + right)
                .orElse("nobody");
    }

    private static String spaces(BuildingView yard) {
        return yard.freeStartSpaces().isEmpty() ? "none" : yard.freeStartSpaces().stream()
                .map(String::valueOf)
                .reduce((left, right) -> left + " " + right)
                .orElse("none");
    }

    private static String names(List<TileView> tiles) {
        return tiles.stream()
                .map(TileView::tileId)
                .reduce((left, right) -> left + "  " + right)
                .orElse("");
    }

    private static String words(String constant) {
        return constant.toLowerCase().replace('_', ' ');
    }
}
