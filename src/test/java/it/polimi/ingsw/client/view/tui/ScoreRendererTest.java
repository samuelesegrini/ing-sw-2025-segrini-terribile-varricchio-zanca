package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.ScoreSheet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the last thing a player reads is laid out straight.
 *
 * <p>The ledger is a table drawn with two format strings — one for the headings and one for the
 * numbers under them — which only line up while nobody changes a column. A heading wider than
 * the column it names does not truncate; it pushes every heading after it along, and the table
 * still looks like a table, so this is the kind of thing that ships.
 *
 * <p>Names are left out of these tests on purpose: with no players given, the renderer falls
 * back to the colour, which keeps the widths under the test's control.
 *
 * <p>Components involved: {@link ScoreRenderer}, {@link ScoreSheet}, {@link PlayerColor}.
 */
class ScoreRendererTest {

    /** The header sits third, after the blank line and the title. */
    private static final int HEADER = 2;

    private static List<String> ledger() {
        return ScoreRenderer.render(
                List.of(new ScoreSheet(PlayerColor.BLUE, true, 4, 2, 3, 5, 6),
                        new ScoreSheet(PlayerColor.GREEN, false, 0, 0, 0, 0, 7)),
                List.of());
    }

    @Test
    @DisplayName("every heading sits over the column it names")
    void theHeadingsLineUpWithTheNumbers() {
        List<String> ledger = ledger();
        String header = ledger.get(HEADER);

        for (int row = HEADER + 1; row < HEADER + 3; row++) {
            assertEquals(header.length(), ledger.get(row).length(),
                    "the headings and the numbers under them are formatted separately, so a "
                            + "heading too wide for its column shifts the rest of the row: "
                            + System.lineSeparator() + header + System.lineSeparator()
                            + ledger.get(row));
        }
    }

    @Test
    @DisplayName("and no two headings are run together")
    void theHeadingsAreSeparate() {
        String header = ledger().get(HEADER);

        for (String heading : List.of("player", "finish", "prettiest", "goods", "earned",
                "lost", "total")) {
            assertTrue(header.contains(" " + heading),
                    "'" + heading + "' has no space in front of it, so it has run into the "
                            + "heading before it: " + header);
        }
    }
}
