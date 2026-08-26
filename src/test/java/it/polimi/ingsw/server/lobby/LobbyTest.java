package it.polimi.ingsw.server.lobby;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.BuildingCommand;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import it.polimi.ingsw.common.protocol.LobbyEvent;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.protocol.PreparationCommand;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.common.transport.LocalChannel;
import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that people end up at the right table with the right name.
 *
 * <p>Three of these matter more than the rest. That two clients cannot both be called Samuele,
 * however close together they ask. That a table becomes a game at the moment somebody takes the
 * last seat, and not a command later. And that logging in with the name of a seat nobody is
 * attached to puts a player straight back into the game they dropped out of — because that is
 * the whole of the reconnection story, and there is deliberately no other way to do it.
 */
class LobbyTest {

    private static final Duration PATIENCE = Duration.ofSeconds(2);
    private static final GameData DATA = GameDataLoader.loadBundled();

    private Lobby lobby = new Lobby(DATA, new Random(20260826L),
            InstantSource.fixed(Instant.parse("2026-08-26T10:00:00Z")));

    @AfterEach
    void closeTheDesk() {
        lobby.close();
    }

    /**
     * Swaps in a lobby that ends a game when somebody leaves it.
     *
     * <p>Requirement L4's baseline, which the requirements are explicit has to stay reachable
     * even once AF4 replaces it.
     */
    private void withTheBaselinePolicy() {
        lobby.close();
        lobby = new Lobby(DATA, new Random(20260826L),
                InstantSource.fixed(Instant.parse("2026-08-26T10:00:00Z")),
                DisconnectionPolicy.ENDS_THE_GAME);
    }

    /** A client: what it can send, and what it has been told. */
    private final class Client {

        private final List<Event> events = new CopyOnWriteArrayList<>();
        private final Channel<Command, Event> channel;

        Client() {
            LocalChannel.Pair<Command, Event> pair = LocalChannel.connect(
                    Command.class, Event.class,
                    ignored -> new ChannelListener<Event>() {
                        @Override
                        public void received(Event event) {
                            events.add(event);
                        }

                        @Override
                        public void closed(String reason) {
                            // Nothing here depends on being told.
                        }
                    },
                    lobby::welcome);
            this.channel = pair.near();
        }

        Client send(Command command) {
            channel.send(command);
            return this;
        }

        Client login(String nickname) {
            return send(new LobbyCommand.Login(nickname));
        }

        void hangUp() {
            channel.close();
        }

        <E extends Event> List<E> only(Class<E> kind) {
            return List.copyOf(events).stream().filter(kind::isInstance).map(kind::cast).toList();
        }

        void forget() {
            events.clear();
        }
    }

    private void settle() {
        assertTrue(lobby.awaitQuiet(PATIENCE), "the lobby never caught up");
    }

    @Nested
    @DisplayName("names")
    class Names {

        @Test
        @DisplayName("a duplicate nickname is rejected, and says so clearly enough to try again")
        void duplicateNicknameIsRejectedAtJoin() {
            new Client().login("samuele");
            settle();

            Client impostor = new Client();
            impostor.login("samuele");
            settle();

            assertTrue(impostor.only(LobbyEvent.LoggedIn.class).isEmpty());
            assertEquals("somebody is already called samuele",
                    impostor.only(GameEvent.Rejected.class).get(0).reason());
        }

        @Test
        @DisplayName("a name is free again once its client has gone")
        void namesAreReleased() {
            Client first = new Client().login("samuele");
            settle();
            first.hangUp();
            settle();

            Client second = new Client();
            second.login("samuele");
            settle();

            assertFalse(second.only(LobbyEvent.LoggedIn.class).isEmpty(),
                    "nobody is called samuele any more");
        }

        @Test
        @DisplayName("two clients asking for one name at the same moment: one of them gets it")
        void aRaceForOneName() throws InterruptedException {
            int racers = 16;
            CountDownLatch go = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(racers);
            List<Client> clients = new CopyOnWriteArrayList<>();

            for (int racer = 0; racer < racers; racer++) {
                Thread thread = new Thread(() -> {
                    Client client = new Client();
                    clients.add(client);
                    try {
                        go.await();
                        client.login("samuele");
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
                thread.setDaemon(true);
                thread.start();
            }
            go.countDown();
            assertTrue(done.await(PATIENCE.toMillis(), TimeUnit.MILLISECONDS));
            settle();

            long accepted = clients.stream()
                    .filter(client -> !client.only(LobbyEvent.LoggedIn.class).isEmpty())
                    .count();
            assertEquals(1, accepted, "a nickname belongs to one person");
        }

        @Test
        @DisplayName("nothing but logging in works until you have")
        void anonymousClients() {
            Client anonymous = new Client();

            anonymous.send(new LobbyCommand.ListGames());
            settle();

            assertEquals("say who you are first",
                    anonymous.only(GameEvent.Rejected.class).get(0).reason());
        }

        @Test
        @DisplayName("logging in twice is refused rather than quietly renaming somebody")
        void loggingInTwice() {
            Client client = new Client().login("samuele");
            settle();
            client.forget();

            client.login("chiara");
            settle();

            assertEquals("you are already samuele",
                    client.only(GameEvent.Rejected.class).get(0).reason());
        }
    }

    @Nested
    @DisplayName("tables")
    class Tables {

        @Test
        @DisplayName("the player who opens a table decides how many are playing")
        void creatingATable() {
            Client host = new Client().login("samuele");
            settle();

            host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 3));
            settle();

            assertEquals(PlayerColor.BLUE, host.only(LobbyEvent.JoinedGame.class).get(0).colour());
            assertEquals(1, lobby.tablesWaiting().size());
        }

        @Test
        @DisplayName("a table that is still filling up is listed; one that is not, is not")
        void listingTables() {
            Client host = new Client().login("samuele");
            settle();
            host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
            settle();

            Client looker = new Client().login("chiara");
            settle();
            looker.send(new LobbyCommand.ListGames());
            settle();

            List<LobbyEvent.GamesListed> listings = looker.only(LobbyEvent.GamesListed.class);
            assertEquals(1, listings.get(0).games().size());
            assertEquals(List.of("samuele"), listings.get(0).games().get(0).players());
            assertTrue(listings.get(0).games().get(0).hasRoom());
        }

        @Test
        @DisplayName("everybody at a table is told who else arrives")
        void arrivalsAreAnnounced() {
            Client host = new Client().login("samuele");
            settle();
            host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 3));
            settle();
            host.forget();

            new Client().login("chiara").send(new LobbyCommand.JoinGame("game-1"));
            settle();

            assertEquals("chiara", host.only(LobbyEvent.PlayerEntered.class).get(0).nickname());
        }

        @Test
        @DisplayName("reaching the expected count starts the game")
        void reachingTheExpectedCountStartsTheGame() {
            Client host = new Client().login("samuele");
            settle();
            host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
            settle();

            assertEquals(List.of(), lobby.gamesRunning(), "one of two is not a game yet");

            new Client().login("chiara").send(new LobbyCommand.JoinGame("game-1"));
            settle();

            assertEquals(List.of("game-1"), lobby.gamesRunning());
            assertEquals(List.of(), lobby.tablesWaiting(), "a table that became a game is not a table");
        }

        @Test
        @DisplayName("a started game sends everybody the whole picture, unasked")
        void startingSendsTheState() {
            Client host = new Client().login("samuele");
            settle();
            host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
            settle();
            host.forget();

            new Client().login("chiara").send(new LobbyCommand.JoinGame("game-1"));
            settle();

            List<GameEvent.StateChanged> states = host.only(GameEvent.StateChanged.class);
            assertFalse(states.isEmpty(), "a client that has not been told anything cannot draw");
            assertEquals(GamePhase.BUILDING, states.get(states.size() - 1).state().phase());
        }

        @Test
        @DisplayName("joining a game nobody is running is refused by name")
        void joiningNothing() {
            Client client = new Client().login("samuele");
            settle();

            client.send(new LobbyCommand.JoinGame("game-99"));
            settle();

            assertEquals("there is no game called game-99 waiting for players",
                    client.only(GameEvent.Rejected.class).get(0).reason());
        }

        @Test
        @DisplayName("a table that empties out is gone")
        void leavingATable() {
            Client host = new Client().login("samuele");
            settle();
            host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 3));
            settle();

            host.send(new LobbyCommand.LeaveGame());
            settle();

            assertEquals(List.of(), lobby.tablesWaiting());
        }

        @Test
        @DisplayName("somebody who hangs up at a table gives up their seat")
        void droppingBeforeTheGameStarts() {
            Client host = new Client().login("samuele");
            settle();
            host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 3));
            settle();

            Client leaver = new Client().login("chiara");
            settle();
            leaver.send(new LobbyCommand.JoinGame("game-1"));
            settle();
            host.forget();

            leaver.hangUp();
            settle();

            assertEquals("chiara", host.only(LobbyEvent.PlayerLeft.class).get(0).nickname());
            assertEquals(List.of("game-1"), lobby.tablesWaiting(), "the host is still waiting");
        }

        @Test
        @DisplayName("a player already waiting cannot also open a table")
        void oneTableAtATime() {
            Client client = new Client().login("samuele");
            settle();
            client.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 3));
            settle();
            client.forget();

            client.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
            settle();

            assertEquals("you are already waiting at game-1",
                    client.only(GameEvent.Rejected.class).get(0).reason());
        }

        @Test
        @DisplayName("more than one game runs at once, and they do not know about each other")
        void severalGames() {
            startAGameOfTwo("samuele", "chiara");
            startAGameOfTwo("marco", "giulia");

            assertEquals(List.of("game-1", "game-2"), lobby.gamesRunning());
        }

        private void startAGameOfTwo(String host, String guest) {
            Client first = new Client().login(host);
            settle();
            first.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
            settle();
            String table = first.only(LobbyEvent.JoinedGame.class).get(0).gameId();
            new Client().login(guest).send(new LobbyCommand.JoinGame(table));
            settle();
        }
    }

    @Nested
    @DisplayName("coming back")
    class ComingBack {

        @Test
        @DisplayName("logging in with the name of an empty seat is how a player reconnects")
        void reconnecting() {
            Client host = new Client().login("samuele");
            settle();
            host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
            settle();
            Client guest = new Client().login("chiara");
            settle();
            guest.send(new LobbyCommand.JoinGame("game-1"));
            settle();

            host.hangUp();
            settle();

            Client returning = new Client();
            returning.login("samuele");
            settle();

            assertFalse(returning.only(LobbyEvent.LoggedIn.class).isEmpty());
            assertEquals("game-1", returning.only(LobbyEvent.JoinedGame.class).get(0).gameId());
            assertFalse(returning.only(GameEvent.StateChanged.class).isEmpty(),
                    "a returning player is sent the state, which is all they need");
        }

        @Test
        @DisplayName("a player who left mid-question gets the question back, not just the board")
        void theQuestionIsStillWaiting() {
            // The point of reconnection. A board tells a returning player where everything is;
            // the outstanding prompt tells them the game has been sitting waiting for them, and
            // what it is waiting for. Without it they would be looking at a flight that appears
            // to have stopped for no reason.
            Client host = new Client().login("samuele");
            settle();
            host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
            settle();
            Client guest = new Client().login("chiara");
            settle();
            guest.send(new LobbyCommand.JoinGame("game-1"));
            settle();
            host.send(new BuildingCommand.FinishBuilding(null));
            guest.send(new BuildingCommand.FinishBuilding(null));
            settle();
            host.send(new PreparationCommand.FinishPreparation());
            guest.send(new PreparationCommand.FinishPreparation());
            settle();

            PlayerPrompt asked = latestState(host).flatMap(GameView::pendingIfAny)
                    .or(() -> latestState(guest).flatMap(GameView::pendingIfAny))
                    .orElseThrow(() -> new AssertionError("no card asked anybody anything"));
            Client leaving = asked.player() == latestState(host).orElseThrow().you() ? host : guest;

            leaving.hangUp();
            settle();
            Client returning = new Client();
            returning.login(leaving == host ? "samuele" : "chiara");
            settle();

            assertEquals(Optional.of(asked), latestState(returning).flatMap(GameView::pendingIfAny),
                    "the same question, still waiting");
        }

        /** The last board a client was sent, which is the only thing it is expected to keep. */
        private Optional<GameView> latestState(Client client) {
            List<GameEvent.StateChanged> states = client.only(GameEvent.StateChanged.class);
            return states.isEmpty()
                    ? Optional.empty()
                    : Optional.of(states.get(states.size() - 1).state());
        }

        @Test
        @DisplayName("the game carries on while somebody is away")
        void theGameDoesNotWait() {
            Client host = new Client().login("samuele");
            settle();
            host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
            settle();
            Client guest = new Client().login("chiara");
            settle();
            guest.send(new LobbyCommand.JoinGame("game-1"));
            settle();

            host.hangUp();
            settle();
            guest.forget();
            guest.send(new BuildingCommand.DrawFromPool());
            settle();
            assertTrue(lobby.gamesRunning().contains("game-1"));

            assertFalse(guest.only(GameEvent.StateChanged.class).isEmpty(),
                    "a flight that stopped every time a laptop did would be a worse game");
        }

        @Test
        @DisplayName("a seat somebody is sitting in cannot be taken by name")
        void takingAnOccupiedSeat() {
            Client host = new Client().login("samuele");
            settle();
            host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
            settle();
            new Client().login("chiara").send(new LobbyCommand.JoinGame("game-1"));
            settle();

            Client impostor = new Client();
            impostor.login("samuele");
            settle();

            assertEquals("somebody is already called samuele",
                    impostor.only(GameEvent.Rejected.class).get(0).reason());
        }

        @Test
        @DisplayName("a game command from somebody not in a game is refused")
        void gameCommandsInTheLobby() {
            Client client = new Client().login("samuele");
            settle();
            client.forget();

            client.send(new BuildingCommand.DrawFromPool());
            settle();

            assertEquals("you are not in a game yet",
                    client.only(GameEvent.Rejected.class).get(0).reason());
        }

        @Test
        @DisplayName("under the baseline policy, somebody leaving ends the game for everybody")
        void theBaselinePolicy() {
            withTheBaselinePolicy();
            Client host = new Client().login("samuele");
            settle();
            host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
            settle();
            Client guest = new Client().login("chiara");
            settle();
            guest.send(new LobbyCommand.JoinGame("game-1"));
            settle();
            guest.forget();

            host.hangUp();
            settle();

            assertEquals(List.of(), lobby.gamesRunning(), "the game is over");
            assertFalse(guest.only(GameEvent.GameEnded.class).isEmpty(),
                    "a client that is simply cut off cannot tell a finished game from a "
                            + "failed network");
        }

        @Test
        @DisplayName("under the baseline policy, the names go back to being free")
        void theBaselinePolicyReleasesNames() {
            withTheBaselinePolicy();
            Client host = new Client().login("samuele");
            settle();
            host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
            settle();
            new Client().login("chiara").send(new LobbyCommand.JoinGame("game-1"));
            settle();

            host.hangUp();
            settle();

            Client reuse = new Client();
            reuse.login("samuele");
            settle();

            assertFalse(reuse.only(LobbyEvent.LoggedIn.class).isEmpty(),
                    "there is no game holding that seat any more");
        }

        @Test
        @DisplayName("colours go out in the order people arrive")
        void coloursFollowArrival() {
            Client host = new Client().login("samuele");
            settle();
            host.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
            settle();
            Client guest = new Client().login("chiara");
            settle();
            guest.send(new LobbyCommand.JoinGame("game-1"));
            settle();

            assertNotEquals(host.only(LobbyEvent.JoinedGame.class).get(0).colour(),
                    guest.only(LobbyEvent.JoinedGame.class).get(0).colour());
        }
    }
}
