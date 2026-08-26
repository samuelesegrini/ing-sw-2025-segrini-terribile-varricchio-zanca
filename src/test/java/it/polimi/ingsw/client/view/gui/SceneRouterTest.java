package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.view.GameView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a player is always looking at the right window.
 *
 * <p>Routing is the piece of a graphical client that goes wrong quietly: a screen that lingers
 * a phase too long leaves somebody staring at a shipyard that closed, and nothing throws. It is
 * also the piece that can be checked exhaustively without a screen, which is what this does —
 * every phase the game has, not the four somebody remembered.
 */
class SceneRouterTest {

    private static GameView inPhase(GamePhase phase) {
        return new GameView("game-1", GameLevel.LEVEL_II, phase, PlayerColor.RED,
                List.of(), null, null, null, List.of());
    }

    @Test
    @DisplayName("somebody who has not said who they are is asked")
    void beforeLoggingIn() {
        assertEquals(Screen.LOGIN, SceneRouter.screenFor(Optional.empty(), Optional.empty()));
    }

    @Test
    @DisplayName("a player with a name but no game is choosing a table")
    void inTheLobby() {
        assertEquals(Screen.LOBBY,
                SceneRouter.screenFor(Optional.of("samuele"), Optional.empty()));
    }

    @ParameterizedTest
    @EnumSource(GamePhase.class)
    @DisplayName("every phase the game has routes somewhere, including the ones nobody remembers")
    void everyPhaseRoutes(GamePhase phase) {
        Screen screen = SceneRouter.screenFor(Optional.of("samuele"), Optional.of(inPhase(phase)));

        assertNotNull(screen);
        assertNotEqualsLobbyOrLogin(screen, phase);
    }

    private static void assertNotEqualsLobbyOrLogin(Screen screen, GamePhase phase) {
        assertTrue(screen != Screen.LOGIN && screen != Screen.LOBBY,
                "a player who is in a game is not in the lobby, even during " + phase);
    }

    @Test
    @DisplayName("each phase goes where the work is, which is not one screen per phase")
    void thePhases() {
        assertEquals(Screen.SHIPYARD, screenIn(GamePhase.BUILDING));
        assertEquals(Screen.REPAIRS, screenIn(GamePhase.VALIDATION));
        assertEquals(Screen.CREW, screenIn(GamePhase.CREW_PLACEMENT));
        assertEquals(Screen.FLIGHT, screenIn(GamePhase.FLIGHT));
        assertEquals(Screen.LEDGER, screenIn(GamePhase.SCORING),
                "the scores are being worked out, and that is what a player wants to watch");
        assertEquals(Screen.LEDGER, screenIn(GamePhase.FINISHED));
    }

    @Test
    @DisplayName("a game still gathering players shows the shipyard, not the lobby")
    void beforeBuildingStarts() {
        // They have a seat. Sending them back to the list of tables would suggest they had not.
        assertEquals(Screen.SHIPYARD, screenIn(GamePhase.LOBBY));
    }

    @Test
    @DisplayName("the screens around the ship keep the board underneath as the job changes")
    void keepingTheBoard() {
        assertTrue(SceneRouter.keepsTheBoard(Screen.SHIPYARD, Screen.REPAIRS));
        assertTrue(SceneRouter.keepsTheBoard(Screen.REPAIRS, Screen.CREW));
        assertFalse(SceneRouter.keepsTheBoard(Screen.CREW, Screen.FLIGHT),
                "the route is a new picture and should arrive as one");
        assertFalse(SceneRouter.keepsTheBoard(Screen.FLIGHT, Screen.LEDGER));
    }

    @ParameterizedTest
    @EnumSource(Screen.class)
    @DisplayName("every screen has a title a player would recognise")
    void everyScreenIsNamed(Screen screen) {
        String title = SceneRouter.titleOf(screen);

        assertNotNull(title);
        assertFalse(title.isBlank(), screen + " has no title");
    }

    private static Screen screenIn(GamePhase phase) {
        return SceneRouter.screenFor(Optional.of("samuele"), Optional.of(inPhase(phase)));
    }
}
