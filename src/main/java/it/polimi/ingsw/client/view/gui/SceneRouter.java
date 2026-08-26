package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.protocol.view.GameView;

import java.util.Optional;

/**
 * Which screen belongs to what the client currently knows.
 *
 * <p>A pure function of the projection, and deliberately not a method on a controller. Routing
 * is the one piece of a graphical client that is easy to get wrong in a way nobody notices —
 * a screen that lingers a phase too long, a player left looking at a shipyard that closed — and
 * it is the one piece that can be checked exhaustively on a machine with no screen at all.
 *
 * <p>Nothing here asks what is currently displayed. The screen follows from the state, so a
 * client that reconnects halfway through a flight lands on the flight, and one that missed the
 * launch catches up rather than waiting for an event it will never be sent again.
 */
public final class SceneRouter {

    private SceneRouter() {
    }

    /**
     * Works out where a player should be.
     *
     * @param nickname who they are, empty until the server agrees they are somebody
     * @param game     the game they are in, empty in the lobby
     * @return the screen to show
     */
    public static Screen screenFor(Optional<String> nickname, Optional<GameView> game) {
        if (nickname.isEmpty()) {
            return Screen.LOGIN;
        }
        return game.map(SceneRouter::duringAGame).orElse(Screen.LOBBY);
    }

    private static Screen duringAGame(GameView game) {
        return switch (game.phase()) {
            case LOBBY, BUILDING -> Screen.SHIPYARD;
            case VALIDATION -> Screen.REPAIRS;
            case CREW_PLACEMENT -> Screen.CREW;
            case FLIGHT -> Screen.FLIGHT;
            case SCORING, FINISHED -> Screen.LEDGER;
        };
    }

    /**
     * Tells whether moving between two screens should keep what the player was looking at.
     *
     * <p>The shipyard, the repairs and the crew screens are all the same board with different
     * things to do to it, so replacing the whole window between them throws away the thing the
     * player is actually reading. The flight and the ledger are new pictures and should arrive
     * as new pictures.
     *
     * @param from where they were
     * @param to   where they are going
     * @return {@code true} when the board should stay put underneath
     */
    public static boolean keepsTheBoard(Screen from, Screen to) {
        return aroundTheShip(from) && aroundTheShip(to);
    }

    private static boolean aroundTheShip(Screen screen) {
        return screen == Screen.SHIPYARD || screen == Screen.REPAIRS || screen == Screen.CREW;
    }

    /**
     * Names a screen the way a player would.
     *
     * @param screen the screen
     * @return a title for the window
     */
    public static String titleOf(Screen screen) {
        return switch (screen) {
            case LOGIN -> "Galaxy Trucker";
            case LOBBY -> "Choose a table";
            case SHIPYARD -> "Shipyard";
            case REPAIRS -> "Check over the ship";
            case CREW -> "Crew the ship";
            case FLIGHT -> "Flight";
            case LEDGER -> "Final ledger";
        };
    }
}
