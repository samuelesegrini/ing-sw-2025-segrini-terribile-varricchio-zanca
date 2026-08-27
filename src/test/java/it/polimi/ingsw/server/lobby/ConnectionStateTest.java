package it.polimi.ingsw.server.lobby;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.BuildingCommand;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import it.polimi.ingsw.common.protocol.LobbyEvent;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.common.transport.LocalChannel;
import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where a connection has got to, and what that makes a command mean.
 *
 * <p>These three states used to be eight combinations of three nullable fields, and the two
 * questions the desk asked about them — is this named, is this in a game — were null checks
 * that ran before every command it handled. Nothing stopped a connection holding a colour
 * without a nickname.
 *
 * <p>Driven through the states directly rather than over a channel. The refusals below are
 * already reachable through {@link LobbyTest}, over a whole conversation; what has no other
 * home is the claim that they belong to the <em>state</em> and not to a guard clause that a
 * future command could be added without.
 *
 * <p>Components involved: {@link ConnectionState}, {@link Connection}, {@link Lobby}.
 */
class ConnectionStateTest {

    private static final GameData DATA = GameDataLoader.loadBundled();

    private final List<Lobby> opened = new ArrayList<>();

    @AfterEach
    void closeWhatWasOpened() {
        opened.forEach(Lobby::close);
    }

    private Lobby aDesk() {
        Lobby lobby = new Lobby(DATA, new Random(20260827L),
                InstantSource.fixed(Instant.parse("2026-08-27T10:00:00Z")));
        opened.add(lobby);
        return lobby;
    }

    /** A connection to a desk, with everything it has been told. */
    private record Wired(Connection connection, List<Event> heard) {
    }

    /**
     * Connects to a desk and hands back the server's own end.
     *
     * <p>The connection rather than the channel, because these tests apply commands on it
     * directly: going through the channel would put them on the desk's queue and turn a test
     * about one state into a test about the whole desk.
     */
    private static Wired wire(Lobby lobby) {
        List<Event> heard = new CopyOnWriteArrayList<>();
        List<ChannelListener<Command>> server = new ArrayList<>();
        LocalChannel.connect(Command.class, Event.class,
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
                channel -> {
                    ChannelListener<Command> listener = lobby.welcome(channel);
                    server.add(listener);
                    return listener;
                });
        return new Wired((Connection) server.get(0), heard);
    }

    private static String lastRefusal(List<Event> heard) {
        List<GameEvent.Rejected> refusals = heard.stream()
                .filter(GameEvent.Rejected.class::isInstance)
                .map(GameEvent.Rejected.class::cast)
                .toList();
        assertTrue(!refusals.isEmpty(), "nothing was refused");
        return refusals.get(refusals.size() - 1).reason();
    }

    @Nested
    @DisplayName("before saying who it is")
    class Anonymous {

        @Test
        @DisplayName("a lobby command other than a login is refused")
        void aLobbyCommandBeforeLoggingInIsRefused() {
            Lobby desk = aDesk();
            Wired wired = wire(desk);

            wired.connection().apply(new LobbyCommand.ListGames(), desk);

            assertEquals("say who you are first", lastRefusal(wired.heard()));
        }

        @Test
        @DisplayName("a game command is refused as a game command, not as an anonymous one")
        void aGameCommandBeforeLoggingInIsRefusedAsAGameCommand() {
            Lobby desk = aDesk();
            Wired wired = wire(desk);

            wired.connection().apply(new BuildingCommand.DrawFromPool(), desk);

            // The order of the two old guard clauses, preserved: "you are not in a game yet"
            // comes first, because it is true whether or not they have logged in.
            assertEquals("you are not in a game yet", lastRefusal(wired.heard()));
        }

        @Test
        @DisplayName("a login is accepted, which is the one thing it may do")
        void aLoginIsAccepted() {
            Lobby desk = aDesk();
            Wired wired = wire(desk);

            wired.connection().apply(new LobbyCommand.Login("samuele"), desk);

            assertEquals(1, wired.heard().stream()
                    .filter(LobbyEvent.LoggedIn.class::isInstance).count());
            assertEquals("samuele", wired.connection().nickname());
        }

        @Test
        @DisplayName("has no name to give, and says so rather than handing back null")
        void hasNoNickname() {
            // The whole point of the state. A null nickname used to propagate silently into the
            // desk's books, where the only thing that noticed was `forget` checking for it.
            assertThrows(IllegalStateException.class,
                    () -> new ConnectionState.Anonymous().nickname());
        }
    }

    @Nested
    @DisplayName("once it has a name")
    class Named {

        @Test
        @DisplayName("a game command is still refused, because a name is not a seat")
        void aGameCommandFromTheDeskIsRefused() {
            Lobby desk = aDesk();
            Wired wired = wire(desk);
            wired.connection().apply(new LobbyCommand.Login("samuele"), desk);

            wired.connection().apply(new BuildingCommand.DrawFromPool(), desk);

            assertEquals("you are not in a game yet", lastRefusal(wired.heard()));
        }

        @Test
        @DisplayName("logging in twice is refused, and says what they are already called")
        void loggingInTwiceIsRefused() {
            Lobby desk = aDesk();
            Wired wired = wire(desk);
            wired.connection().apply(new LobbyCommand.Login("samuele"), desk);

            wired.connection().apply(new LobbyCommand.Login("chiara"), desk);

            assertEquals("you are already samuele", lastRefusal(wired.heard()));
        }

        @Test
        @DisplayName("a lobby command reaches the desk")
        void aLobbyCommandReachesTheDesk() {
            Lobby desk = aDesk();
            Wired wired = wire(desk);
            wired.connection().apply(new LobbyCommand.Login("samuele"), desk);

            wired.connection().apply(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2), desk);

            assertEquals(1, wired.heard().stream()
                    .filter(LobbyEvent.JoinedGame.class::isInstance).count());
        }
    }

    @Nested
    @DisplayName("the states themselves")
    class TheStates {

        @Test
        @DisplayName("a seat cannot be held without a name, which the old three fields allowed")
        void aSeatNeedsAName() {
            ChannelListener<Command> nothing = new ChannelListener<>() {
                @Override
                public void received(Command command) {
                    // Not used.
                }

                @Override
                public void closed(String reason) {
                    // Not used.
                }
            };

            assertThrows(NullPointerException.class,
                    () -> new ConnectionState.InGame(null, PlayerColor.BLUE, nothing));
            assertThrows(NullPointerException.class,
                    () -> new ConnectionState.InGame("samuele", null, nothing));
        }

        @Test
        @DisplayName("a named state cannot be nameless either")
        void aNamedStateNeedsAName() {
            assertThrows(NullPointerException.class, () -> new ConnectionState.AtTheDesk(null));
        }

        @Test
        @DisplayName("a connection in a game hands arrivals to the game, not to the desk")
        void aConnectionInAGameBypassesTheDesk() {
            List<Command> reachedTheGame = new ArrayList<>();
            ChannelListener<Command> game = new ChannelListener<>() {
                @Override
                public void received(Command command) {
                    reachedTheGame.add(command);
                }

                @Override
                public void closed(String reason) {
                    // Not used here.
                }
            };
            ConnectionState seated =
                    new ConnectionState.InGame("samuele", PlayerColor.BLUE, game);

            seated.deliver(new BuildingCommand.DrawFromPool(), null);

            // Routing every game's commands through the desk would put the whole building
            // behind one thread whose only job would be to forward.
            assertEquals(1, reachedTheGame.size());
            assertInstanceOf(BuildingCommand.DrawFromPool.class, reachedTheGame.get(0));
        }
    }
}
