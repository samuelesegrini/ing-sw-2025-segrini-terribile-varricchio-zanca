package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.server.model.goods.GoodColor;
import it.polimi.ingsw.server.model.ship.Rotation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a cargo hold will and will not carry.
 *
 * <p>The red cube rule is the one worth guarding: hazardous material only travels in a
 * reinforced hold, and red cubes are also the most valuable, so a hold that accepted
 * them would both break the rule and inflate every score.
 *
 * <p>Components involved: {@link CargoHoldComponent}, {@link GoodColor}.
 */
class CargoHoldComponentTest {

    private static CargoHoldComponent standard(int capacity) {
        return new CargoHoldComponent(Tiles.of(ComponentKind.CARGO_HOLD, capacity), Rotation.NONE);
    }

    private static CargoHoldComponent special(int capacity) {
        return new CargoHoldComponent(Tiles.of(ComponentKind.SPECIAL_CARGO_HOLD, capacity), Rotation.NONE);
    }

    @Test
    @DisplayName("a standard hold refuses a red cube, because hazardous material needs a reinforced hold")
    void standardHold_refusesRedCubes() {
        CargoHoldComponent hold = standard(3);

        assertFalse(hold.accepts(GoodColor.RED));
        assertThrows(IllegalStateException.class, () -> hold.store(GoodColor.RED));
    }

    @Test
    @DisplayName("a special hold takes every colour, red included")
    void specialHold_takesEveryColour() {
        CargoHoldComponent hold = special(2);

        assertTrue(hold.accepts(GoodColor.RED));
        hold.store(GoodColor.RED);
        hold.store(GoodColor.BLUE);

        assertEquals(List.of(GoodColor.RED, GoodColor.BLUE), hold.contents());
    }

    @Test
    @DisplayName("a hold takes one cube per printed slot and refuses the next one")
    void hold_refusesCubesBeyondItsCapacity() {
        CargoHoldComponent hold = standard(2);
        hold.store(GoodColor.BLUE);
        hold.store(GoodColor.GREEN);

        assertTrue(hold.isFull());
        assertFalse(hold.accepts(GoodColor.YELLOW));
        assertThrows(IllegalStateException.class, () -> hold.store(GoodColor.YELLOW));
    }

    @Test
    @DisplayName("the most valuable cube is the one a card takes first")
    void mostValuable_isTheFirstToBeTaken() {
        CargoHoldComponent hold = special(3);
        hold.store(GoodColor.BLUE);
        hold.store(GoodColor.RED);
        hold.store(GoodColor.GREEN);

        assertEquals(Optional.of(GoodColor.RED), hold.mostValuable());
    }

    @Test
    @DisplayName("an empty hold has nothing to give up")
    void emptyHold_hasNoMostValuableCube() {
        assertEquals(Optional.empty(), standard(2).mostValuable());
    }

    @Test
    @DisplayName("removing a colour the hold is not carrying is refused rather than silently ignored")
    void removingAnAbsentColour_isRefused() {
        CargoHoldComponent hold = standard(2);
        hold.store(GoodColor.BLUE);

        assertThrows(IllegalStateException.class, () -> hold.remove(GoodColor.GREEN));
        hold.remove(GoodColor.BLUE);
        assertEquals(0, hold.load());
    }

    @Test
    @DisplayName("contents are reported most valuable first and cannot be edited through the view")
    void contents_areSortedAndUnmodifiable() {
        CargoHoldComponent hold = special(3);
        hold.store(GoodColor.BLUE);
        hold.store(GoodColor.YELLOW);
        hold.store(GoodColor.RED);

        assertEquals(List.of(GoodColor.RED, GoodColor.YELLOW, GoodColor.BLUE), hold.contents());
        assertThrows(UnsupportedOperationException.class, () -> hold.contents().add(GoodColor.GREEN));
    }
}
