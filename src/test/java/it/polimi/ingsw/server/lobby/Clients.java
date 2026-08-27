package it.polimi.ingsw.server.lobby;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import it.polimi.ingsw.common.protocol.LobbyEvent;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.common.transport.LocalChannel;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Somebody sitting at a desk, for tests that need one.
 *
 * <p>Public because the persistence tests live next door and need the same thing. Three copies
 * of "connect a fake client to a lobby" is three places to get the channel plumbing subtly
 * different, and the plumbing is not what any of those tests are about.
 */
public final class Clients {

    private static final Duration PATIENCE = Duration.ofSeconds(3);

    private Clients() {
    }

    /** A client: what it sends, and what it has been told. */
    public static final class Client {

        private final List<Event> heard = new CopyOnWriteArrayList<>();
        private final Channel<Command, Event> channel;

        /**
         * Connects and logs in.
         *
         * @param lobby    the desk
         * @param nickname what to be called
         */
        public Client(Lobby lobby, String nickname) {
            LocalChannel.Pair<Command, Event> pair = LocalChannel.connect(
                    Command.class, Event.class,
                    ignored -> new ChannelListener<Event>() {
                        @Override
                        public void received(Event event) {
                            heard.add(event);
                        }

                        @Override
                        public void closed(String reason) {
                            // Nothing here depends on being told.
                        }
                    },
                    lobby::welcome);
            this.channel = pair.near();
            channel.send(new LobbyCommand.Login(nickname));
        }

        /**
         * Sends something.
         *
         * @param command what to send
         * @return this client, for chaining
         */
        public Client send(Command command) {
            channel.send(command);
            return this;
        }

        /** Drops the connection, as a closed laptop would. */
        public void hangUp() {
            channel.close();
        }

        /**
         * Returns everything of one kind this client has been told.
         *
         * @param kind which events
         * @param <E>  their type
         * @return them, oldest first
         */
        public <E extends Event> List<E> only(Class<E> kind) {
            return List.copyOf(heard).stream()
                    .filter(kind::isInstance)
                    .map(kind::cast)
                    .toList();
        }

        /**
         * Returns the game this client was last seated at.
         *
         * @return its identifier, or {@code null} if they are not at one
         */
        public String gameId() {
            List<LobbyEvent.JoinedGame> joined = only(LobbyEvent.JoinedGame.class);
            return joined.isEmpty() ? null : joined.get(joined.size() - 1).gameId();
        }
    }

    /**
     * Opens a two-player table and seats both players on it.
     *
     * @param lobby the desk
     * @param host  who opens it
     * @param guest who joins
     * @return both clients, host first
     */
    public static List<Client> aTableOfTwo(Lobby lobby, String host, String guest) {
        Client one = new Client(lobby, host);
        settle(lobby);
        one.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
        settle(lobby);
        Client two = new Client(lobby, guest);
        settle(lobby);
        two.send(new LobbyCommand.JoinGame(one.gameId()));
        settle(lobby);
        return List.of(one, two);
    }

    /**
     * Waits for the desk to finish whatever it was doing.
     *
     * @param lobby the desk
     */
    public static void settle(Lobby lobby) {
        assertTrue(lobby.awaitQuiet(PATIENCE), "the lobby never caught up");
    }
}
