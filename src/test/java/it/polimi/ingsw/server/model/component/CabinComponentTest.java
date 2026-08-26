package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Rotation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the capacity of a single cabin against manual p.18.
 *
 * <p>An alien displaces both humans but counts as one crew member, not two. That
 * asymmetry is what makes aliens a real trade-off, and getting it wrong would shift
 * every combat zone crew comparison and every abandoned station requirement.
 *
 * <p>Components involved: {@link CabinComponent}, {@link AlienColor}.
 */
class CabinComponentTest {

    private static CabinComponent cabin() {
        return new CabinComponent(Tiles.of(ComponentKind.CABIN), Rotation.NONE);
    }

    private static CabinComponent startingCabin() {
        return new CabinComponent(Tiles.startingCabin(PlayerColor.GREEN), Rotation.NONE);
    }

    @Test
    @DisplayName("a cabin starts empty, since crew boards during launch preparation")
    void newCabin_startsEmpty() {
        CabinComponent cabin = cabin();

        assertTrue(cabin.isEmpty());
        assertEquals(0, cabin.crewCount());
        assertEquals(Optional.empty(), cabin.alien());
    }

    @Test
    @DisplayName("boarding humans fills the cabin with two of them")
    void boardingHumans_putsTwoAboard() {
        CabinComponent cabin = cabin();
        cabin.boardHumans();

        assertEquals(2, cabin.humans());
        assertEquals(2, cabin.crewCount());
    }

    @Test
    @DisplayName("an alien takes the space of two humans but counts as one crew member")
    void alien_displacesTwoHumansAndCountsAsOne() {
        CabinComponent cabin = cabin();
        cabin.boardAlien(AlienColor.PURPLE);

        assertEquals(0, cabin.humans());
        assertEquals(Optional.of(AlienColor.PURPLE), cabin.alien());
        assertEquals(1, cabin.crewCount());
    }

    @Test
    @DisplayName("the starting cabin never takes an alien, whatever life support is attached to it")
    void startingCabin_refusesAliens() {
        CabinComponent starting = startingCabin();

        assertFalse(starting.canHostAlien());
        assertThrows(IllegalStateException.class, () -> starting.boardAlien(AlienColor.BROWN));

        starting.boardHumans();
        assertEquals(2, starting.humans());
    }

    @Test
    @DisplayName("a cabin that already has crew cannot take more, so nobody is double-boarded")
    void occupiedCabin_refusesMoreCrew() {
        CabinComponent cabin = cabin();
        cabin.boardHumans();

        assertThrows(IllegalStateException.class, cabin::boardHumans);
        assertThrows(IllegalStateException.class, () -> cabin.boardAlien(AlienColor.BROWN));
    }

    @Test
    @DisplayName("crew is given up one at a time, and the player chooses humans or the alien")
    void crewIsRemovedOneAtATime() {
        CabinComponent cabin = cabin();
        cabin.boardHumans();

        cabin.removeOne(false);
        assertEquals(1, cabin.crewCount());
        cabin.removeOne(false);
        assertTrue(cabin.isEmpty());
        assertThrows(IllegalStateException.class, () -> cabin.removeOne(false));
    }

    @Test
    @DisplayName("asking a human-crewed cabin to give up an alien is refused")
    void removingAnAbsentAlien_isRefused() {
        CabinComponent cabin = cabin();
        cabin.boardHumans();

        assertThrows(IllegalStateException.class, () -> cabin.removeOne(true));
    }

    @Test
    @DisplayName("evacuating empties the cabin, as when its life support is destroyed")
    void evacuating_emptiesTheCabin() {
        CabinComponent cabin = cabin();
        cabin.boardAlien(AlienColor.BROWN);
        cabin.evacuate();

        assertTrue(cabin.isEmpty());
        assertEquals(Optional.empty(), cabin.alien());
    }
}
