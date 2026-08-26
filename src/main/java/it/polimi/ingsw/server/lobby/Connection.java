package it.polimi.ingsw.server.lobby;

import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelListener;

/**
 * One client, wherever it has got to.
 *
 * <p>A connection starts anonymous, acquires a nickname, sits at a table, and eventually finds
 * itself in a game. The channel underneath cannot change its listener once it is open — and
 * should not, since something has to be listening from the first byte — so this is the thing
 * that changes instead.
 *
 * <p>Once a game has started, commands go <b>straight</b> to that game's queue rather than
 * through the lobby's. Routing them through the lobby would put every game in the building
 * behind one thread whose only job would be to forward.
 */
final class Connection implements ChannelListener<Command> {

    private final Lobby lobby;
    private final Channel<Event, Command> channel;

    private volatile String nickname;
    private volatile ChannelListener<Command> playing;
    private volatile PlayerColor colour;

    Connection(Lobby lobby, Channel<Event, Command> channel) {
        this.lobby = lobby;
        this.channel = channel;
    }

    @Override
    public void received(Command command) {
        ChannelListener<Command> inGame = playing;
        if (inGame != null) {
            inGame.received(command);
            return;
        }
        lobby.submit(this, command);
    }

    @Override
    public void closed(String reason) {
        ChannelListener<Command> inGame = playing;
        if (inGame != null) {
            inGame.closed(reason);
        }
        lobby.disconnected(this);
    }

    /**
     * Sends an event to this client.
     *
     * @param event what to send
     */
    void send(Event event) {
        channel.send(event);
    }

    Channel<Event, Command> channel() {
        return channel;
    }

    String nickname() {
        return nickname;
    }

    void nameYourself(String name) {
        this.nickname = name;
    }

    boolean isLoggedIn() {
        return nickname != null;
    }

    PlayerColor colour() {
        return colour;
    }

    boolean isPlaying() {
        return playing != null;
    }

    /**
     * Hands this connection over to a game.
     *
     * @param colour   the seat it is now bound to
     * @param listener what the game wants done with what arrives
     */
    void handOverTo(PlayerColor colour, ChannelListener<Command> listener) {
        this.colour = colour;
        this.playing = listener;
    }
}
