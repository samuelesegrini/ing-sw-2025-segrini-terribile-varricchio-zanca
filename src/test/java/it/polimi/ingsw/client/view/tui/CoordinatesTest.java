package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.ShipAttributes;
import it.polimi.ingsw.common.game.ValidationReport;
import it.polimi.ingsw.common.protocol.view.ShipView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the subtraction between what a player types and what the model means.
 *
 * <p>Worth its own tests because getting it backwards is not a crash. It is a tile welded three
 * squares from where the player meant, found during validation, and paid for with a component —
 * the kind of mistake that looks like the game being unfair rather than the client being wrong.
 */
class CoordinatesTest {

    /** The level II board: five rows numbered from 5, seven columns numbered from 4. */
    private static final ShipView LEVEL_TWO = new ShipView(5, 7, 5, 4,
            Set.of(new Position(0, 0)), Map.of(), List.of(), 0,
            new ShipAttributes(0, 0, 0), ValidationReport.legal());

    @Test
    @DisplayName("the top left square is row five, column four")
    void theCorner() {
        assertEquals(Optional.of(new Position(0, 0)),
                Coordinates.on(LEVEL_TWO, OptionalInt.of(5), OptionalInt.of(4)));
    }

    @Test
    @DisplayName("the starting cabin is where a player would say it is")
    void theStartingCabin() {
        // The middle of a level II board is index (2,3) and is printed 7,7. A player reading a
        // meteor roll of seven has to be able to find it without counting.
        assertEquals(Optional.of(new Position(2, 3)),
                Coordinates.on(LEVEL_TWO, OptionalInt.of(7), OptionalInt.of(7)));
        assertEquals("7,7", Coordinates.printed(LEVEL_TWO, new Position(2, 3)));
    }

    @Test
    @DisplayName("the bottom right square is the last one on the board")
    void theFarCorner() {
        assertEquals(Optional.of(new Position(4, 6)),
                Coordinates.on(LEVEL_TWO, OptionalInt.of(9), OptionalInt.of(10)));
    }

    @Test
    @DisplayName("a square that is not on the board is refused rather than wrapped")
    void offTheBoard() {
        assertTrue(Coordinates.on(LEVEL_TWO, OptionalInt.of(4), OptionalInt.of(7)).isEmpty(),
                "row four is above the board");
        assertTrue(Coordinates.on(LEVEL_TWO, OptionalInt.of(10), OptionalInt.of(7)).isEmpty(),
                "row ten is below it");
        assertTrue(Coordinates.on(LEVEL_TWO, OptionalInt.of(7), OptionalInt.of(3)).isEmpty(),
                "column three is to the left of it");
        assertTrue(Coordinates.on(LEVEL_TWO, OptionalInt.of(7), OptionalInt.of(11)).isEmpty(),
                "column eleven is to the right");
    }

    @Test
    @DisplayName("a square nobody named is not a square at nought, nought")
    void missingNumbers() {
        assertTrue(Coordinates.on(LEVEL_TWO, OptionalInt.empty(), OptionalInt.of(7)).isEmpty());
        assertTrue(Coordinates.on(LEVEL_TWO, OptionalInt.of(7), OptionalInt.empty()).isEmpty());
        assertTrue(Coordinates.on(LEVEL_TWO, OptionalInt.empty(), OptionalInt.empty()).isEmpty());
    }

    @Test
    @DisplayName("somebody who gets it wrong is told what would have worked")
    void theRange() {
        assertEquals("rows 5-9, columns 4-10", Coordinates.range(LEVEL_TWO));
    }

    @Test
    @DisplayName("a different board is numbered differently, and nothing is hard-coded")
    void anotherBoard() {
        // The test flight board is smaller and starts elsewhere. A client that assumed level II
        // would put every tile in the wrong place on it.
        ShipView testFlight = new ShipView(5, 5, 5, 5, Set.of(), Map.of(), List.of(), 0,
                new ShipAttributes(0, 0, 0), ValidationReport.legal());

        assertEquals(Optional.of(new Position(0, 0)),
                Coordinates.on(testFlight, OptionalInt.of(5), OptionalInt.of(5)));
        assertTrue(Coordinates.on(testFlight, OptionalInt.of(5), OptionalInt.of(4)).isEmpty(),
                "column four is off this board even though it is on the other one");
        assertEquals("rows 5-9, columns 5-9", Coordinates.range(testFlight));
    }
}
