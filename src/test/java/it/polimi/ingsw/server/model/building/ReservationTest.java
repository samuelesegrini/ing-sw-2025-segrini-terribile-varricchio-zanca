package it.polimi.ingsw.server.model.building;

import it.polimi.ingsw.server.model.board.ShipBoardSpec;
import it.polimi.ingsw.server.model.component.ComponentKind;
import it.polimi.ingsw.server.model.component.ComponentTile;
import it.polimi.ingsw.server.model.component.Tiles;
import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.Connector;
import it.polimi.ingsw.server.model.ship.Position;
import it.polimi.ingsw.server.model.ship.Rotation;
import it.polimi.ingsw.server.model.ship.Ship;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the reservation corner against manual p.17.
 *
 * <p>Reserving looks like a safety net and is not one. A reserved tile can never go back
 * on the table, so the only two things that can happen to it are being welded on or being
 * written off — and a tile still sitting in the corner when building ends counts as a
 * component lost along the route, worth a credit off the final score. Modelling it as a
 * free parking space would remove the whole cost of the decision.
 *
 * <p>Components involved: {@link ShipBuilder}, {@link ComponentPool}, {@link ShipBoardSpec}.
 */
class ReservationTest {

    private static final Position CABIN = new Position(2, 2);

    private static List<ComponentTile> tiles() {
        List<ComponentTile> tiles = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tiles.add(new ComponentTile("tile-" + i, ComponentKind.STRUCTURAL_MODULE,
                    Tiles.allSides(Connector.UNIVERSAL), 0));
        }
        return tiles;
    }

    /**
     * Returns a builder on a board with the given number of reservation slots.
     *
     * @param slots two for a level II board, none for a test flight board
     */
    private static ShipBuilder builderWith(int slots) {
        Ship ship = new Ship(new ShipBoardSpec(5, 5, 5, 4, CABIN, slots, Set.of()),
                Tiles.startingCabin(PlayerColor.YELLOW));
        return new ShipBuilder(ship, new ComponentPool(tiles(), new Random(20250825L)));
    }

    @Test
    @DisplayName("a level II board offers two reservation slots and a test flight board none")
    void reservationDependsOnTheBoard() {
        assertTrue(builderWith(2).reservationAllowed());
        assertFalse(builderWith(0).reservationAllowed());
    }

    @Test
    @DisplayName("reserving in a test flight is refused, because that rule arrives with the complete game")
    void reservingInATestFlight_isRefused() {
        ShipBuilder builder = builderWith(0);
        builder.drawFaceDown();

        assertThrows(IllegalStateException.class, builder::reserve);
    }

    @Test
    @DisplayName("a reserved tile leaves the hand and waits in the corner")
    void reservedTile_waitsInTheCorner() {
        ShipBuilder builder = builderWith(2);
        ComponentTile drawn = builder.drawFaceDown();

        builder.reserve();

        assertEquals(List.of(drawn), builder.reserved());
        assertTrue(builder.inHand().isEmpty());
    }

    @Test
    @DisplayName("only two tiles fit in the corner, and the third is refused")
    void onlyTwoTilesFitInTheCorner() {
        ShipBuilder builder = builderWith(2);
        builder.drawFaceDown();
        builder.reserve();
        builder.drawFaceDown();
        builder.reserve();

        assertFalse(builder.canReserve());
        builder.drawFaceDown();
        assertThrows(IllegalStateException.class, builder::reserve);
    }

    @Test
    @DisplayName("attaching a reserved tile frees its slot for another")
    void attachingAReservedTile_freesItsSlot() {
        ShipBuilder builder = builderWith(2);
        ComponentTile first = builder.drawFaceDown();
        builder.reserve();
        builder.drawFaceDown();
        builder.reserve();

        builder.takeReserved(first.id());
        builder.attach(new Position(1, 2), Rotation.NONE);

        assertEquals(1, builder.reserved().size());
        assertTrue(builder.canReserve());
        assertTrue(builder.ship().componentAt(new Position(1, 2)).isPresent());
    }

    @Test
    @DisplayName("a reserved tile can never go back on the table, which is what makes reserving cost something")
    void reservedTile_neverGoesBackOnTheTable() {
        ShipBuilder builder = builderWith(2);
        ComponentTile drawn = builder.drawFaceDown();
        builder.reserve();

        builder.takeReserved(drawn.id());

        assertThrows(IllegalStateException.class, builder::returnToPool);
        assertTrue(builder.inHand().isPresent(), "the tile stays in hand rather than vanishing");
    }

    @Test
    @DisplayName("a tile picked up from the corner can go straight back into it")
    void tileFromTheCorner_canGoBackIntoIt() {
        ShipBuilder builder = builderWith(2);
        ComponentTile drawn = builder.drawFaceDown();
        builder.reserve();
        builder.takeReserved(drawn.id());

        builder.reserve();

        assertEquals(List.of(drawn), builder.reserved());
    }

    @Test
    @DisplayName("picking a reserved tile back up welds whatever was still loose")
    void takingAReservedTile_weldsTheLooseOne() {
        ShipBuilder builder = builderWith(2);
        ComponentTile spare = builder.drawFaceDown();
        builder.reserve();
        builder.drawFaceDown();
        builder.attach(new Position(1, 2), Rotation.NONE);

        builder.takeReserved(spare.id());

        assertTrue(builder.unweldedCell().isEmpty());
    }

    @Test
    @DisplayName("reserving with empty hands, or picking up a tile that is not there, is refused")
    void impossibleReservationMoves_areRefused() {
        ShipBuilder builder = builderWith(2);

        assertThrows(IllegalStateException.class, builder::reserve);
        assertThrows(IllegalStateException.class, () -> builder.takeReserved("tile-0"));

        builder.drawFaceDown();
        assertThrows(IllegalStateException.class, () -> builder.takeReserved("tile-0"));
    }

    @Test
    @DisplayName("a tile left in the corner is still there when building ends, ready to be written off")
    void tileLeftInTheCorner_staysThere() {
        ShipBuilder builder = builderWith(2);
        ComponentTile abandoned = builder.drawFaceDown();
        builder.reserve();

        builder.drawFaceDown();
        builder.attach(new Position(1, 2), Rotation.NONE);
        builder.weld();

        assertEquals(List.of(abandoned), builder.reserved());
    }
}
