package it.polimi.ingsw.common.protocol.view;

import it.polimi.ingsw.common.game.GameLevel;

import java.io.Serializable;
import java.util.List;

/**
 * One line of the lobby list: a game somebody could join.
 *
 * <p>Enough to choose between games and no more. The ships are not here — a game in
 * progress is not something to browse (requirement AF2).
 *
 * @param gameId  what to name when joining
 * @param level   which flight it will be
 * @param seats   how many players it was created for
 * @param players who is already sitting there, in the order they arrived
 */
public record GameSummary(String gameId, GameLevel level, int seats,
                          List<String> players) implements Serializable {

    /**
     * Takes a defensive copy of the players.
     *
     * @throws NullPointerException     if the id or the level is {@code null}
     * @throws IllegalArgumentException if the game seats fewer than two players
     */
    public GameSummary {
        if (gameId == null || level == null) {
            throw new NullPointerException("a game summary needs an id and a level");
        }
        if (seats < 2 || seats > 4) {
            throw new IllegalArgumentException("a game seats two to four players, not " + seats);
        }
        players = List.copyOf(players);
    }

    /**
     * Tells whether there is still room.
     *
     * @return {@code true} when somebody could still join
     */
    public boolean hasRoom() {
        return players.size() < seats;
    }
}
