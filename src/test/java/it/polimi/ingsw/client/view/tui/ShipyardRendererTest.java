package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.AdventureCardType;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.common.protocol.view.BuildingView;
import it.polimi.ingsw.common.protocol.view.ShipView;
import it.polimi.ingsw.common.protocol.view.TileView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a player can play the building phase from what is on the screen.
 *
 * <p>Almost everything in a shipyard is public and has to be shown: the discard pile is what
 * anybody may take, how many players have finished is how much time is really left, and the
 * free start spaces are what everybody is racing for. A screen showing only your own ship would
 * be a screen you could not play from.
 */
class ShipyardRendererTest {

    private static TileView tile(String id, ComponentKind kind) {
        Map<Direction, Connector> connectors = new EnumMap<>(Direction.class);
        connectors.put(Direction.NORTH, Connector.UNIVERSAL);
        connectors.put(Direction.EAST, Connector.SINGLE);
        connectors.put(Direction.SOUTH, Connector.DOUBLE);
        connectors.put(Direction.WEST, Connector.PLAIN);
        return new TileView(id, kind, Rotation.NONE, connectors);
    }

    private static BuildingView yard(int faceDown, List<TileView> faceUp, TileView hand,
                                     List<AdventureCardIdentity> scouted, Integer glassAt,
                                     int glassSpaces, long seconds, Set<PlayerColor> finished,
                                     List<Integer> free) {
        return new BuildingView(faceDown, faceUp, hand, null, scouted, glassAt, glassSpaces,
                seconds, finished, free);
    }

    private static String joined(List<String> lines) {
        return String.join("\n", lines);
    }

    @Test
    @DisplayName("the heap, the discard pile and the hourglass are all on screen")
    void theTable() {
        String screen = joined(ShipyardRenderer.render(yard(96,
                List.of(tile("shield_1U2.", ComponentKind.SHIELD)), null, List.of(),
                1, 3, 42, Set.of(PlayerColor.BLUE), List.of(2, 4))));

        assertTrue(screen.contains("96 face down"));
        assertTrue(screen.contains("shield_1U2."), "the discard pile is what anybody may take");
        assertTrue(screen.contains("hourglass 2/3, 42s left"),
                "the model counts spaces from zero; a player reading a board sees three of them "
                        + "and the sand in one");
        assertTrue(screen.contains("finished: BLUE"),
                "how many have finished is how much time is really left");
        assertTrue(screen.contains("free start spaces: 2 4"));
    }

    @Test
    @DisplayName("an empty discard pile says so rather than showing a blank")
    void nothingDiscardedYet() {
        String screen = joined(ShipyardRenderer.render(
                yard(120, List.of(), null, List.of(), null, 3, 0, Set.of(), List.of(1, 2, 3, 4))));

        assertTrue(screen.contains("nothing yet"));
        assertTrue(screen.contains("finished: nobody"));
        assertTrue(screen.contains("hourglass not started"));
    }

    @Test
    @DisplayName("the test flight has no hourglass, and is not told it has one at zero")
    void noHourglass() {
        String screen = joined(ShipyardRenderer.render(
                yard(60, List.of(), null, List.of(), null, 0, 0, Set.of(), List.of(1, 2))));

        assertTrue(screen.contains("no hourglass"));
        assertFalse(screen.contains("0/0"), "inventing something to worry about is worse than "
                + "saying there is nothing");
    }

    @Test
    @DisplayName("the tile in your hand is shown with its connectors, because that is the decision")
    void theHand() {
        String screen = joined(ShipyardRenderer.render(yard(96, List.of(),
                tile("battery_UD-SD", ComponentKind.BATTERY), List.of(), 1, 3, 10,
                Set.of(), List.of(1))));

        assertTrue(screen.contains("in hand"));
        assertTrue(screen.contains("battery_UD-SD"));
        assertTrue(screen.contains("U"), "the board says what a component is; this says what it "
                + "will join to");
    }

    @Test
    @DisplayName("empty hands take up no room on the screen")
    void noHand() {
        assertEquals(List.of(), ShipyardRenderer.hand(
                yard(96, List.of(), null, List.of(), 1, 3, 10, Set.of(), List.of(1))));
    }

    @Test
    @DisplayName("a pile somebody peeked at is shown to them, and to nobody else")
    void peekedCards() {
        // The server builds this view per recipient, so a pile in it is a pile this player is
        // holding. Everything else on the screen is public.
        String screen = joined(ShipyardRenderer.render(yard(96, List.of(), null,
                List.of(new AdventureCardIdentity("pirates_lvl2", AdventureCardType.PIRATES,
                        CardLevel.LEVEL_II, false)),
                1, 3, 10, Set.of(), List.of(1))));

        assertTrue(screen.contains("the pile in your hands"));
        assertTrue(screen.contains("pirates"));
        assertTrue(screen.contains("level ii"));
    }

    @Test
    @DisplayName("a tile that is down but not welded is called out, because a board cannot show it")
    void looseTiles() {
        ShipView ship = new ShipView(5, 7, 5, 4, Set.of(), Map.of(), List.of(), 0,
                new it.polimi.ingsw.common.game.ShipAttributes(0, 0, 0),
                it.polimi.ingsw.common.game.ValidationReport.legal());
        BuildingView loose = new BuildingView(96, List.of(), null,
                new it.polimi.ingsw.common.game.Position(2, 4), List.of(), 1, 3, 10,
                Set.of(), List.of(1));

        String line = joined(ShipyardRenderer.loose(loose, ship));

        assertTrue(line.contains("7,8"), "and it says which square, in the printed numbers");
        assertTrue(line.contains("weld"), "along with what to do about it");
        assertEquals(List.of(), ShipyardRenderer.loose(
                yard(96, List.of(), null, List.of(), 1, 3, 10, Set.of(), List.of(1)), ship),
                "a ship with nothing loose on it says nothing");
    }

    @Test
    @DisplayName("a table with nothing left to claim says so")
    void everySpaceTaken() {
        String screen = joined(ShipyardRenderer.render(
                yard(0, List.of(), null, List.of(), 3, 3, 0, Set.of(PlayerColor.RED,
                        PlayerColor.BLUE), List.of())));

        assertTrue(screen.contains("free start spaces: none"));
        assertTrue(screen.contains("finished: BLUE RED"), "and who took them");
    }
}
