package it.polimi.ingsw.server.controller;

import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.transport.Channel;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A seat at a game and whatever connection is currently serving it, if any.
 *
 * <p>The separation architecture § 3.9 asked for. The model knows that the red player exists
 * and what their ship looks like; this knows whether anybody is presently attached to being
 * the red player. Keeping the two apart is what makes disconnection resilience a small change
 * rather than a rewrite — a drop clears the channel, a reconnect sets a new one, and the game
 * in between neither notices nor cares.
 *
 * <p>Sending to a session nobody is attached to does nothing. That is not an oversight to be
 * fixed with a queue: a player who has been away for three cards does not want three cards of
 * backlog, they want the state of the game, and the protocol sends them exactly that when they
 * come back.
 */
final class PlayerSession {

    private final String nickname;
    private final PlayerColor colour;
    private final AtomicReference<Channel<Event, Command>> channel = new AtomicReference<>();

    PlayerSession(String nickname, PlayerColor colour) {
        this.nickname = nickname;
        this.colour = colour;
    }

    String nickname() {
        return nickname;
    }

    PlayerColor colour() {
        return colour;
    }

    /**
     * Points this seat at a connection, replacing whatever was there.
     *
     * @param connection where to send events
     * @return the connection that was replaced, or {@code null} if there was none
     */
    Channel<Event, Command> attach(Channel<Event, Command> connection) {
        return channel.getAndSet(connection);
    }

    /**
     * Forgets the connection, if it is still the one given.
     *
     * <p>The check matters. A slow disconnection notice can arrive after the same player has
     * already reconnected, and clearing unconditionally would hang up on the new connection
     * because the old one finally noticed it was dead.
     *
     * @param expected the connection that reported itself closed
     * @return {@code true} if this seat is now unattached
     */
    boolean detach(Channel<Event, Command> expected) {
        return channel.compareAndSet(expected, null);
    }

    boolean isAttached() {
        return channel.get() != null;
    }

    /**
     * Sends an event, if there is anywhere to send it.
     *
     * @param event what to send
     */
    void send(Event event) {
        Channel<Event, Command> connection = channel.get();
        if (connection != null) {
            connection.send(event);
        }
    }
}
