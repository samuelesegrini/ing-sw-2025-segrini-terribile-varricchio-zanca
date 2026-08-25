package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.server.model.ship.Direction;
import it.polimi.ingsw.server.model.ship.Rotation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a shield covers the pair of sides its rotation puts it on.
 *
 * <p>Which sides a shield covers decides whether a small meteor or a light shot gets
 * through, so this is the rule the whole damage model leans on.
 *
 * <p>Components involved: {@link ShieldComponent}.
 */
class ShieldComponentTest {

    private static ShieldComponent shield(Rotation rotation) {
        return new ShieldComponent(Tiles.of(ComponentKind.SHIELD), rotation);
    }

    @Test
    @DisplayName("an unrotated shield covers north and east, the pair the artwork prints")
    void unrotatedShield_coversNorthAndEast() {
        assertEquals(Set.of(Direction.NORTH, Direction.EAST), shield(Rotation.NONE).covered());
    }

    @Test
    @DisplayName("rotating a shield moves both covered sides together")
    void rotation_movesBothCoveredSides() {
        assertEquals(Set.of(Direction.EAST, Direction.SOUTH), shield(Rotation.CLOCKWISE_90).covered());
        assertEquals(Set.of(Direction.SOUTH, Direction.WEST), shield(Rotation.CLOCKWISE_180).covered());
        assertEquals(Set.of(Direction.WEST, Direction.NORTH), shield(Rotation.CLOCKWISE_270).covered());
    }

    @Test
    @DisplayName("a shield covers exactly two of the four sides, so two shields are needed for a whole ship")
    void aShield_leavesTwoSidesOpen() {
        ShieldComponent shield = shield(Rotation.NONE);

        assertTrue(shield.covers(Direction.NORTH));
        assertTrue(shield.covers(Direction.EAST));
        assertFalse(shield.covers(Direction.SOUTH));
        assertFalse(shield.covers(Direction.WEST));
    }
}
