package it.polimi.ingsw.server.model.ship;

import it.polimi.ingsw.server.model.board.ShipBoardSpec;
import it.polimi.ingsw.server.model.component.ComponentKind;
import it.polimi.ingsw.server.model.component.ComponentTile;
import it.polimi.ingsw.server.model.component.ShipComponent;
import it.polimi.ingsw.server.model.component.Tiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the assembly rules of manual p.8, the ones other players would run through by
 * eye and the application runs instead ({@code requirements.pdf} § 2.1).
 *
 * <p>The case worth naming is two smooth sides touching. It is legal, it is common, and
 * a validator written from "all touching sides must connect" would reject it — which
 * would make perfectly good ships illegal and cost their owners components they never
 * had to lose.
 *
 * <p>Components involved: {@link ShipValidator}, {@link ShipGrid}, {@link ValidationReport}.
 */
class ShipValidatorTest {

    private static final Position CENTRE = new Position(1, 1);

    private final ShipGrid grid = new ShipGrid(new ShipBoardSpec(3, 3, 5, 4, CENTRE, 0, Set.of()));

    private ValidationReport validate() {
        return new ShipValidator(grid).validate();
    }

    private void put(Position cell, ShipComponent component) {
        grid.put(cell, component);
    }

    /** A structural module with the four sides given in north, east, south, west order. */
    private static ShipComponent structural(Connector north, Connector east, Connector south, Connector west) {
        return ShipComponent.place(new ComponentTile("hull", ComponentKind.STRUCTURAL_MODULE,
                Tiles.sides(north, east, south, west), 0), Rotation.NONE);
    }

    private static ShipComponent universal() {
        return ShipComponent.place(Tiles.of(ComponentKind.STRUCTURAL_MODULE), Rotation.NONE);
    }

    private static ShipComponent engine(Rotation rotation) {
        return ShipComponent.place(Tiles.of(ComponentKind.SINGLE_ENGINE), rotation);
    }

    private static ShipComponent cannon(Rotation rotation) {
        return ShipComponent.place(Tiles.of(ComponentKind.SINGLE_CANNON), rotation);
    }

    @Nested
    @DisplayName("joints between touching sides")
    class Joints {

        @Test
        @DisplayName("an empty ship breaks no rule")
        void emptyShip_isLegal() {
            assertTrue(validate().isLegal());
        }

        @Test
        @DisplayName("two universal connectors facing each other are a good joint")
        void weldedComponents_areLegal() {
            put(CENTRE, universal());
            put(new Position(0, 1), universal());

            assertTrue(validate().isLegal());
        }

        @Test
        @DisplayName("two smooth sides touching is legal, and a validator must not mistake it for a broken joint")
        void twoSmoothSidesTouching_areLegal() {
            put(CENTRE, structural(Connector.PLAIN, Connector.UNIVERSAL,
                    Connector.UNIVERSAL, Connector.UNIVERSAL));
            put(new Position(1, 0), structural(Connector.UNIVERSAL, Connector.UNIVERSAL,
                    Connector.UNIVERSAL, Connector.UNIVERSAL));
            put(new Position(0, 1), structural(Connector.UNIVERSAL, Connector.UNIVERSAL,
                    Connector.PLAIN, Connector.UNIVERSAL));

            ValidationReport report = validate();

            assertFalse(report.hasViolationOf(ViolationKind.CONNECTOR_MEETS_SMOOTH_SIDE));
            assertFalse(report.hasViolationOf(ViolationKind.INCOMPATIBLE_CONNECTORS));
        }

        @Test
        @DisplayName("a single connector facing a double one is reported, naming both pieces")
        void singleFacingDouble_isReported() {
            put(CENTRE, structural(Connector.SINGLE, Connector.PLAIN, Connector.PLAIN, Connector.PLAIN));
            put(new Position(0, 1), structural(Connector.PLAIN, Connector.PLAIN,
                    Connector.DOUBLE, Connector.PLAIN));

            ValidationReport report = validate();

            assertTrue(report.hasViolationOf(ViolationKind.INCOMPATIBLE_CONNECTORS));
            assertEquals(Set.of(CENTRE, new Position(0, 1)),
                    report.violations().stream()
                            .filter(v -> v.kind() == ViolationKind.INCOMPATIBLE_CONNECTORS)
                            .findFirst().orElseThrow().cells());
        }

        @Test
        @DisplayName("a connector pressed against a smooth side is reported as its own kind of mistake")
        void connectorAgainstSmoothSide_isReported() {
            put(CENTRE, structural(Connector.SINGLE, Connector.PLAIN, Connector.PLAIN, Connector.PLAIN));
            put(new Position(0, 1), structural(Connector.PLAIN, Connector.PLAIN,
                    Connector.PLAIN, Connector.PLAIN));

            ValidationReport report = validate();

            assertTrue(report.hasViolationOf(ViolationKind.CONNECTOR_MEETS_SMOOTH_SIDE));
            assertFalse(report.hasViolationOf(ViolationKind.INCOMPATIBLE_CONNECTORS));
        }

        @Test
        @DisplayName("one bad joint is reported once, not once from each of its two ends")
        void oneBadJoint_isReportedOnce() {
            put(CENTRE, structural(Connector.SINGLE, Connector.PLAIN, Connector.PLAIN, Connector.PLAIN));
            put(new Position(0, 1), structural(Connector.PLAIN, Connector.PLAIN,
                    Connector.DOUBLE, Connector.PLAIN));

            List<ShipViolation> mismatches = validate().violations().stream()
                    .filter(v -> v.kind() == ViolationKind.INCOMPATIBLE_CONNECTORS)
                    .toList();

            assertEquals(1, mismatches.size());
        }
    }

    @Nested
    @DisplayName("engines")
    class Engines {

        @Test
        @DisplayName("an engine firing off the stern edge of the board is legal")
        void engineFiringOffTheBoard_isLegal() {
            put(new Position(2, 1), engine(Rotation.NONE));

            assertTrue(validate().isLegal());
        }

        @Test
        @DisplayName("an engine turned sideways is illegal however clear its path is")
        void sidewaysEngine_isReported() {
            put(CENTRE, engine(Rotation.CLOCKWISE_90));

            ValidationReport report = validate();

            assertTrue(report.hasViolationOf(ViolationKind.ENGINE_NOT_FACING_STERN));
            assertFalse(report.hasViolationOf(ViolationKind.BLOCKED_ENGINE_EXHAUST));
        }

        @Test
        @DisplayName("a component in the cell an engine fires into is reported, naming both")
        void blockedExhaust_isReported() {
            put(CENTRE, engine(Rotation.NONE));
            put(new Position(2, 1), universal());

            ValidationReport report = validate();

            assertTrue(report.hasViolationOf(ViolationKind.BLOCKED_ENGINE_EXHAUST));
            assertEquals(Set.of(CENTRE, new Position(2, 1)), report.offendingCells());
        }

        @Test
        @DisplayName("a sideways engine with something in its way is two mistakes, because fixing one leaves the ship illegal")
        void sidewaysAndBlockedEngine_isTwoViolations() {
            put(CENTRE, engine(Rotation.CLOCKWISE_90));
            put(new Position(1, 0), universal());

            ValidationReport report = validate();

            assertTrue(report.hasViolationOf(ViolationKind.ENGINE_NOT_FACING_STERN));
            assertTrue(report.hasViolationOf(ViolationKind.BLOCKED_ENGINE_EXHAUST));
        }
    }

    @Nested
    @DisplayName("cannons")
    class Cannons {

        @Test
        @DisplayName("a cannon may point any way it likes, forward is merely worth more")
        void cannonPointingAnyWay_isLegal() {
            put(CENTRE, cannon(Rotation.CLOCKWISE_180));

            assertTrue(validate().isLegal());
        }

        @Test
        @DisplayName("a component in front of a muzzle is reported, naming both")
        void blockedMuzzle_isReported() {
            put(CENTRE, cannon(Rotation.NONE));
            put(new Position(0, 1), universal());

            ValidationReport report = validate();

            assertTrue(report.hasViolationOf(ViolationKind.BLOCKED_CANNON_MUZZLE));
            assertEquals(Set.of(CENTRE, new Position(0, 1)), report.offendingCells());
        }

        @Test
        @DisplayName("a cannon firing off the edge of the board is legal")
        void cannonFiringOffTheBoard_isLegal() {
            put(new Position(0, 1), cannon(Rotation.NONE));

            assertTrue(validate().isLegal());
        }
    }

    @Nested
    @DisplayName("connectivity")
    class Connectivity {

        @Test
        @DisplayName("a component welded to nothing is reported as stranded")
        void strandedComponent_isReported() {
            put(CENTRE, universal());
            put(new Position(0, 1), universal());
            put(new Position(2, 0), universal());

            ValidationReport report = validate();

            assertTrue(report.hasViolationOf(ViolationKind.DISCONNECTED));
            assertEquals(Set.of(new Position(2, 0)), report.offendingCells());
        }

        @Test
        @DisplayName("two loose fragments are reported separately, so neither hides behind the other")
        void eachStrandedFragment_isReportedSeparately() {
            put(CENTRE, universal());
            put(new Position(0, 1), universal());
            put(new Position(2, 0), universal());
            put(new Position(2, 2), universal());

            List<ShipViolation> stranded = validate().violations().stream()
                    .filter(v -> v.kind() == ViolationKind.DISCONNECTED)
                    .toList();

            assertEquals(2, stranded.size());
            assertEquals(Set.of(new Position(2, 0), new Position(2, 2)),
                    stranded.stream().flatMap(v -> v.cells().stream()).collect(java.util.stream.Collectors.toSet()));
        }

        @Test
        @DisplayName("components welded only to each other are one fragment, not several")
        void componentsWeldedToEachOther_areOneFragment() {
            put(new Position(0, 0), universal());
            put(new Position(0, 1), universal());
            put(new Position(1, 0), universal());

            put(new Position(2, 1), universal());
            put(new Position(2, 2), universal());

            List<ShipViolation> stranded = validate().violations().stream()
                    .filter(v -> v.kind() == ViolationKind.DISCONNECTED)
                    .toList();

            assertEquals(1, stranded.size());
            assertEquals(Set.of(new Position(2, 1), new Position(2, 2)), stranded.getFirst().cells());
        }

        @Test
        @DisplayName("a piece attached only along smooth sides is stranded, because touching is not welding")
        void pieceTouchingOnlyAlongSmoothSides_isStranded() {
            put(CENTRE, structural(Connector.PLAIN, Connector.UNIVERSAL,
                    Connector.UNIVERSAL, Connector.UNIVERSAL));
            put(new Position(0, 1), structural(Connector.UNIVERSAL, Connector.UNIVERSAL,
                    Connector.PLAIN, Connector.UNIVERSAL));

            ValidationReport report = validate();

            assertTrue(report.hasViolationOf(ViolationKind.DISCONNECTED));
            assertFalse(report.hasViolationOf(ViolationKind.CONNECTOR_MEETS_SMOOTH_SIDE));
        }
    }

    @Nested
    @DisplayName("the report itself")
    class Report {

        @Test
        @DisplayName("every violation names at least one cell, so a player always knows what to remove")
        void everyViolation_namesTheCellsToRemove() {
            put(CENTRE, engine(Rotation.CLOCKWISE_90));
            put(new Position(1, 0), universal());
            put(new Position(0, 0), structural(Connector.SINGLE, Connector.PLAIN,
                    Connector.PLAIN, Connector.PLAIN));

            ValidationReport report = validate();

            assertFalse(report.isLegal());
            report.violations().forEach(violation ->
                    assertFalse(violation.cells().isEmpty(), violation.description()));
            assertTrue(report.offendingCells().contains(CENTRE));
        }

        @Test
        @DisplayName("a legal report is empty and names no cells")
        void legalReport_isEmpty() {
            ValidationReport report = ValidationReport.legal();

            assertTrue(report.isLegal());
            assertEquals(Set.of(), report.offendingCells());
            assertFalse(report.hasViolationOf(ViolationKind.DISCONNECTED));
        }
    }
}
