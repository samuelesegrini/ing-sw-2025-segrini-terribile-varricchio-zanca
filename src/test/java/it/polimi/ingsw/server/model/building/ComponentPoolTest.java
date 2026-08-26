package it.polimi.ingsw.server.model.building;

import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.server.model.component.ComponentTile;
import it.polimi.ingsw.server.model.component.Tiles;
import it.polimi.ingsw.common.game.Connector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the heap in the middle of the table against manual p.4.
 *
 * <p>The rule that shapes the whole building phase is that a tile turned face up stays
 * face up. Returning a tile is therefore a real decision — it hands everyone else a
 * known, pickable piece — rather than a free undo. A pool that let tiles go back down
 * would make returning costless and building far less interesting.
 *
 * <p>Components involved: {@link ComponentPool}, {@link ComponentTile}.
 */
class ComponentPoolTest {

    /** Distinct tiles, since two tiles with the same identity would defeat the point. */
    private static List<ComponentTile> tiles(int count) {
        List<ComponentTile> tiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tiles.add(new ComponentTile("tile-" + i, ComponentKind.STRUCTURAL_MODULE,
                    Tiles.allSides(Connector.UNIVERSAL), 0));
        }
        return tiles;
    }

    private static ComponentPool pool(int size) {
        return new ComponentPool(tiles(size), new Random(20250825L));
    }

    @Test
    @DisplayName("a new pool holds every tile face down and nothing on show")
    void newPool_isAllFaceDown() {
        ComponentPool pool = pool(5);

        assertEquals(5, pool.faceDownCount());
        assertEquals(List.of(), pool.faceUp());
        assertFalse(pool.isExhausted());
    }

    @Test
    @DisplayName("drawing face down takes one tile out of the heap")
    void drawingFaceDown_shrinksTheHeap() {
        ComponentPool pool = pool(3);

        ComponentTile drawn = pool.drawFaceDown();

        assertEquals(2, pool.faceDownCount());
        assertTrue(pool.peekFaceUp(drawn.id()).isEmpty(), "a drawn tile is in a player's hand, not on show");
    }

    @Test
    @DisplayName("drawing the heap empty never hands out the same tile twice")
    void drawingTheHeapEmpty_yieldsEveryTileOnce() {
        ComponentPool pool = pool(10);
        Set<String> drawn = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            assertTrue(drawn.add(pool.drawFaceDown().id()));
        }

        assertEquals(0, pool.faceDownCount());
        assertTrue(pool.isExhausted());
        assertThrows(IllegalStateException.class, pool::drawFaceDown);
    }

    @Test
    @DisplayName("a returned tile becomes visible to everyone and stays that way")
    void returnedTile_becomesPublic() {
        ComponentPool pool = pool(3);
        ComponentTile drawn = pool.drawFaceDown();

        pool.returnFaceUp(drawn);

        assertEquals(List.of(drawn), pool.faceUp());
        assertEquals(Optional.of(drawn), pool.peekFaceUp(drawn.id()));
        assertEquals(2, pool.faceDownCount(), "it does not go back into the heap");
    }

    @Test
    @DisplayName("a face-up tile can be picked deliberately, unlike anything in the heap")
    void faceUpTile_canBeTakenByName() {
        ComponentPool pool = pool(3);
        ComponentTile first = pool.drawFaceDown();
        ComponentTile second = pool.drawFaceDown();
        pool.returnFaceUp(first);
        pool.returnFaceUp(second);

        assertEquals(second, pool.takeFaceUp(second.id()));
        assertEquals(List.of(first), pool.faceUp());
    }

    @Test
    @DisplayName("two players reaching for the same face-up tile: the first gets it, the second is told it is gone")
    void raceForAFaceUpTile_resolvesByArrivalOrder() {
        ComponentPool pool = pool(2);
        ComponentTile contested = pool.drawFaceDown();
        pool.returnFaceUp(contested);

        assertEquals(contested, pool.takeFaceUp(contested.id()));
        assertThrows(IllegalStateException.class, () -> pool.takeFaceUp(contested.id()));
    }

    @Test
    @DisplayName("taking a tile nobody has put on show is refused")
    void takingATileThatIsNotOnShow_isRefused() {
        ComponentPool pool = pool(3);

        assertThrows(IllegalStateException.class, () -> pool.takeFaceUp("tile-0"));
        assertEquals(Optional.empty(), pool.peekFaceUp("tile-0"));
    }

    @Test
    @DisplayName("returning a tile the pool already holds is refused, so nothing can be duplicated")
    void returningATileAlreadyInThePool_isRefused() {
        ComponentPool pool = pool(3);
        ComponentTile drawn = pool.drawFaceDown();
        pool.returnFaceUp(drawn);

        assertThrows(IllegalStateException.class, () -> pool.returnFaceUp(drawn));
    }

    @Test
    @DisplayName("a pool is exhausted only when both piles are empty")
    void poolIsExhausted_onlyWhenBothPilesAreEmpty() {
        ComponentPool pool = pool(1);
        ComponentTile only = pool.drawFaceDown();

        assertTrue(pool.isExhausted(), "nothing is in either pile while a player holds it");

        pool.returnFaceUp(only);
        assertFalse(pool.isExhausted());

        pool.takeFaceUp(only.id());
        assertTrue(pool.isExhausted());
    }
}
