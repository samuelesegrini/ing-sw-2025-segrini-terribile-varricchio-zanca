package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.PlayerColor;

/**
 * One place at the table: a name and a set of markers.
 *
 * <p>Deliberately not a connection. The model knows that the red player exists and what their
 * ship looks like; whether anybody is currently attached to the red player is a fact about the
 * network, and keeping the two apart is what makes disconnection resilience a small change
 * rather than a rewrite (architecture § 3.9).
 *
 * @param nickname what they are called
 * @param colour   which markers are theirs
 */
public record Seat(String nickname, PlayerColor colour) {

    /**
     * Validates the seat.
     *
     * @throws NullPointerException     if the colour is {@code null}
     * @throws IllegalArgumentException if the nickname is blank
     */
    public Seat {
        if (colour == null) {
            throw new NullPointerException("a seat needs a colour");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("a player needs a nickname");
        }
    }
}
