package it.polimi.ingsw.common.protocol.view;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.ScoreSheet;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Everything one player is allowed to know about a game, at one moment.
 *
 * <p>This is the whole picture, and the protocol's answer to a hard question: how does a
 * client that missed a message catch up? It does not. Every batch of events ends with one
 * of these, so a client that ignores every other event and reads only this is still
 * correct — slower to narrate, never wrong. Reconnecting after a dropped connection is then
 * the same operation as joining, and needs no replay log (requirement AF4).
 *
 * <p>It is built per recipient, because {@link BuildingView} carries the tile in the
 * player's hand and the cards they have peeked at, and neither is public.
 *
 * @param gameId    which game this is
 * @param level     which flight is being played
 * @param phase     where the game has got to
 * @param you       which player is being told
 * @param players   everybody at the table, in seating order
 * @param building  the shipyard, {@code null} once building is over
 * @param flight    the route, {@code null} until the ships launch
 * @param pending   the decision the game is waiting for, {@code null} when it is waiting for nobody
 * @param scores    the final ledger, {@code null} until the game is over
 */
public record GameView(String gameId, GameLevel level, GamePhase phase, PlayerColor you,
                       List<PlayerView> players, BuildingView building, FlightView flight,
                       PlayerPrompt pending, List<ScoreSheet> scores) implements Serializable {

    /**
     * Takes a defensive copy of the players and the scores.
     *
     * @throws NullPointerException if the game, the level, the phase or the recipient is {@code null}
     */
    public GameView {
        if (gameId == null || level == null || phase == null || you == null) {
            throw new NullPointerException("a game view needs an id, a level, a phase and a recipient");
        }
        players = List.copyOf(players);
        scores = scores == null ? null : List.copyOf(scores);
    }

    /**
     * Returns the shipyard.
     *
     * @return the shipyard, or empty once the ships are built
     */
    public Optional<BuildingView> buildingIfAny() {
        return Optional.ofNullable(building);
    }

    /**
     * Returns the route.
     *
     * @return the route, or empty before the ships launch
     */
    public Optional<FlightView> flightIfAny() {
        return Optional.ofNullable(flight);
    }

    /**
     * Returns the decision the game is waiting for.
     *
     * <p>Present does not mean it is this player's decision: everyone is told who the game
     * is waiting for, so that a view can say so rather than appearing to hang.
     *
     * @return the outstanding prompt, or empty when nothing is outstanding
     */
    public Optional<PlayerPrompt> pendingIfAny() {
        return Optional.ofNullable(pending);
    }

    /**
     * Returns the final ledger.
     *
     * @return the score sheets, richest first, or empty until the game is over
     */
    public Optional<List<ScoreSheet>> scoresIfAny() {
        return Optional.ofNullable(scores);
    }
}
