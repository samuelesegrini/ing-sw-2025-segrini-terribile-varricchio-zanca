package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.server.model.ship.Direction;
import it.polimi.ingsw.server.model.ship.Rotation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the two attribute-bearing components against manual p.11.
 *
 * <p>Firepower is counted in halves throughout, because the manual insists that 5½
 * beats 5 and loses to 6. A side-facing single cannon is worth exactly one half, so
 * anything that rounded would change who takes the cannon fire in a combat zone.
 *
 * <p>Components involved: {@link CannonComponent}, {@link EngineComponent}.
 */
class CannonAndEngineTest {

    private static CannonComponent cannon(ComponentKind kind, Rotation rotation) {
        return new CannonComponent(Tiles.of(kind), rotation);
    }

    private static EngineComponent engine(ComponentKind kind, Rotation rotation) {
        return new EngineComponent(Tiles.of(kind), rotation);
    }

    @DisplayName("firepower in halves, by cannon kind, facing and whether a battery is spent")
    @ParameterizedTest(name = "{0} facing {1}, powered={2} is worth {3} halves")
    @CsvSource({
            // A single cannon never needs a battery and is always counted at full value forward.
            "SINGLE_CANNON, NONE,          false, 2",
            "SINGLE_CANNON, NONE,          true,  2",
            "SINGLE_CANNON, CLOCKWISE_90,  false, 1",
            "SINGLE_CANNON, CLOCKWISE_180, false, 1",
            "SINGLE_CANNON, CLOCKWISE_270, false, 1",
            // A double cannon is worth nothing at all without a charge.
            "DOUBLE_CANNON, NONE,          false, 0",
            "DOUBLE_CANNON, NONE,          true,  4",
            "DOUBLE_CANNON, CLOCKWISE_90,  true,  2",
            "DOUBLE_CANNON, CLOCKWISE_180, true,  2",
            "DOUBLE_CANNON, CLOCKWISE_270, false, 0"
    })
    void firepower_countedInHalves(ComponentKind kind, Rotation rotation, boolean powered, int expectedHalves) {
        assertEquals(expectedHalves, cannon(kind, rotation).firepowerHalves(powered));
    }

    @Test
    @DisplayName("a side-facing single cannon is worth exactly one half, which no rounding may absorb")
    void sideFacingSingleCannon_isWorthOneHalf() {
        CannonComponent side = cannon(ComponentKind.SINGLE_CANNON, Rotation.CLOCKWISE_90);

        assertEquals(1, side.firepowerHalves(false));
        assertFalse(side.facesForward());
        assertEquals(Direction.EAST, side.muzzleDirection());
    }

    @Test
    @DisplayName("only double cannons cost a charge, so a single one is counted whether the player likes it or not")
    void onlyDoubleCannons_needACharge() {
        assertFalse(cannon(ComponentKind.SINGLE_CANNON, Rotation.NONE).needsCharge());
        assertTrue(cannon(ComponentKind.DOUBLE_CANNON, Rotation.NONE).needsCharge());
    }

    @DisplayName("engine power, by engine kind and whether a battery is spent")
    @ParameterizedTest(name = "{0} powered={1} is worth {2}")
    @CsvSource({
            "SINGLE_ENGINE, false, 1",
            "SINGLE_ENGINE, true,  1",
            "DOUBLE_ENGINE, false, 0",
            "DOUBLE_ENGINE, true,  2"
    })
    void enginePower_dependsOnTheCharge(ComponentKind kind, boolean powered, int expected) {
        assertEquals(expected, engine(kind, Rotation.NONE).power(powered));
    }

    @Test
    @DisplayName("an engine's exhaust follows its rotation, which is what makes an illegal engine detectable")
    void engineExhaust_followsRotation() {
        assertEquals(Direction.SOUTH, engine(ComponentKind.SINGLE_ENGINE, Rotation.NONE).exhaustDirection());
        assertEquals(Direction.WEST, engine(ComponentKind.SINGLE_ENGINE, Rotation.CLOCKWISE_90).exhaustDirection());
        assertEquals(Direction.NORTH, engine(ComponentKind.SINGLE_ENGINE, Rotation.CLOCKWISE_180).exhaustDirection());
    }
}
