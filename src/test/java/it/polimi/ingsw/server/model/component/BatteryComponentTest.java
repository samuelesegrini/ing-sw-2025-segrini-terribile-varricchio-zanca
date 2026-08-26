package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Rotation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a battery compartment holds exactly what is printed on it and no more.
 *
 * <p>Charges are the scarce resource of the whole flight: every double cannon, double
 * engine and shield activation spends one, and nothing gives them back. A compartment
 * that could be over-spent or silently refilled would quietly break every attribute
 * declaration.
 *
 * <p>Components involved: {@link BatteryComponent}.
 */
class BatteryComponentTest {

    private static BatteryComponent battery(int capacity) {
        return new BatteryComponent(Tiles.of(ComponentKind.BATTERY, capacity), Rotation.NONE);
    }

    @Test
    @DisplayName("a compartment starts empty, because charges are added during launch preparation")
    void newCompartment_startsEmpty() {
        BatteryComponent battery = battery(3);

        assertEquals(3, battery.capacity());
        assertEquals(0, battery.charges());
        assertTrue(battery.isEmpty());
    }

    @Test
    @DisplayName("filling puts in exactly the printed number of charges")
    void filling_addsThePrintedCharges() {
        BatteryComponent battery = battery(2);
        battery.fill();

        assertEquals(2, battery.charges());
        assertFalse(battery.isEmpty());
    }

    @Test
    @DisplayName("filling twice does not stack, so a compartment can never exceed its capacity")
    void fillingTwice_doesNotStack() {
        BatteryComponent battery = battery(2);
        battery.fill();
        battery.fill();

        assertEquals(2, battery.charges());
    }

    @Test
    @DisplayName("each activation spends one charge and there is no way to get it back")
    void spending_consumesOneChargeAtATime() {
        BatteryComponent battery = battery(3);
        battery.fill();

        battery.spend();
        assertEquals(2, battery.charges());
        battery.spend();
        battery.spend();
        assertTrue(battery.isEmpty());
    }

    @Test
    @DisplayName("spending from an empty compartment is refused rather than going negative")
    void spendingWhenEmpty_isRefused() {
        BatteryComponent battery = battery(2);

        assertThrows(IllegalStateException.class, battery::spend);
    }

    @Test
    @DisplayName("draining returns the charges to the bank, as happens when the component is destroyed")
    void draining_emptiesTheCompartment() {
        BatteryComponent battery = battery(3);
        battery.fill();
        battery.drain();

        assertTrue(battery.isEmpty());
    }
}
