package it.polimi.ingsw.server.lobby;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
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
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Many games at once, and none of them able to reach the others.
 *
 * <p>The {@code Lobby} is what the architecture calls the game registry: the one structure
 * shared across games, and the reason it is safe is not a lock but a single thread. Every
 * command from every table is applied by the same worker, one at a time, so there is no
 * interleaving to reason about and nothing to get wrong under load.
 *
 * <p>Isolation is the part worth testing rather than asserting. A server that runs three games
 * happily until one of them goes wrong has not isolated anything.
 */
class GameRegistryTest {

    private static final Duration PATIENCE = Duration.ofSeconds(3);
    private static final GameData DATA = GameDataLoader.loadBundled();

    private final Lobby lobby = new Lobby(ServerSettings.defaults()
            .dealtFrom(DATA)
            .shuffledBy(new Random(20260827L))
            .timedBy(InstantSource.fixed(Instant.parse("2026-08-27T10:00:00Z"))));

    private final List<Client> everybody = new ArrayList<>();

    @AfterEach
    void closeTheDesk() {
        lobby.close();
    }

    /** A client: what it sends, and what it has been told. */
    private final class Client {

        private final List<Event> heard = new CopyOnWriteArrayList<>();
        private final Channel<Command, Event> channel;

        Client(String nickname) {
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
            everybody.add(this);
            channel.send(new LobbyCommand.Login(nickname));
        }

        Client send(Command command) {
            channel.send(command);
            return this;
        }

        void hangUp() {
            channel.close();
        }

        <E extends Event> List<E> only(Class<E> kind) {
            return List.copyOf(heard).stream()
                    .filter(kind::isInstance)
                    .map(kind::cast)
                    .toList();
        }

        Optional<GamePhase> phase() {
            List<GameEvent.StateChanged> states = only(GameEvent.StateChanged.class);
            return states.isEmpty()
                    ? Optional.empty()
                    : Optional.of(states.get(states.size() - 1).state().phase());
        }

        String gameId() {
            List<LobbyEvent.JoinedGame> joined = only(LobbyEvent.JoinedGame.class);
            return joined.isEmpty() ? null : joined.get(joined.size() - 1).gameId();
        }
    }

    private void settle() {
        assertTrue(lobby.awaitQuiet(PATIENCE), "the registry never caught up");
    }

    /** Opens a two-player table and seats both players on it. */
    private List<Client> aTableOfTwo(String host, String guest) {
        Client one = new Client(host);
        settle();
        one.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
        settle();
        Client two = new Client(guest);
        settle();
        two.send(new LobbyCommand.JoinGame(one.gameId()));
        settle();
        return List.of(one, two);
    }

    @Test
    @DisplayName("threeConcurrentGames_progressIndependently")
    void threeConcurrentGamesProgressIndependently() {
        List<Client> first = aTableOfTwo("samuele", "chiara");
        List<Client> second = aTableOfTwo("marco", "giulia");
        List<Client> third = aTableOfTwo("elena", "davide");

        assertEquals(3, lobby.gamesRunning().size(), "three tables should be playing");
        assertNotEquals(first.get(0).gameId(), second.get(0).gameId());
        assertNotEquals(second.get(0).gameId(), third.get(0).gameId());

        // Only the first table finishes building. The other two must be exactly where they
        // were, which is what "independently" means — not merely that they still exist.
        first.forEach(player -> player.send(new BuildingCommand.FinishBuilding(null)));
        settle();

        assertNotEquals(GamePhase.BUILDING, first.get(0).phase().orElseThrow(),
                "the first table should have moved on");
        assertEquals(GamePhase.BUILDING, second.get(0).phase().orElseThrow(),
                "the second table was not touched and should not have moved");
        assertEquals(GamePhase.BUILDING, third.get(0).phase().orElseThrow());
    }

    @Test
    @DisplayName("crashInOneGame_doesNotAffectAnother")
    void crashInOneGameDoesNotAffectAnother() {
        List<Client> doomed = aTableOfTwo("samuele", "chiara");
        List<Client> bystander = aTableOfTwo("marco", "giulia");
        String doomedId = doomed.get(0).gameId();

        // Everybody at the first table hangs up at once. Under the default policy that ends
        // the game, which is the closest thing to a table falling over that a player can cause.
        doomed.forEach(Client::hangUp);
        settle();

        assertFalse(lobby.gamesRunning().contains(doomedId),
                "the abandoned game should have been reclaimed");
        assertTrue(lobby.gamesRunning().contains(bystander.get(0).gameId()),
                "and the other table should still be playing");

        // And the survivor is not merely listed — it still answers.
        bystander.forEach(player -> player.send(new BuildingCommand.FinishBuilding(null)));
        settle();

        assertNotEquals(GamePhase.BUILDING, bystander.get(0).phase().orElseThrow(),
                "the surviving table stopped working when the other one fell over");
    }

    @Test
    @DisplayName("a name is taken across the whole server, not merely at one table")
    void namesAreUniqueEverywhere() {
        aTableOfTwo("samuele", "chiara");
        aTableOfTwo("marco", "giulia");

        Client impostor = new Client("marco");
        settle();

        assertTrue(impostor.only(LobbyEvent.LoggedIn.class).isEmpty(),
                "a name in use at another table is still in use");
        assertFalse(impostor.only(GameEvent.Rejected.class).isEmpty(),
                "and saying so is better than silence");
    }

    @Test
    @DisplayName("a table still gathering players is offered to everybody, whichever game they are in")
    void openTablesAreVisibleToAll() {
        Client host = new Client("samuele");
        settle();
        host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 4));
        settle();

        Client browsing = new Client("chiara");
        settle();
        browsing.send(new LobbyCommand.ListGames());
        settle();

        assertTrue(browsing.only(LobbyEvent.GamesListed.class).stream()
                        .anyMatch(listed -> listed.games().stream()
                                .anyMatch(game -> game.gameId().equals(host.gameId()))),
                "an open table nobody can see is a table nobody can join");
    }

    @Test
    @DisplayName("a finished game is reclaimed, and its players' names go back into circulation")
    void finishedGamesAreReclaimed() {
        List<Client> table = aTableOfTwo("samuele", "chiara");
        String id = table.get(0).gameId();
        assertTrue(lobby.gamesRunning().contains(id));

        table.forEach(Client::hangUp);
        settle();

        assertFalse(lobby.gamesRunning().contains(id), "the game was not reclaimed");
        assertTrue(lobby.tablesWaiting().isEmpty(), "and it is not waiting for anybody either");

        // The names are free again: somebody else may now be samuele.
        Client newcomer = new Client("samuele");
        settle();
        assertFalse(newcomer.only(LobbyEvent.LoggedIn.class).isEmpty(),
                "a name held by a game that no longer exists is a name nobody can ever use");
    }

    @Test
    @DisplayName("the registry is the only thing shared, and one thread touches it")
    void oneThreadTouchesIt() {
        // Not a lock. Every command from every table is applied by the same worker, one at a
        // time, so there is no interleaving to reason about. This is the closest a test can get
        // to that claim: hammer it from several threads and check nothing is lost or duplicated.
        List<Thread> senders = new ArrayList<>();
        for (int table = 0; table < 4; table++) {
            String host = "host-" + table;
            Thread thread = new Thread(() -> {
                Client client = new Client(host);
                client.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
            }, "sender-" + table);
            senders.add(thread);
            thread.start();
        }
        senders.forEach(thread -> {
            try {
                thread.join(PATIENCE.toMillis());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });
        settle();

        assertEquals(4, lobby.tablesWaiting().size(),
                "four tables were opened at once and " + lobby.tablesWaiting().size()
                        + " exist: " + lobby.tablesWaiting());
        assertEquals(4, lobby.tablesWaiting().stream().distinct().count(),
                "and none of them shares a name with another");
    }
}
