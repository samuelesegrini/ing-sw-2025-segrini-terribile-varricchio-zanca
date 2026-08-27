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
 * <p>What it holds is a {@link ConnectionState} rather than a nickname, a colour and a game
 * listener that may or may not be null. The three states are the three things a connection can
 * be; the eight combinations of three nulls included five that never meant anything, and the
 * two questions the desk had to ask about them were null checks it ran before every command.
 *
 * <p>Once a game has started, commands go <b>straight</b> to that game's queue rather than
 * through the lobby's. Routing them through the lobby would put every game in the building
 * behind one thread whose only job would be to forward.
 */
final class Connection implements ChannelListener<Command> {

    private final Lobby lobby;
    private final Channel<Event, Command> channel;

    /**
     * Volatile because the transport's reading thread reads it and the desk's worker writes
     * it. One reference rather than three fields is also why a half-applied transition — a
     * colour set before the nickname — is no longer expressible.
     */
    private volatile ConnectionState state = new ConnectionState.Anonymous();

    Connection(Lobby lobby, Channel<Event, Command> channel) {
        this.lobby = lobby;
        this.channel = channel;
    }

    @Override
    public void received(Command command) {
        state.deliver(command, this);
    }

    @Override
    public void closed(String reason) {
        state.leaving(reason);
        lobby.disconnected(this);
    }

    /**
     * Puts a command on the desk's queue.
     *
     * <p>Called by the states that have nowhere better to send one. Here rather than in them
     * so that the lobby a connection belongs to stays the connection's own business.
     *
     * @param command what arrived
     */
    void queueAtTheDesk(Command command) {
        lobby.submit(this, command);
    }

    /**
     * Applies a command on the desk's thread, in whatever state this connection is in.
     *
     * @param command what they sent
     * @param desk    the lobby
     */
    void apply(Command command, Lobby desk) {
        state.apply(command, this, desk);
    }

    /**
     * Takes this connection out of the desk's books, according to how far it had got.
     *
     * @param desk the lobby
     */
    void gone(Lobby desk) {
        state.gone(this, desk);
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

    /**
     * Returns what this client is called.
     *
     * @return the nickname
     * @throws IllegalStateException if it has not said yet
     */
    String nickname() {
        return state.nickname();
    }

    /**
     * Takes a nickname, which is what turns an anonymous connection into a client.
     *
     * @param name what they will be called
     */
    void nameYourself(String name) {
        this.state = new ConnectionState.AtTheDesk(name);
    }

    /**
     * Hands this connection over to a game.
     *
     * @param colour   the seat it is now bound to
     * @param listener what the game wants done with what arrives
     */
    void handOverTo(PlayerColor colour, ChannelListener<Command> listener) {
        this.state = new ConnectionState.InGame(nickname(), colour, listener);
    }
}
