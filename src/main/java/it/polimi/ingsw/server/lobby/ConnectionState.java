package it.polimi.ingsw.server.lobby;

import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import it.polimi.ingsw.common.transport.ChannelListener;

/**
 * Where a connection has got to.
 *
 * <p>A client arrives anonymous, says who it is, and eventually finds itself in a game. Those
 * are three different things to be, and what a command means depends on which of them the
 * connection currently is — which is the definition of a state machine, and the same shape the
 * model side uses for game phases (architecture § 3.3).
 *
 * <p>Before this, the three states were eight combinations of three nullable fields on
 * {@link Connection}, and the two questions worth asking about them —  is this connection
 * named, is it in a game — were null checks that {@link Lobby} had to run before every command
 * it handled. Nothing stopped a connection holding a colour without a nickname.
 *
 * <p><b>There is no state for waiting at a table.</b> A connection sitting at a
 * {@link PendingGame} is a fact about the table, and {@code PendingGame.players()} is already
 * the record of it. A fourth state holding the table would be a second place for that to be
 * true, which is a second place for it to be wrong.
 *
 * <p><b>On threads.</b> {@link #deliver} runs on the transport's own reading thread and must
 * return promptly; {@link #apply} and {@link #gone} run on the desk's single worker. The split
 * is what stops every game in the building queueing behind one thread whose only job would be
 * to forward.
 */
sealed interface ConnectionState {

    /**
     * Returns what this connection is called.
     *
     * @return the nickname
     * @throws IllegalStateException if it has not said yet, which is a fault in the caller:
     *                               every question the desk asks a nickname of is asked of a
     *                               connection that has already given one
     */
    String nickname();

    /**
     * Sends an arriving command wherever this state sends commands.
     *
     * <p>On the transport's reading thread. A connection in a game hands straight to that
     * game's queue rather than through the desk's, because routing every game's commands
     * through the desk would put the whole building behind one forwarding thread.
     *
     * @param command what arrived
     * @param on      the connection it arrived on
     */
    void deliver(Command command, Connection on);

    /**
     * Applies a command, on the desk's thread.
     *
     * <p>The guard that used to open {@code Lobby.handle}: anything that is not a lobby
     * command is somebody trying to play a game they are not in yet. True in every state, so
     * it lives here rather than in each of them.
     *
     * @param command what they sent
     * @param from    who sent it
     * @param desk    the lobby
     */
    default void apply(Command command, Connection from, Lobby desk) {
        if (!(command instanceof LobbyCommand lobby)) {
            desk.refuse(from, command, "you are not in a game yet");
            return;
        }
        atTheDesk(lobby, from, desk);
    }

    /**
     * Applies a lobby command in this particular state.
     *
     * @param command what they sent
     * @param from    who sent it
     * @param desk    the lobby
     */
    void atTheDesk(LobbyCommand command, Connection from, Lobby desk);

    /**
     * Tells whatever else is listening that the connection has closed.
     *
     * <p>On the transport's thread, before the desk hears about it. Only a connection in a
     * game has anything to pass the news on to.
     *
     * @param reason why, in a sentence
     */
    default void leaving(String reason) {
        // Nobody downstream to tell.
    }

    /**
     * Takes this connection out of the desk's books.
     *
     * <p>On the desk's thread. What has to be forgotten depends entirely on what the
     * connection had got as far as being, which is why it is asked rather than tested for.
     *
     * @param from the connection that has gone
     * @param desk the lobby
     */
    void gone(Connection from, Lobby desk);

    /**
     * A client that has connected and not yet said who it is.
     *
     * <p>The only thing it may do is say. Everything else is refused, because a desk that
     * answered an anonymous connection would have nothing to attribute the answer to.
     */
    record Anonymous() implements ConnectionState {

        /**
         * {@inheritDoc}
         *
         * @return never
         * @throws IllegalStateException always
         */
        @Override
        public String nickname() {
            throw new IllegalStateException("this connection has not said who it is");
        }

        /**
         * {@inheritDoc}
         *
         * @param command what arrived
         * @param on      the connection it arrived on
         */
        @Override
        public void deliver(Command command, Connection on) {
            on.queueAtTheDesk(command);
        }

        /**
         * Accepts a login and refuses everything else.
         *
         * @param command what they sent
         * @param from    who sent it
         * @param desk    the lobby
         */
        @Override
        public void atTheDesk(LobbyCommand command, Connection from, Lobby desk) {
            if (!(command instanceof LobbyCommand.Login login)) {
                desk.refuse(from, command, "say who you are first");
                return;
            }
            desk.login(from, login);
        }

        /**
         * Forgets nothing, because a connection with no name was never written down.
         *
         * @param from the connection that has gone
         * @param desk the lobby
         */
        @Override
        public void gone(Connection from, Lobby desk) {
            // Never in the books, so never taken out of them.
        }
    }

    /**
     * A named client that is not in a game: choosing a table, or waiting at one.
     *
     * @param nickname what they are called
     */
    record AtTheDesk(String nickname) implements ConnectionState {

        /**
         * Validates the state.
         *
         * @throws NullPointerException if there is no nickname
         */
        public AtTheDesk {
            if (nickname == null) {
                throw new NullPointerException("a named connection needs a name");
            }
        }

        /**
         * {@inheritDoc}
         *
         * @param command what arrived
         * @param on      the connection it arrived on
         */
        @Override
        public void deliver(Command command, Connection on) {
            on.queueAtTheDesk(command);
        }

        /**
         * Hands the command to the desk, which knows what each one means.
         *
         * @param command what they sent
         * @param from    who sent it
         * @param desk    the lobby
         */
        @Override
        public void atTheDesk(LobbyCommand command, Connection from, Lobby desk) {
            desk.fromSomebodyNamed(from, command);
        }

        /**
         * Gives up the nickname, and the seat at any table they were waiting at.
         *
         * @param from the connection that has gone
         * @param desk the lobby
         */
        @Override
        public void gone(Connection from, Lobby desk) {
            desk.leftTheDesk(nickname, from);
        }
    }

    /**
     * A client that has been handed over to a game.
     *
     * <p>Holds the listener that game gave back, which is the whole of how a command reaches
     * the right game's queue without passing through the desk's.
     *
     * @param nickname what they are called
     * @param colour   the seat they hold
     * @param listener what the game wants done with what arrives
     */
    record InGame(String nickname, PlayerColor colour,
                  ChannelListener<Command> listener) implements ConnectionState {

        /**
         * Validates the state.
         *
         * @throws NullPointerException if any part is missing
         */
        public InGame {
            if (nickname == null || colour == null || listener == null) {
                throw new NullPointerException("a seat needs a name, a colour and a listener");
            }
        }

        /**
         * Hands straight to the game, without going near the desk's queue.
         *
         * @param command what arrived
         * @param on      the connection it arrived on
         */
        @Override
        public void deliver(Command command, Connection on) {
            listener.received(command);
        }

        /**
         * Hands the command to the desk, as a named client would.
         *
         * <p>Reached only by a command that was queued at the desk just as this connection was
         * being handed over. It is answered as it would have been a moment earlier rather than
         * refused, because from the client's side nothing about it was wrong.
         *
         * @param command what they sent
         * @param from    who sent it
         * @param desk    the lobby
         */
        @Override
        public void atTheDesk(LobbyCommand command, Connection from, Lobby desk) {
            desk.fromSomebodyNamed(from, command);
        }

        /**
         * Tells the game its player has gone.
         *
         * @param reason why, in a sentence
         */
        @Override
        public void leaving(String reason) {
            listener.closed(reason);
        }

        /**
         * Leaves the seat held under this nickname, so that logging in again finds it.
         *
         * @param from the connection that has gone
         * @param desk the lobby
         */
        @Override
        public void gone(Connection from, Lobby desk) {
            desk.leftAGame(nickname, from);
        }
    }
}
