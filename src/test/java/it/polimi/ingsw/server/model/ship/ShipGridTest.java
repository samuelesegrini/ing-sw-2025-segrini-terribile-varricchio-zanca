package it.polimi.ingsw.server.model.ship;

import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.server.model.board.ShipBoardSpec;
import it.polimi.ingsw.server.model.component.ComponentTile;
import it.polimi.ingsw.server.model.component.ShipComponent;
import it.polimi.ingsw.server.model.component.Tiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the grid every ship is built on: what may sit where, what is welded to what,
 * and what a threat travelling along a line runs into first.
 *
 * <p>The distinction under test throughout is between cells that merely touch and cells
 * that are actually joined. Two smooth sides side by side are legal but are not a
 * joint, so a piece attached only that way is not attached at all — it flies off the
 * moment its real joints are cut (manual p.10). Testing connectivity over adjacency
 * instead of over joints would let that ship pass as whole.
 *
 * <p>Components involved: {@link ShipGrid}, {@link ShipBoardSpec}, {@link ShipComponent}.
 */
class ShipGridTest {

    /** A three by three board with every cell usable, so tests state their own shape. */
    private static ShipBoardSpec openBoard() {
        return new ShipBoardSpec(3, 3, 5, 4, new Position(1, 1), 0, true, Set.of());
    }

    private static ShipComponent tile(Connector north, Connector east, Connector south, Connector west) {
        return ShipComponent.place(
                new ComponentTile("t", ComponentKind.STRUCTURAL_MODULE,
                        Tiles.sides(north, east, south, west), 0),
                Rotation.NONE);
    }

    private static ShipComponent universal() {
        return ShipComponent.place(Tiles.of(ComponentKind.STRUCTURAL_MODULE), Rotation.NONE);
    }

    @Nested
    @DisplayName("occupancy")
    class Occupancy {

        @Test
        @DisplayName("a component welded into a cell is found there afterwards")
        void placedComponent_isFoundInItsCell() {
            ShipGrid grid = new ShipGrid(openBoard());
            ShipComponent component = universal();

            grid.put(new Position(1, 1), component);

            assertEquals(Optional.of(component), grid.at(new Position(1, 1)));
            assertTrue(grid.isOccupied(new Position(1, 1)));
            assertEquals(1, grid.size());
        }

        @Test
        @DisplayName("a cell outside the ship outline refuses a component")
        void cellOutsideTheOutline_refusesAComponent() {
            ShipBoardSpec board = new ShipBoardSpec(3, 3, 5, 4, new Position(1, 1), 0, true,
                    Set.of(new Position(0, 0)));
            ShipGrid grid = new ShipGrid(board);

            assertThrows(IllegalArgumentException.class,
                    () -> grid.put(new Position(0, 0), universal()));
            assertThrows(IllegalArgumentException.class,
                    () -> grid.put(new Position(9, 9), universal()));
        }

        @Test
        @DisplayName("welding onto an occupied cell is refused rather than overwriting silently")
        void occupiedCell_refusesASecondComponent() {
            ShipGrid grid = new ShipGrid(openBoard());
            grid.put(new Position(1, 1), universal());

            assertThrows(IllegalStateException.class, () -> grid.put(new Position(1, 1), universal()));
        }

        @Test
        @DisplayName("removing gives back what was there and leaves the cell free")
        void removing_freesTheCell() {
            ShipGrid grid = new ShipGrid(openBoard());
            ShipComponent component = universal();
            grid.put(new Position(1, 1), component);

            assertEquals(Optional.of(component), grid.remove(new Position(1, 1)));
            assertFalse(grid.isOccupied(new Position(1, 1)));
            assertEquals(Optional.empty(), grid.remove(new Position(1, 1)));
        }
    }

    @Nested
    @DisplayName("joints")
    class Joints {

        @Test
        @DisplayName("two universal connectors facing each other form a joint")
        void universalSidesFacingEachOther_weld() {
            ShipGrid grid = new ShipGrid(openBoard());
            grid.put(new Position(1, 1), universal());
            grid.put(new Position(0, 1), universal());

            assertTrue(grid.isJointAcross(new Position(1, 1), Direction.NORTH));
            assertTrue(grid.isJointAcross(new Position(0, 1), Direction.SOUTH));
        }

        @Test
        @DisplayName("two smooth sides touching is not a joint, however legal it is to place them")
        void smoothSidesTouching_areNotAJoint() {
            ShipGrid grid = new ShipGrid(openBoard());
            grid.put(new Position(1, 1), tile(Connector.PLAIN, Connector.UNIVERSAL,
                    Connector.UNIVERSAL, Connector.UNIVERSAL));
            grid.put(new Position(0, 1), tile(Connector.UNIVERSAL, Connector.UNIVERSAL,
                    Connector.PLAIN, Connector.UNIVERSAL));

            assertFalse(grid.isJointAcross(new Position(1, 1), Direction.NORTH));
            assertTrue(grid.touchesShip(new Position(0, 1)));
        }

        @Test
        @DisplayName("a single connector facing a double one is not a joint")
        void mismatchedConnectors_areNotAJoint() {
            ShipGrid grid = new ShipGrid(openBoard());
            grid.put(new Position(1, 1), tile(Connector.SINGLE, Connector.PLAIN,
                    Connector.PLAIN, Connector.PLAIN));
            grid.put(new Position(0, 1), tile(Connector.PLAIN, Connector.PLAIN,
                    Connector.DOUBLE, Connector.PLAIN));

            assertFalse(grid.isJointAcross(new Position(1, 1), Direction.NORTH));
        }

        @Test
        @DisplayName("a cell with nothing next to it has no joints and touches no ship")
        void isolatedCell_hasNeitherJointsNorNeighbours() {
            ShipGrid grid = new ShipGrid(openBoard());
            grid.put(new Position(1, 1), universal());

            assertEquals(Set.of(), grid.jointNeighbours(new Position(1, 1)));
            assertFalse(grid.touchesShip(new Position(1, 1)));
            assertTrue(grid.touchesShip(new Position(0, 1)));
        }
    }

    @Nested
    @DisplayName("connectivity")
    class Connectivity {

        @Test
        @DisplayName("components joined in a chain form one piece")
        void chainedComponents_formOnePiece() {
            ShipGrid grid = new ShipGrid(openBoard());
            grid.put(new Position(1, 0), universal());
            grid.put(new Position(1, 1), universal());
            grid.put(new Position(1, 2), universal());

            assertEquals(1, grid.pieces().size());
            assertTrue(grid.isWhole());
            assertEquals(3, grid.pieceContaining(new Position(1, 0)).size());
        }

        @Test
        @DisplayName("a component touching the hull only along smooth sides is a piece of its own")
        void componentJoinedOnlyBySmoothSides_isItsOwnPiece() {
            ShipGrid grid = new ShipGrid(openBoard());
            grid.put(new Position(1, 1), tile(Connector.PLAIN, Connector.UNIVERSAL,
                    Connector.UNIVERSAL, Connector.UNIVERSAL));
            grid.put(new Position(0, 1), tile(Connector.UNIVERSAL, Connector.UNIVERSAL,
                    Connector.PLAIN, Connector.UNIVERSAL));

            assertFalse(grid.isWhole(), "adjacency is not attachment");
            assertEquals(2, grid.pieces().size());
        }

        @Test
        @DisplayName("cutting the joint in the middle of a chain splits the ship in two")
        void removingTheMiddleOfAChain_splitsTheShip() {
            ShipGrid grid = new ShipGrid(openBoard());
            grid.put(new Position(1, 0), universal());
            grid.put(new Position(1, 1), universal());
            grid.put(new Position(1, 2), universal());

            grid.remove(new Position(1, 1));

            assertFalse(grid.isWhole());
            assertEquals(2, grid.pieces().size());
        }

        @Test
        @DisplayName("pieces come back largest first, so the fragment worth keeping is easy to offer")
        void pieces_areOrderedLargestFirst() {
            ShipGrid grid = new ShipGrid(openBoard());
            grid.put(new Position(0, 0), universal());
            grid.put(new Position(2, 0), universal());
            grid.put(new Position(2, 1), universal());
            grid.put(new Position(2, 2), universal());

            assertEquals(3, grid.pieces().get(0).size());
            assertEquals(1, grid.pieces().get(1).size());
        }

        @Test
        @DisplayName("an empty grid is whole, and an empty cell belongs to no piece")
        void emptyGrid_isWhole() {
            ShipGrid grid = new ShipGrid(openBoard());

            assertTrue(grid.isWhole());
            assertEquals(Set.of(), grid.pieceContaining(new Position(1, 1)));
        }
    }

    @Nested
    @DisplayName("lines of fire")
    class LinesOfFire {

        private ShipGrid gridWithColumn() {
            ShipGrid grid = new ShipGrid(openBoard());
            grid.put(new Position(1, 1), universal());
            grid.put(new Position(2, 1), universal());
            return grid;
        }

        @Test
        @DisplayName("a threat from the bow meets the topmost component of its column")
        void threatFromNorth_meetsTheTopmostComponent() {
            assertEquals(Optional.of(new Position(1, 1)),
                    gridWithColumn().firstInLine(Direction.NORTH, 1));
        }

        @Test
        @DisplayName("a threat from the stern meets the bottommost component of its column")
        void threatFromSouth_meetsTheBottommostComponent() {
            assertEquals(Optional.of(new Position(2, 1)),
                    gridWithColumn().firstInLine(Direction.SOUTH, 1));
        }

        @Test
        @DisplayName("a threat from the side meets the nearest component of its row")
        void threatFromASide_meetsTheNearestComponentOfItsRow() {
            ShipGrid grid = new ShipGrid(openBoard());
            grid.put(new Position(1, 0), universal());
            grid.put(new Position(1, 2), universal());

            assertEquals(Optional.of(new Position(1, 0)), grid.firstInLine(Direction.WEST, 1));
            assertEquals(Optional.of(new Position(1, 2)), grid.firstInLine(Direction.EAST, 1));
        }

        @Test
        @DisplayName("a threat down an empty line hits nothing at all")
        void threatDownAnEmptyLine_hitsNothing() {
            assertEquals(Optional.empty(), gridWithColumn().firstInLine(Direction.NORTH, 0));
        }
    }

    @Nested
    @DisplayName("exposed connectors")
    class ExposedConnectors {

        @Test
        @DisplayName("a lone component exposes every side that carries pipes and no smooth side")
        void loneComponent_exposesOnlyItsConnectors() {
            ShipGrid grid = new ShipGrid(openBoard());
            grid.put(new Position(1, 1), tile(Connector.SINGLE, Connector.PLAIN,
                    Connector.DOUBLE, Connector.UNIVERSAL));

            assertEquals(3, grid.exposedConnectors());
            assertTrue(grid.isConnectorExposed(new Position(1, 1), Direction.NORTH));
            assertFalse(grid.isConnectorExposed(new Position(1, 1), Direction.EAST));
        }

        @Test
        @DisplayName("a connector counts once whether it carries one pipe, two or three")
        void connectorCountsOnce_whateverItsPipeCount() {
            ShipGrid single = new ShipGrid(openBoard());
            single.put(new Position(1, 1), tile(Connector.SINGLE, Connector.PLAIN,
                    Connector.PLAIN, Connector.PLAIN));

            ShipGrid universal = new ShipGrid(openBoard());
            universal.put(new Position(1, 1), tile(Connector.UNIVERSAL, Connector.PLAIN,
                    Connector.PLAIN, Connector.PLAIN));

            assertEquals(1, single.exposedConnectors());
            assertEquals(1, universal.exposedConnectors());
        }

        @Test
        @DisplayName("a side facing a neighbour is covered, even when the two do not weld")
        void sideFacingANeighbour_isNotExposed() {
            ShipGrid grid = new ShipGrid(openBoard());
            grid.put(new Position(1, 1), tile(Connector.SINGLE, Connector.PLAIN,
                    Connector.PLAIN, Connector.PLAIN));
            grid.put(new Position(0, 1), tile(Connector.PLAIN, Connector.PLAIN,
                    Connector.DOUBLE, Connector.PLAIN));

            assertFalse(grid.isConnectorExposed(new Position(1, 1), Direction.NORTH));
            assertEquals(0, grid.exposedConnectors());
        }

        @Test
        @DisplayName("a connector at the edge of the board faces open space and stays exposed")
        void connectorAtTheBoardEdge_isExposed() {
            ShipGrid grid = new ShipGrid(openBoard());
            grid.put(new Position(0, 0), tile(Connector.SINGLE, Connector.PLAIN,
                    Connector.PLAIN, Connector.SINGLE));

            assertEquals(2, grid.exposedConnectors());
        }
    }
}
