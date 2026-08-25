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
 * Checks what declaring a ship finished does to a builder.
 *
 * <p>Finishing is not always voluntary: when the last hourglass period runs out, everyone
 * still building stops where they stand (manual p.17). So it has to cope with a player
 * caught mid-move — holding a tile, with another still loose on the hull — and put
 * everything down somewhere legal rather than leaving a tile in limbo or quietly
 * destroying it.
 *
 * <p>Components involved: {@link ShipBuilder}, {@link ComponentPool}.
 */
class FinishBuildingTest {

    private static final Position CABIN = new Position(2, 2);
    private static final Position NORTH_OF_CABIN = new Position(1, 2);

    private ComponentPool pool;

    private ShipBuilder builder(int reservationSlots) {
        List<ComponentTile> tiles = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tiles.add(new ComponentTile("tile-" + i, ComponentKind.STRUCTURAL_MODULE,
                    Tiles.allSides(Connector.UNIVERSAL), 0));
        }
        pool = new ComponentPool(tiles, new Random(20250825L));
        Ship ship = new Ship(new ShipBoardSpec(5, 5, 5, 4, CABIN, reservationSlots, Set.of()),
                Tiles.startingCabin(PlayerColor.GREEN));
        return new ShipBuilder(ship, pool);
    }

    @Test
    @DisplayName("a builder is unfinished until it says otherwise")
    void builderStartsUnfinished() {
        ShipBuilder builder = builder(2);

        assertFalse(builder.hasFinished());
        builder.finish();
        assertTrue(builder.hasFinished());
    }

    @Test
    @DisplayName("finishing welds whatever was still loose")
    void finishing_weldsTheLooseTile() {
        ShipBuilder builder = builder(2);
        builder.drawFaceDown();
        builder.attach(NORTH_OF_CABIN, Rotation.NONE);

        builder.finish();

        assertTrue(builder.unweldedCell().isEmpty());
        assertTrue(builder.ship().componentAt(NORTH_OF_CABIN).isPresent());
    }

    @Test
    @DisplayName("a tile still in hand goes back on the table face up, where anyone can take it")
    void heldTile_goesBackFaceUp() {
        ShipBuilder builder = builder(2);
        ComponentTile held = builder.drawFaceDown();

        builder.finish();

        assertTrue(builder.inHand().isEmpty());
        assertEquals(List.of(held), pool.faceUp());
    }

    @Test
    @DisplayName("a reserved tile still in hand goes back to the corner, since it may not return to the table")
    void heldReservedTile_goesBackToTheCorner() {
        ShipBuilder builder = builder(2);
        ComponentTile reserved = builder.drawFaceDown();
        builder.reserve();
        builder.takeReserved(reserved.id());

        builder.finish();

        assertTrue(builder.inHand().isEmpty());
        assertEquals(List.of(reserved), builder.reserved());
        assertEquals(List.of(), pool.faceUp(), "a reserved tile never goes back on the table");
    }

    @Test
    @DisplayName("a finished ship takes no more tiles, however they are offered")
    void finishedShip_refusesEveryBuildingMove() {
        ShipBuilder builder = builder(2);
        ComponentTile spare = builder.drawFaceDown();
        builder.returnToPool();
        builder.finish();

        assertThrows(IllegalStateException.class, builder::drawFaceDown);
        assertThrows(IllegalStateException.class, () -> builder.takeFaceUp(spare.id()));
        assertThrows(IllegalStateException.class, () -> builder.attach(NORTH_OF_CABIN, Rotation.NONE));
        assertThrows(IllegalStateException.class, () -> builder.adjust(NORTH_OF_CABIN, Rotation.NONE));
        assertThrows(IllegalStateException.class, builder::reserve);
        assertThrows(IllegalStateException.class, () -> builder.takeReserved(spare.id()));
        assertThrows(IllegalStateException.class, builder::returnToPool);
    }

    @Test
    @DisplayName("finishing twice is refused, so nobody can claim a second start space")
    void finishingTwice_isRefused() {
        ShipBuilder builder = builder(2);
        builder.finish();

        assertThrows(IllegalStateException.class, builder::finish);
    }

    @Test
    @DisplayName("a player caught mid-move keeps every tile they had, one on the hull and one on the table")
    void forcedStop_leavesNothingInLimbo() {
        ShipBuilder builder = builder(2);
        builder.drawFaceDown();
        builder.attach(NORTH_OF_CABIN, Rotation.NONE);
        ComponentTile caught = builder.drawFaceDown();

        builder.finish();

        assertEquals(2, builder.ship().components().size(), "the loose tile stayed welded on");
        assertEquals(List.of(caught), pool.faceUp(), "the tile in hand went back on the table");
        assertTrue(builder.ship().validate().isLegal());
    }
}
