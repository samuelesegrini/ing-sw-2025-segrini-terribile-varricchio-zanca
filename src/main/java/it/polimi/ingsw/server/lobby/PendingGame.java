package it.polimi.ingsw.server.lobby;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.view.GameSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * A table with a chair pulled out, waiting for the rest.
 *
 * <p>Not a {@code Game}: there is no ship, no pool and no deck until the last seat is taken.
 * Building any of that for a table that might never fill would mean shuffling a hundred and
 * fifty tiles for every player who changed their mind.
 *
 * <p>Colours go out in the order people arrive. Letting players choose would be a second
 * negotiation before the game even starts, and would need its own rules for what happens when
 * two people want red.
 */
final class PendingGame {

    private final String id;
    private final GameLevel level;
    private final int seats;
    private final List<Connection> players = new ArrayList<>();

    PendingGame(String id, GameLevel level, int seats) {
        this.id = id;
        this.level = level;
        this.seats = seats;
    }

    String id() {
        return id;
    }

    GameLevel level() {
        return level;
    }

    int seats() {
        return seats;
    }

    List<Connection> players() {
        return List.copyOf(players);
    }

    boolean hasRoom() {
        return players.size() < seats;
    }

    boolean isFull() {
        return players.size() == seats;
    }

    /**
     * Seats a player and gives them their colour.
     *
     * @param player who arrived
     * @return the colour they were given
     */
    PlayerColor seat(Connection player) {
        PlayerColor colour = PlayerColor.values()[players.size()];
        players.add(player);
        return colour;
    }

    PlayerColor colourOf(Connection player) {
        return PlayerColor.values()[players.indexOf(player)];
    }

    boolean remove(Connection player) {
        return players.remove(player);
    }

    boolean isEmpty() {
        return players.isEmpty();
    }

    /**
     * Describes this table for the lobby list.
     *
     * @return one line of what somebody choosing a game needs to know
     */
    GameSummary summary() {
        return new GameSummary(id, level, seats,
                players.stream().map(Connection::nickname).toList());
    }
}
