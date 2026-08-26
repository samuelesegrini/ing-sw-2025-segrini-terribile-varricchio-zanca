package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.common.game.ShipAttributes;
import it.polimi.ingsw.common.game.ShipViolation;
import it.polimi.ingsw.common.game.ValidationReport;
import it.polimi.ingsw.common.game.ViolationKind;
import it.polimi.ingsw.common.protocol.view.CellView;
import it.polimi.ingsw.common.protocol.view.ShipView;
import it.polimi.ingsw.common.protocol.view.TileView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a board reads the way a player needs it to.
 *
 * <p>Pure functions of a projection, so these are ordinary tests with no terminal, no client
 * and no server in them. That is the point of the split — a renderer that needed a running game
 * to test would be a renderer nobody tested.
 *
 * <p>Two things here are not cosmetic. The labels along the edges have to be the numbers
 * <em>printed on the board</em>, because that is what a meteor roll names. And firepower has to
 * be shown in halves, because it is the one number in this game that decides fights and a
 * display that rounded it would be lying.
 */
class ShipRendererTest {

    private static final Position CABIN = new Position(2, 3);

    private static TileView tile(ComponentKind kind, Rotation rotation) {
        Map<Direction, Connector> connectors = new EnumMap<>(Direction.class);
        connectors.put(Direction.NORTH, Connector.UNIVERSAL);
        connectors.put(Direction.EAST, Connector.SINGLE);
        connectors.put(Direction.SOUTH, Connector.DOUBLE);
        connectors.put(Direction.WEST, Connector.PLAIN);
        return new TileView(kind.name().toLowerCase(), kind, rotation, connectors);
    }

    private static ShipView shipWith(Map<Position, CellView> cells) {
        Set<Position> outline = new java.util.LinkedHashSet<>();
        for (int row = 0; row < 5; row++) {
            for (int column = 0; column < 7; column++) {
                outline.add(new Position(row, column));
            }
        }
        return new ShipView(5, 7, 5, 4, outline, cells, List.of(), 0,
                new ShipAttributes(0, 0, 0), ValidationReport.legal());
    }

    @Nested
    @DisplayName("the board")
    class Board {

        @Test
        @DisplayName("the edges are numbered the way the board is printed, not the way it is indexed")
        void printedLabels() {
            List<String> lines = ShipRenderer.render(shipWith(Map.of()));

            assertEquals("     4   5   6   7   8   9   10", lines.get(0));
            assertTrue(lines.get(1).startsWith("  5 "), "the top row is row five on the board");
            assertTrue(lines.get(5).startsWith("  9 "), "and the bottom is row nine");
        }

        @Test
        @DisplayName("an empty square you may build on is not the same as a hole in the board")
        void outlineVersusEmpty() {
            ShipView holed = new ShipView(1, 3, 5, 4,
                    Set.of(new Position(0, 0), new Position(0, 2)), Map.of(), List.of(), 0,
                    new ShipAttributes(0, 0, 0), ValidationReport.legal());

            String row = ShipRenderer.render(holed).get(1);

            assertEquals("  5  .       .", row);
            assertEquals(Glyphs.EMPTY, row.substring(4, 7), "the first square may be built on");
            assertEquals(Glyphs.OFF_BOARD, row.substring(8, 11),
                    "the middle one is a hole in the printed board, and has to look like one");
        }

        @Test
        @DisplayName("a cabin shows who is in it")
        void cabins() {
            assertEquals("@2 ", Glyphs.of(new CellView(
                    tile(ComponentKind.STARTING_CABIN, Rotation.NONE), 0, List.of(), 2, null)));
            assertEquals("C. ", Glyphs.of(new CellView(
                    tile(ComponentKind.CABIN, Rotation.NONE), 0, List.of(), 0, null)));
            assertEquals("CP ", Glyphs.of(new CellView(
                    tile(ComponentKind.CABIN, Rotation.NONE), 0, List.of(), 0, AlienColor.PURPLE)));
            assertEquals("CB ", Glyphs.of(new CellView(
                    tile(ComponentKind.CABIN, Rotation.NONE), 0, List.of(), 0, AlienColor.BROWN)));
        }

        @Test
        @DisplayName("a battery shows its charges and a hold its cubes")
        void contents() {
            assertEquals("B3 ", Glyphs.of(new CellView(
                    tile(ComponentKind.BATTERY, Rotation.NONE), 3, List.of(), 0, null)));
            assertEquals("B. ", Glyphs.of(new CellView(
                    tile(ComponentKind.BATTERY, Rotation.NONE), 0, List.of(), 0, null)));
            assertEquals("g2 ", Glyphs.of(new CellView(
                    tile(ComponentKind.CARGO_HOLD, Rotation.NONE), 0,
                    List.of(GoodColor.BLUE, GoodColor.GREEN), 0, null)));
            assertEquals("G1 ", Glyphs.of(new CellView(
                    tile(ComponentKind.SPECIAL_CARGO_HOLD, Rotation.NONE), 0,
                    List.of(GoodColor.RED), 0, null)));
        }

        @Test
        @DisplayName("a cannon and an engine point somewhere, and the board says where")
        void directions() {
            assertEquals("x^ ", Glyphs.of(new CellView(
                    tile(ComponentKind.SINGLE_CANNON, Rotation.NONE), 0, List.of(), 0, null)));
            assertEquals("X> ", Glyphs.of(new CellView(
                    tile(ComponentKind.DOUBLE_CANNON, Rotation.CLOCKWISE_90), 0, List.of(), 0, null)));
            assertEquals("ev ", Glyphs.of(new CellView(
                    tile(ComponentKind.SINGLE_ENGINE, Rotation.NONE), 0, List.of(), 0, null)),
                    "an engine exhausts towards the stern");
            assertEquals("E^ ", Glyphs.of(new CellView(
                    tile(ComponentKind.DOUBLE_ENGINE, Rotation.CLOCKWISE_180), 0, List.of(), 0, null)));
        }

        @Test
        @DisplayName("a shield says which two sides it covers, because that is what decides a hit")
        void shields() {
            assertEquals("sNE", Glyphs.of(new CellView(
                    tile(ComponentKind.SHIELD, Rotation.NONE), 0, List.of(), 0, null)));
            assertEquals("sES", Glyphs.of(new CellView(
                    tile(ComponentKind.SHIELD, Rotation.CLOCKWISE_90), 0, List.of(), 0, null)));
            assertEquals("sWN", Glyphs.of(new CellView(
                    tile(ComponentKind.SHIELD, Rotation.CLOCKWISE_270), 0, List.of(), 0, null)));
        }

        @Test
        @DisplayName("every kind of component fits in the space it is given")
        void everythingFits() {
            for (ComponentKind kind : ComponentKind.values()) {
                String glyph = Glyphs.of(new CellView(tile(kind, Rotation.NONE), 0, List.of(), 0, null));
                assertEquals(Glyphs.CELL_WIDTH, glyph.length(), kind + " does not fit");
            }
            assertEquals(Glyphs.CELL_WIDTH, Glyphs.EMPTY.length());
            assertEquals(Glyphs.CELL_WIDTH, Glyphs.OFF_BOARD.length());
        }
    }

    @Nested
    @DisplayName("what the board does not show")
    class Underneath {

        @Test
        @DisplayName("firepower is never rounded")
        void halves() {
            assertEquals("0", ShipRenderer.halves(0));
            assertEquals("2", ShipRenderer.halves(4));
            assertEquals("4½", ShipRenderer.halves(9));
            assertEquals("5½", ShipRenderer.halves(11),
                    "five and a half beats five, and a display that showed both as five "
                            + "would be lying about the number that decides fights");
        }

        @Test
        @DisplayName("what is wrong with a ship is said in printed coordinates")
        void problemsAreLocatable() {
            ShipView broken = new ShipView(5, 7, 5, 4, Set.of(CABIN), Map.of(), List.of(), 1,
                    new ShipAttributes(0, 0, 0),
                    new ValidationReport(List.of(new ShipViolation(
                            ViolationKind.BLOCKED_ENGINE_EXHAUST, Set.of(CABIN),
                            "something is welded behind this engine"))));

            List<String> problems = ShipRenderer.problems(broken);

            assertEquals(2, problems.size());
            assertTrue(problems.get(1).contains("7,7"),
                    "row 2 column 3 is printed as 7,7 and that is where the player will look");
        }

        @Test
        @DisplayName("a ship with nothing wrong with it says nothing")
        void silenceWhenLegal() {
            assertEquals(List.of(), ShipRenderer.problems(shipWith(Map.of())));
        }

        @Test
        @DisplayName("a described ship carries its title, its board and its numbers")
        void describing() {
            Map<Position, CellView> cells = new LinkedHashMap<>();
            cells.put(CABIN, new CellView(tile(ComponentKind.STARTING_CABIN, Rotation.NONE),
                    0, List.of(), 2, null));

            List<String> lines = ShipRenderer.describe(shipWith(cells), "samuele (RED)");

            assertEquals("samuele (RED)", lines.get(0));
            assertTrue(lines.stream().anyMatch(line -> line.contains("@2")));
            assertTrue(lines.stream().anyMatch(line -> line.contains("firepower")));
        }
    }

    @Nested
    @DisplayName("a tile in the hand")
    class InHand {

        @Test
        @DisplayName("its four sides are shown, because that is what decides where it can go")
        void connectorsAreVisible() {
            List<String> lines = TileRenderer.render(tile(ComponentKind.CABIN, Rotation.NONE), "C.");

            assertEquals("     U", lines.get(0));
            assertTrue(lines.get(1).startsWith("   . C. 1"),
                    "west is smooth and east takes a single pipe");
            assertEquals("     2", lines.get(2));
        }

        @Test
        @DisplayName("a tile says what it is called, what it is, and how far it has been turned")
        void describing() {
            assertEquals("cabin · cabin · upright",
                    TileRenderer.describe(tile(ComponentKind.CABIN, Rotation.NONE)));
            assertEquals("shield · shield · turned 180°",
                    TileRenderer.describe(tile(ComponentKind.SHIELD, Rotation.CLOCKWISE_180)));
        }
    }
}
