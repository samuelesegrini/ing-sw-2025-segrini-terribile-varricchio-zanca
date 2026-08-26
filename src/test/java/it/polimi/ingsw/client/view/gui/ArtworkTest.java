package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that everything in the game has a picture, and that the picture is really there.
 *
 * <p>Worth a test rather than a glance because the failure is quiet: a component with no
 * artwork is a blank square in the middle of somebody's ship, and it appears only when that one
 * tile happens to be drawn. There are a hundred and fifty-six of them.
 *
 * <p>None of this needs a screen, which is the point of keeping the lookup away from anything
 * that draws — the machine that builds this project has no display at all.
 */
class ArtworkTest {

    private final Artwork artwork = Artwork.bundled();

    @Test
    @DisplayName("every component in the game has a picture, and the file exists")
    void everyTileIsDrawn() {
        GameData data = GameDataLoader.loadBundled();

        List<String> missing = data.tiles().stream()
                .map(tile -> tile.id())
                .filter(id -> artwork.ofTile(id).isEmpty())
                .toList();
        assertTrue(missing.isEmpty(), "components with no artwork: " + missing);

        List<String> broken = data.tiles().stream()
                .map(tile -> artwork.ofTile(tile.id()).orElseThrow())
                .filter(path -> Artwork.class.getResource(path) == null)
                .toList();
        assertTrue(broken.isEmpty(), "artwork named but not shipped: " + broken);
    }

    @Test
    @DisplayName("every adventure card has a picture, and the file exists")
    void everyCardIsDrawn() {
        GameData data = GameDataLoader.loadBundled();

        List<String> missing = data.cards().stream()
                .map(card -> card.id())
                .filter(id -> artwork.ofCard(id).isEmpty())
                .toList();
        assertTrue(missing.isEmpty(), "cards with no artwork: " + missing);

        List<String> broken = data.cards().stream()
                .map(card -> artwork.ofCard(card.id()).orElseThrow())
                .filter(path -> Artwork.class.getResource(path) == null)
                .toList();
        assertTrue(broken.isEmpty(), "artwork named but not shipped: " + broken);
    }

    @Test
    @DisplayName("both boards are shipped, for both levels that are played")
    void theBoards() {
        for (GameLevel level : GameLevel.values()) {
            assertNotNull(Artwork.class.getResource(artwork.ofShipBoard(level)),
                    "no ship board for " + level);
            assertNotNull(Artwork.class.getResource(artwork.ofFlightBoard(level)),
                    "no route board for " + level);
        }
    }

    @Test
    @DisplayName("a name nobody filed anything under comes back empty rather than guessing")
    void unknownNames() {
        assertTrue(artwork.ofTile("no-such-tile").isEmpty());
        assertTrue(artwork.ofCard("no-such-card").isEmpty());
    }

    @Test
    @DisplayName("the manifest is read whole, not to the first thing that looks like the end")
    void theWholeManifest() {
        // It is one file with two sections and a hundred and ninety-six entries between them.
        // A parser that stopped early would still answer for the first few and fail only for
        // whatever came after, which is the kind of bug that shows up in somebody's demo.
        assertEquals(156, artwork.tileCount());
        assertEquals(40, artwork.cardCount());
    }
}
