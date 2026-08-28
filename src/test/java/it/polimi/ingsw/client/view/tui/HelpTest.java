package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.GamePhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the command reference is about now.
 *
 * <p>A list of every command in the game would be a list a player has to filter in their head
 * while an hourglass runs. So it shows what is legal in the phase they are in, and the three
 * things that are always legal underneath.
 *
 * <p>Components involved: {@link Help}, {@link GamePhase}.
 */
class HelpTest {

    @Test
    @DisplayName("the lobby lists what you can do in a lobby")
    void inTheLobby() {
        List<String> lines = Help.inTheLobby();

        assertTrue(lines.contains("In the lobby"));
        assertTrue(joined(lines).contains("name <nickname>"));
        assertFalse(joined(lines).contains("weld"),
                "a player choosing a game does not need to be told how to weld");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(GamePhase.class)
    @DisplayName("every phase has a reference, and every reference has a way out")
    void everyPhase(GamePhase phase) {
        List<String> lines = Help.during(phase);

        assertTrue(lines.contains("Always"), phase + " leaves a player with nothing to type");
        assertTrue(joined(lines).contains("quit"));
        assertTrue(joined(lines).contains("help"));
    }

    @Test
    @DisplayName("building lists the shipyard, and only the shipyard")
    void building() {
        String lines = joined(Help.during(GamePhase.BUILDING));

        assertTrue(lines.contains("draw"));
        assertTrue(lines.contains("weld"));
        assertTrue(lines.contains("flip"));
        assertFalse(lines.contains("give up"), "there is no route to leave yet");
    }

    @Test
    @DisplayName("the flight lists the answers a card can want")
    void flight() {
        String lines = joined(Help.during(GamePhase.FLIGHT));

        assertTrue(lines.contains("power"));
        assertTrue(lines.contains("shield"));
        assertTrue(lines.contains("give up"));
        assertFalse(lines.contains("draw"), "the shipyard closed a while ago");
    }

    @Test
    @DisplayName("a phase with nothing to type still says what is always possible")
    void quietPhases() {
        List<String> lines = Help.during(GamePhase.SCORING);

        assertTrue(lines.contains("Always"));
        assertTrue(joined(lines).contains("look"),
                "the ledger is worth looking at even when there is nothing to do about it");
    }

    private static String joined(List<String> lines) {
        return String.join("\n", lines);
    }
}
