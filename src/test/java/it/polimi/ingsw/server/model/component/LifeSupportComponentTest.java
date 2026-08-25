package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.server.model.crew.AlienColor;
import it.polimi.ingsw.server.model.ship.Rotation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks that a life support module reports the species it keeps alive.
 *
 * <p>Components involved: {@link LifeSupportComponent}, {@link AlienColor}.
 */
class LifeSupportComponentTest {

    @Test
    @DisplayName("each life support module supports the alien of its own colour")
    void module_supportsItsOwnColour() {
        LifeSupportComponent purple =
                new LifeSupportComponent(Tiles.of(ComponentKind.PURPLE_LIFE_SUPPORT), Rotation.NONE);
        LifeSupportComponent brown =
                new LifeSupportComponent(Tiles.of(ComponentKind.BROWN_LIFE_SUPPORT), Rotation.NONE);

        assertEquals(AlienColor.PURPLE, purple.supports());
        assertEquals(AlienColor.BROWN, brown.supports());
    }

    @Test
    @DisplayName("each alien colour names the module it needs, which is what makes a cabin habitable")
    void alienColour_namesItsModule() {
        assertEquals(ComponentKind.PURPLE_LIFE_SUPPORT, AlienColor.PURPLE.lifeSupport());
        assertEquals(ComponentKind.BROWN_LIFE_SUPPORT, AlienColor.BROWN.lifeSupport());
    }

    @Test
    @DisplayName("asking which alien a cabin supports is a programming error, not a rules question")
    void nonLifeSupportKind_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> AlienColor.supportedBy(ComponentKind.CABIN));
    }
}
