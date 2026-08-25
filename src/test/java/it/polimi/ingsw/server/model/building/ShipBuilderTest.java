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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * Checks how a player builds, against manual p.4 and p.5.
 *
 * <p>Two rules do most of the work here. Only one tile may be held at a time, so a tile
 * has to be dealt with before another can be taken. And the tile just placed stays
 * movable until the player reaches for the next one, at which point it is welded and
 * final — which makes drawing a tile a commitment to the previous one rather than a
 * neutral act.
 *
 * <p>Components involved: {@link ShipBuilder}, {@link ComponentPool}, {@link Ship}.
 */
class ShipBuilderTest {

    private static final Position CABIN = new Position(2, 2);
    private static final Position NORTH_OF_CABIN = new Position(1, 2);
    private static final Position WEST_OF_CABIN = new Position(2, 1);

    private ShipBuilder builder;
    private ComponentPool pool;

    @BeforeEach
    void setUp() {
        List<ComponentTile> tiles = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tiles.add(new ComponentTile("tile-" + i, ComponentKind.STRUCTURAL_MODULE,
                    Tiles.allSides(Connector.UNIVERSAL), 0));
        }
        pool = new ComponentPool(tiles, new Random(20250825L));
        Ship ship = new Ship(new ShipBoardSpec(5, 5, 5, 4, CABIN, 0, Set.of()),
                Tiles.startingCabin(PlayerColor.RED));
        builder = new ShipBuilder(ship, pool);
    }

    @Nested
    @DisplayName("one tile at a time")
    class OneTileAtATime {

        @Test
        @DisplayName("a player starts with their hands free")
        void playerStartsEmptyHanded() {
            assertTrue(builder.inHand().isEmpty());
            assertTrue(builder.unweldedCell().isEmpty());
        }

        @Test
        @DisplayName("drawing while already holding a tile is refused")
        void drawingWhileHolding_isRefused() {
            builder.drawFaceDown();

            assertThrows(IllegalStateException.class, builder::drawFaceDown);
        }

        @Test
        @DisplayName("taking a face-up tile while already holding one is refused too")
        void takingFaceUpWhileHolding_isRefused() {
            ComponentTile first = builder.drawFaceDown();
            builder.returnToPool();
            builder.drawFaceDown();

            assertThrows(IllegalStateException.class, () -> builder.takeFaceUp(first.id()));
        }

        @Test
        @DisplayName("attaching or returning a tile frees the hand for the next one")
        void dealingWithATile_freesTheHand() {
            builder.drawFaceDown();
            builder.attach(NORTH_OF_CABIN, Rotation.NONE);
            assertTrue(builder.inHand().isEmpty());

            builder.drawFaceDown();
            builder.returnToPool();
            assertTrue(builder.inHand().isEmpty());
        }

        @Test
        @DisplayName("attaching or returning with empty hands is refused")
        void actingWithEmptyHands_isRefused() {
            assertThrows(IllegalStateException.class, () -> builder.attach(NORTH_OF_CABIN, Rotation.NONE));
            assertThrows(IllegalStateException.class, builder::returnToPool);
        }
    }

    @Nested
    @DisplayName("welding")
    class Welding {

        @Test
        @DisplayName("a tile just placed is still loose and can be moved")
        void placedTile_staysLoose() {
            builder.drawFaceDown();
            builder.attach(NORTH_OF_CABIN, Rotation.NONE);

            assertEquals(NORTH_OF_CABIN, builder.unweldedCell().orElseThrow());

            builder.adjust(WEST_OF_CABIN, Rotation.CLOCKWISE_90);

            assertEquals(WEST_OF_CABIN, builder.unweldedCell().orElseThrow());
            assertTrue(builder.ship().componentAt(NORTH_OF_CABIN).isEmpty());
            assertEquals(Rotation.CLOCKWISE_90,
                    builder.ship().componentAt(WEST_OF_CABIN).orElseThrow().rotation());
        }

        @Test
        @DisplayName("reaching for the next tile welds the last one, which is what makes drawing a commitment")
        void drawingTheNextTile_weldsTheLastOne() {
            builder.drawFaceDown();
            builder.attach(NORTH_OF_CABIN, Rotation.NONE);

            builder.drawFaceDown();

            assertTrue(builder.unweldedCell().isEmpty());
            assertThrows(IllegalStateException.class, () -> builder.adjust(WEST_OF_CABIN, Rotation.NONE));
        }

        @Test
        @DisplayName("taking a face-up tile welds the last one as surely as drawing does")
        void takingFaceUp_alsoWelds() {
            ComponentTile spare = builder.drawFaceDown();
            builder.returnToPool();

            builder.drawFaceDown();
            builder.attach(NORTH_OF_CABIN, Rotation.NONE);
            builder.takeFaceUp(spare.id());

            assertTrue(builder.unweldedCell().isEmpty());
        }

        @Test
        @DisplayName("a welded tile stays exactly where it was put")
        void weldedTile_staysPut() {
            builder.drawFaceDown();
            builder.attach(NORTH_OF_CABIN, Rotation.NONE);
            builder.weld();

            assertTrue(builder.ship().componentAt(NORTH_OF_CABIN).isPresent());
            assertThrows(IllegalStateException.class, () -> builder.adjust(WEST_OF_CABIN, Rotation.NONE));
        }

        @Test
        @DisplayName("a tile cannot be moved while another is in hand")
        void adjustingWhileHolding_isRefused() {
            builder.drawFaceDown();
            builder.attach(NORTH_OF_CABIN, Rotation.NONE);
            builder.weld();
            builder.drawFaceDown();
            builder.attach(WEST_OF_CABIN, Rotation.NONE);
            builder.drawFaceDown();

            assertThrows(IllegalStateException.class, () -> builder.adjust(new Position(3, 2), Rotation.NONE));
        }
    }

    @Nested
    @DisplayName("attaching to the ship")
    class Attaching {

        @Test
        @DisplayName("a tile must go somewhere touching what is already built")
        void tileMustTouchTheShip() {
            builder.drawFaceDown();

            assertThrows(IllegalArgumentException.class,
                    () -> builder.attach(new Position(0, 0), Rotation.NONE));
            assertTrue(builder.inHand().isPresent(), "a refused placement leaves the tile in hand");
        }

        @Test
        @DisplayName("a refused move leaves the loose tile exactly where it was, rotation included")
        void refusedMove_leavesTheTileWhereItWas() {
            builder.drawFaceDown();
            builder.attach(NORTH_OF_CABIN, Rotation.CLOCKWISE_180);

            assertThrows(IllegalArgumentException.class,
                    () -> builder.adjust(new Position(0, 0), Rotation.NONE));

            assertEquals(NORTH_OF_CABIN, builder.unweldedCell().orElseThrow());
            assertEquals(Rotation.CLOCKWISE_180,
                    builder.ship().componentAt(NORTH_OF_CABIN).orElseThrow().rotation());
        }

        @Test
        @DisplayName("a returned tile goes back face up, where anyone can pick it")
        void returnedTile_goesBackFaceUp() {
            ComponentTile drawn = builder.drawFaceDown();
            builder.returnToPool();

            assertEquals(List.of(drawn), pool.faceUp());
            assertFalse(pool.isExhausted());
        }

        @Test
        @DisplayName("a ship grows one tile at a time and every tile stays where it was welded")
        void shipGrowsOneTileAtATime() {
            builder.drawFaceDown();
            builder.attach(NORTH_OF_CABIN, Rotation.NONE);
            builder.drawFaceDown();
            builder.attach(WEST_OF_CABIN, Rotation.NONE);
            builder.drawFaceDown();
            builder.attach(new Position(3, 2), Rotation.NONE);
            builder.weld();

            assertEquals(4, builder.ship().components().size(), "three tiles plus the starting cabin");
            assertTrue(builder.ship().isWhole());
            assertTrue(builder.ship().validate().isLegal());
        }
    }
}
