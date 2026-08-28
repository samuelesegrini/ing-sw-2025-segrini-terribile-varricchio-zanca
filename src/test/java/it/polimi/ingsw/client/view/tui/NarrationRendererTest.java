package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.AdventureCardType;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.common.game.DamageReport;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.Hit;
import it.polimi.ingsw.common.game.HitKind;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.FlightEvent;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.LobbyEvent;
import it.polimi.ingsw.common.protocol.Messages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a player can follow a game by reading it.
 *
 * <p>Half of what makes this game worth playing is watching a seven come up on somebody else's
 * meteor, and a board state cannot tell you that. So every event that says something has to say
 * it in words — and the test at the bottom is the one that matters: it walks every message the
 * protocol defines and insists that anything worth printing prints.
 */
class NarrationRendererTest {

    private static String rendered(Event event) {
        return NarrationRenderer.render(event).orElseThrow(
                () -> new AssertionError(event.getClass().getSimpleName() + " says nothing"));
    }

    @Test
    @DisplayName("the lobby says who arrived and who left")
    void lobbyEvents() {
        assertEquals("  you are samuele", rendered(new LobbyEvent.LoggedIn("samuele")));
        assertEquals("  you have a seat at game-1 as RED",
                rendered(new LobbyEvent.JoinedGame("game-1", PlayerColor.RED)));
        assertEquals("  chiara sat down (BLUE)",
                rendered(new LobbyEvent.PlayerEntered("chiara", PlayerColor.BLUE)));
        assertEquals("  chiara left", rendered(new LobbyEvent.PlayerLeft("chiara")));
    }

    @Test
    @DisplayName("a connection changing says the same thing whether somebody is new or returning")
    void connections() {
        // The same event announces a player arriving for the first time and one coming back
        // after their laptop closed. "is back" reads oddly for somebody who has never been away.
        assertEquals("  RED is connected",
                rendered(new GameEvent.ConnectionChanged(PlayerColor.RED, true)));
        assertEquals("  RED has dropped",
                rendered(new GameEvent.ConnectionChanged(PlayerColor.RED, false)));
    }

    @Test
    @DisplayName("a refusal is printed, because a client that swallows one has hung")
    void refusals() {
        assertEquals("  ✗ there is nothing waiting to be welded",
                rendered(new GameEvent.Rejected("Weld", "there is nothing waiting to be welded")));
    }

    @Test
    @DisplayName("a phase change is marked, because it is when a view has to restructure")
    void phases() {
        assertEquals("── building — build a ship out of what is on the table",
                rendered(new GameEvent.PhaseBegan(GamePhase.BUILDING)));
        assertEquals("── crew placement — put people and aliens in the cabins",
                rendered(new GameEvent.PhaseBegan(GamePhase.CREW_PLACEMENT)));
        assertEquals("── the game is over", rendered(new GameEvent.GameEnded()));
    }

    @ParameterizedTest
    @EnumSource(GamePhase.class)
    @DisplayName("and says what the phase is for, for every phase there is")
    void everyPhaseSaysWhatItIsFor(GamePhase phase) {
        String line = rendered(new GameEvent.PhaseBegan(phase));

        // The name alone is what this replaced: it told a player something had changed and
        // nothing about what was now expected of them. Driven off values(), so a phase added
        // later fails here as well as failing to compile.
        assertTrue(line.startsWith("── "), line);
        assertTrue(line.contains(" — "), "no clause after the rule: " + line);
        assertFalse(NarrationRenderer.purposeOf(phase).isBlank());
    }

    @Test
    @DisplayName("a game beginning names itself, its rules and the table")
    void aGameBeginningNamesTheTable() {
        String line = NarrationRenderer.gameBegan(Messages.state());

        assertTrue(line.contains("game-1"), line);
        assertTrue(line.contains("level ii"), line);
        assertTrue(line.contains("samuele"), line);
        assertTrue(line.contains("chiara"), line);
    }

    @Test
    @DisplayName("and marks which of them is the player reading it")
    void theTableSaysWhichOneIsYou() {
        String line = NarrationRenderer.gameBegan(Messages.state());

        // A player who joined a game somebody else opened has no other way to tell.
        assertTrue(line.contains("(RED, you)"), line);
        assertFalse(line.contains("(BLUE, you)"), line);
    }

    @Test
    @DisplayName("the flight is told as it happens")
    void flightEvents() {
        assertEquals("  a card is turned over: pirates",
                rendered(new FlightEvent.CardRevealed(new AdventureCardIdentity(
                        "pirates_lvl2", AdventureCardType.PIRATES, CardLevel.LEVEL_II, false))));
        assertEquals("  the dice come up 7", rendered(new FlightEvent.DiceRolled(7)));
        assertEquals("  RED moves from 6 to 9",
                rendered(new FlightEvent.ShipMoved(PlayerColor.RED, 6, 9)));
        assertEquals("  BLUE is out of the flight: a whole lap behind the leader",
                rendered(new FlightEvent.ShipRetired(PlayerColor.BLUE,
                        "a whole lap behind the leader")));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DamageReport.Outcome.class)
    @DisplayName("every way a shot can end is said differently")
    void everyOutcome(DamageReport.Outcome outcome) {
        String line = rendered(new FlightEvent.ThreatResolved(PlayerColor.RED,
                new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 7),
                new DamageReport(new Position(2, 3), outcome, null, List.of())));

        assertTrue(line.startsWith("  heavy fire from the north at RED: "));
        assertFalse(line.endsWith(": "), outcome + " needs something said about it");
    }

    /**
     * The events that are meant to say nothing, and why.
     *
     * <p>Named here rather than left to be inferred from a missing case, so that an event added
     * without a line fails this test instead of quietly never reaching the player.
     */
    private static final Map<Class<? extends Event>, String> DELIBERATELY_SILENT = Map.of(
            GameEvent.StateChanged.class, "the board says it, and narrating every state would "
                    + "bury the events worth reading",
            LobbyEvent.GamesListed.class, "printed by the command that asked for it, in a table",
            FlightEvent.CardResolved.class, "the next card is turned over in the same breath, and "
                    + "saying a card is finished with adds nothing to seeing the next one");

    @Test
    @DisplayName("every message either says something or is on the list of ones that should not")
    void everyMessageIsAccountedFor() {
        // The hole this closes is the one where a message is added, nobody writes a line for it,
        // and a player watching the game simply never hears about it.
        List<String> unaccountedFor = Messages.events().stream()
                .filter(event -> NarrationRenderer.render(event).isEmpty())
                .filter(event -> !DELIBERATELY_SILENT.containsKey(event.getClass()))
                .map(event -> event.getClass().getSimpleName())
                .toList();

        assertEquals(List.of(), unaccountedFor,
                "these arrive at a client and are never mentioned to the player");
    }

    @Test
    @DisplayName("the ones that should say nothing, say nothing")
    void silenceIsKept() {
        assertEquals(Optional.empty(),
                NarrationRenderer.render(new GameEvent.StateChanged(Messages.state())));
        assertEquals(Optional.empty(),
                NarrationRenderer.render(new FlightEvent.CardResolved()));
        assertEquals(Optional.empty(),
                NarrationRenderer.render(new LobbyEvent.GamesListed(List.of())));
    }

    @Test
    @DisplayName("waiting on somebody is said, so a game never looks like it has hung")
    void waiting() {
        assertEquals("  waiting for RED",
                rendered(new FlightEvent.Awaiting(Messages.prompts().get(0))));
    }

}
