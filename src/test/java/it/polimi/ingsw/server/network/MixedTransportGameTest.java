package it.polimi.ingsw.server.network;

import it.polimi.ingsw.common.game.Answers;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.BuildingCommand;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.FlightCommand;
import it.polimi.ingsw.common.protocol.FlightEvent;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import it.polimi.ingsw.common.protocol.LobbyEvent;
import it.polimi.ingsw.common.protocol.PreparationCommand;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.common.transport.Liveness;
import it.polimi.ingsw.common.transport.rmi.RmiConnector;
import it.polimi.ingsw.common.transport.socket.SocketConnector;
import it.polimi.ingsw.server.data.GameDataLoader;
import it.polimi.ingsw.server.lobby.DisconnectionPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Four players, two transports, one game, played to the last credit.
 *
 * <p>This is requirement S5, and it is the test the whole of M4 was arranged around. Two players
 * connect over a socket and two over RMI; nothing anywhere below the transports is told which,
 * and the game they play is one game.
 *
 * <p>What it proves that the unit tests cannot: that real serialization carries every message
 * the protocol defines, that two very different connection mechanisms deliver them in the same
 * order, and that the events describing what happened are <em>identical</em> whichever door a
 * player came through. A transport-specific branch anywhere in the controller would show up
 * here as four players watching four slightly different games.
 */
class MixedTransportGameTest {

    /** A whole flight over two real transports, with room to spare on a slow machine. */
    private static final Duration PATIENCE = Duration.ofSeconds(30);

    private Server server;
    private final List<Player> players = new ArrayList<>();

    @AfterEach
    void shutDown() {
        players.forEach(Player::hangUp);
        players.clear();
        if (server != null) {
            server.close();
        }
    }

    /**
     * A headless client that plays badly but legally, and remembers what it was told.
     *
     * <p>It answers whatever it is asked with the least interesting legal answer. The point is
     * to reach the end of the deck over a real connection, not to play well.
     */
    private final class Player implements ChannelListener<Event> {

        private final String nickname;
        private final List<Event> heard = new CopyOnWriteArrayList<>();
        private final AtomicReference<GameView> latest = new AtomicReference<>();
        private final CountDownLatch finished = new CountDownLatch(1);
        private final CountDownLatch seated = new CountDownLatch(1);

        private volatile Channel<Command, Event> channel;
        private volatile PlayerColor colour;
        private volatile boolean declaredBuilt;
        private volatile boolean declaredCrewed;
        private volatile boolean toldItEnded;
        private volatile boolean sawTheLastBoard;

        Player(String nickname) {
            this.nickname = nickname;
        }

        void connectOver(Channel<Command, Event> connection) {
            this.channel = connection;
            connection.send(new LobbyCommand.Login(nickname));
        }

        @Override
        public void received(Event event) {
            heard.add(event);
            switch (event) {
                case LobbyEvent.JoinedGame joined -> {
                    colour = joined.colour();
                    seated.countDown();
                }
                case GameEvent.StateChanged state -> {
                    latest.set(state.state());
                    play(state.state());
                }
                case GameEvent.GameEnded ignored -> {
                    toldItEnded = true;
                    releaseIfDone();
                }
                default -> {
                    // Narration. Kept, and not acted on: a client that reads only the state is
                    // still correct, which is the protocol's whole promise.
                }
            }
        }

        @Override
        public void closed(String reason) {
            finished.countDown();
        }

        /**
         * Does whatever the state says is owed.
         *
         * <p>Driven entirely by {@code StateChanged}, which is the protocol's claim being taken
         * literally: a client that ignores every narration event should still be able to play.
         */
        private void play(GameView state) {
            if (state.phase() == GamePhase.FINISHED) {
                sawTheLastBoard = true;
                releaseIfDone();
                return;
            }
            if (state.phase() == GamePhase.BUILDING && !declaredBuilt) {
                declaredBuilt = true;
                channel.send(new BuildingCommand.FinishBuilding(null));
                return;
            }
            if (state.phase() == GamePhase.CREW_PLACEMENT && !declaredCrewed) {
                declaredCrewed = true;
                channel.send(new PreparationCommand.FinishPreparation());
                return;
            }
            state.pendingIfAny()
                    .filter(prompt -> prompt.player() == colour)
                    .ifPresent(prompt ->
                            channel.send(new FlightCommand.Answer(Answers.simplestTo(prompt))));
        }

        /**
         * Lets the test move on once this client has both facts and truth.
         *
         * <p>Waiting for either on its own is a race. {@code GameEnded} arrives before the
         * final state — every batch ends with the state, and the last batch is no exception —
         * so a client released by the event has not seen the ledger yet, and one released by
         * the board may still have the event in flight while its narration is compared.
         */
        private void releaseIfDone() {
            if (toldItEnded && sawTheLastBoard) {
                finished.countDown();
            }
        }

        void send(Command command) {
            channel.send(command);
        }

        void hangUp() {
            if (channel != null) {
                channel.close();
            }
        }

        boolean waitToBeSeated() throws InterruptedException {
            return seated.await(PATIENCE.toMillis(), TimeUnit.MILLISECONDS);
        }

        boolean waitForTheEnd() throws InterruptedException {
            return finished.await(PATIENCE.toMillis(), TimeUnit.MILLISECONDS);
        }

        /**
         * Returns the narration this player heard: what happened, without the states.
         *
         * <p>States differ between players on purpose — each carries the tile in that player's
         * hand — so they are not what "the same events" can mean. The narration is.
         */
        List<Event> narration() {
            return List.copyOf(heard).stream()
                    .filter(event -> !(event instanceof GameEvent.StateChanged))
                    .filter(event -> !(event instanceof GameEvent.Rejected))
                    .filter(event -> !(event instanceof LobbyEvent))
                    .toList();
        }

        List<Event> heard() {
            return List.copyOf(heard);
        }

        GameView latest() {
            return latest.get();
        }
    }

    private Player overSocket(String nickname) {
        Player player = new Player(nickname);
        players.add(player);
        player.connectOver(SocketConnector.connect(
                "localhost", server.socketPort(), player, Liveness.DEFAULT));
        return player;
    }

    private Player overRmi(String nickname) {
        Player player = new Player(nickname);
        players.add(player);
        player.connectOver(RmiConnector.connect(
                "localhost", server.rmiPort(), player, Liveness.DEFAULT));
        return player;
    }

    @Test
    @DisplayName("socket and RMI players share one game, and play it to the end")
    void socketAndRmiPlayersShareOneGame() throws InterruptedException {
        server = Server.start(0, 0, GameDataLoader.loadBundled(), new Random(20260826L),
                InstantSource.system(), DisconnectionPolicy.GAME_CARRIES_ON);

        Player samuele = overSocket("samuele");
        Player chiara = overRmi("chiara");
        Player marco = overSocket("marco");
        Player giulia = overRmi("giulia");

        samuele.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 4));
        assertTrue(samuele.waitToBeSeated(), "the host never got a seat");
        chiara.send(new LobbyCommand.JoinGame("game-1"));
        marco.send(new LobbyCommand.JoinGame("game-1"));
        giulia.send(new LobbyCommand.JoinGame("game-1"));

        for (Player player : List.of(chiara, marco, giulia)) {
            assertTrue(player.waitToBeSeated(), "somebody never got a seat");
        }
        for (Player player : players) {
            assertTrue(player.waitForTheEnd(),
                    "the game never finished; " + player.nickname + " is still waiting");
        }

        // ---- one game, four ledgers ----
        for (Player player : players) {
            GameView last = player.latest();
            assertNotNull(last, "a player who was never sent a state cannot draw anything");
            assertEquals("game-1", last.gameId());
            assertEquals(GamePhase.FINISHED, last.phase());
            assertEquals(4, last.players().size());
            assertTrue(last.scoresIfAny().isPresent(), "the ledger travels in the final state");
            assertEquals(4, last.scoresIfAny().orElseThrow().size());
        }

        // ---- the same ledger, whichever door they came through ----
        assertEquals(samuele.latest().scoresIfAny(), chiara.latest().scoresIfAny(),
                "a socket player and an RMI player did not agree on who won");
        assertEquals(samuele.latest().scoresIfAny(), giulia.latest().scoresIfAny());
        assertEquals(marco.latest().scoresIfAny(), chiara.latest().scoresIfAny());

        // ---- the same events, in the same order ----
        List<Event> theStory = samuele.narration();
        assertFalse(theStory.isEmpty(), "nothing was narrated at all");
        for (Player player : players) {
            assertEquals(theStory, player.narration(),
                    player.nickname + " watched a different game");
        }

        // ---- and it was a real flight, not an empty one ----
        long cards = theStory.stream().filter(FlightEvent.CardRevealed.class::isInstance).count();
        assertTrue(cards > 1, "only " + cards + " cards were turned over");
        assertTrue(theStory.stream().anyMatch(GameEvent.PhaseBegan.class::isInstance));
    }

    @Test
    @DisplayName("a player's colour comes from the order they arrive, not the way they connect")
    void transportDoesNotDecideAnything() throws InterruptedException {
        server = Server.start(0, 0);

        Player first = overRmi("chiara");
        first.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
        assertTrue(first.waitToBeSeated());

        Player second = overSocket("samuele");
        second.send(new LobbyCommand.JoinGame("game-1"));
        assertTrue(second.waitToBeSeated());

        assertEquals(PlayerColor.values()[0], first.colour,
                "the first to arrive gets the first colour, whichever door they used");
        assertEquals(PlayerColor.values()[1], second.colour);
    }

    @Test
    @DisplayName("a nickname is taken across both transports, not once per transport")
    void namesAreSharedAcrossTransports() throws InterruptedException {
        server = Server.start(0, 0);

        Player onSocket = overSocket("samuele");
        // Wait for the name to be registered before the other door tries it, since the point
        // here is the shared registry rather than the race, which the lobby tests cover.
        waitUntil(() -> !onSocket.heard().stream()
                .filter(LobbyEvent.LoggedIn.class::isInstance).toList().isEmpty());

        Player onRmi = overRmi("samuele");
        waitUntil(() -> !onRmi.heard().stream()
                .filter(GameEvent.Rejected.class::isInstance).toList().isEmpty());

        List<GameEvent.Rejected> refusals = onRmi.heard().stream()
                .filter(GameEvent.Rejected.class::isInstance)
                .map(GameEvent.Rejected.class::cast)
                .toList();
        assertEquals("somebody is already called samuele", refusals.get(0).reason());
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) {
        long deadline = System.nanoTime() + PATIENCE.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.onSpinWait();
        }
        throw new AssertionError("that never happened within " + PATIENCE);
    }
}
