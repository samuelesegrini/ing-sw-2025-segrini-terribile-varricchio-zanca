package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.GamePhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    @DisplayName("validation asks for a piece number, not a square")
    void keepTakesAPieceNumber() {
        String lines = joined(Help.during(GamePhase.VALIDATION));

        assertTrue(lines.contains("keep <n>"),
                "the handler reads one number and checks it against the list of pieces, so "
                        + "help asking for a row and a column sends the player straight to a "
                        + "refusal");
        assertFalse(lines.contains("keep <row> <col>"));
    }

    @Test
    @DisplayName("the one-line hint names everything the phase accepts")
    void theHintNamesEveryCommand() {
        for (GamePhase phase : List.of(GamePhase.VALIDATION, GamePhase.CREW_PLACEMENT)) {
            String hint = Help.oneLine(phase);
            for (Verb verb : Help.verbsDuring(phase)) {
                assertTrue(hint.contains(verb.form()),
                        phase + " accepts '" + verb.form() + "' and the hint a player gets "
                                + "after typing something else does not mention it: " + hint);
            }
        }
    }

    @Test
    @DisplayName("and the phase hints are built from it rather than written out again")
    void theHintsDelegate() {
        String source = sourceOf("TextInterface");

        for (GamePhase phase : List.of(GamePhase.VALIDATION, GamePhase.CREW_PLACEMENT)) {
            assertTrue(source.contains("Help.oneLine(GamePhase." + phase.name() + ")"),
                    "the hint for " + phase + " lists its commands by hand. That is how the "
                            + "validation hint came to name 'scrap' and not 'keep' — a player "
                            + "whose ship was in pieces was told about the one command that "
                            + "would not help. The test above cannot see a hand-written list, "
                            + "so this one looks for the call instead");
        }
    }

    private static String sourceOf(String className) {
        Path source = Path.of("src/main/java/it/polimi/ingsw/client/view/tui",
                className + ".java");
        try {
            return Files.readString(source);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("cannot read " + source, unreadable);
        }
    }

    private static String joined(List<String> lines) {
        return String.join("\n", lines);
    }
}
