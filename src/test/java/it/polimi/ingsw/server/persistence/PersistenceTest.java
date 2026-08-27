package it.polimi.ingsw.server.persistence;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.BuildingCommand;
import it.polimi.ingsw.common.protocol.PreparationCommand;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import it.polimi.ingsw.server.model.game.Game;
import it.polimi.ingsw.server.lobby.Clients;
import it.polimi.ingsw.server.lobby.ServerSettings;
import it.polimi.ingsw.server.lobby.Lobby;
import it.polimi.ingsw.server.model.game.Seat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeping a game across a server that stopped.
 *
 * <p>A snapshot here is a recipe rather than a photograph: who was playing, which rules, which
 * shuffle, and everything the game accepted. A game is a deterministic function of those — one
 * thread, one command at a time, a catalogue that ships with the build — so replaying them
 * lands on the same state, down to the next card the deck will turn over.
 *
 * <p>The part that has to be right whatever the format is the writing. A server killed halfway
 * through saving must leave the previous snapshot exactly as it was, because the alternative is
 * players being told they have resumed a game that is half somebody else's.
 */
class PersistenceTest {

    private static final GameData DATA = GameDataLoader.loadBundled();
    private static final InstantSource CLOCK =
            InstantSource.fixed(Instant.parse("2026-08-27T10:00:00Z"));
    private static final long SEED = 20260827L;

    private static List<Seat> twoSeats() {
        return List.of(new Seat("samuele", PlayerColor.BLUE),
                new Seat("chiara", PlayerColor.GREEN));
    }

    private static Game aGame() {
        return Game.create("game-1", GameLevel.LEVEL_II, twoSeats(), DATA,
                new Random(SEED), CLOCK, Duration.ofMinutes(2));
    }

    private static GameSnapshot snapshotOf(Game game) {
        return new GameSnapshot(GameSnapshot.FORMAT, "game-1", GameLevel.LEVEL_II,
                twoSeats().stream()
                        .map(seat -> new GameSnapshot.Seated(seat.nickname(), seat.colour()))
                        .toList(),
                SEED, game.history());
    }

    /** Plays a few turns, so there is something to lose. */
    private static Game partWayThrough() {
        Game game = aGame();
        game.apply(PlayerColor.BLUE, new BuildingCommand.DrawFromPool());
        game.apply(PlayerColor.BLUE, new BuildingCommand.ReturnToPool());
        game.apply(PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));
        game.apply(PlayerColor.GREEN, new BuildingCommand.FinishBuilding(null));
        game.tick();
        game.apply(PlayerColor.BLUE, new PreparationCommand.FinishPreparation());
        game.apply(PlayerColor.GREEN, new PreparationCommand.FinishPreparation());
        game.tick();
        return game;
    }

    @Nested
    @DisplayName("putting a game back")
    class Reloading {

        @Test
        @DisplayName("snapshotThenReload_reproducesTheGameExactly")
        void snapshotThenReloadReproducesTheGameExactly(@TempDir Path directory) {
            Game before = partWayThrough();
            GameView wasLike = before.viewFor(PlayerColor.BLUE);

            SnapshotStore store = new SnapshotStore(directory);
            store.save(snapshotOf(before));

            Game after = Game.restore(store.load("game-1").orElseThrow(), DATA, CLOCK,
                    Duration.ofMinutes(2));
            GameView isLike = after.viewFor(PlayerColor.BLUE);

            assertEquals(wasLike.phase(), isLike.phase());
            assertEquals(wasLike.players().size(), isLike.players().size());
            // The deck too: a game restored with a different shuffle would look right for one
            // card and then diverge, which is the worst way for this to be wrong.
            assertEquals(wasLike.flightIfAny().map(flight -> flight.cardsLeft()),
                    isLike.flightIfAny().map(flight -> flight.cardsLeft()));
            assertEquals(wasLike.flightIfAny().flatMap(flight -> flight.cardIfAny()),
                    isLike.flightIfAny().flatMap(flight -> flight.cardIfAny()),
                    "the same card should be on the table");
            assertEquals(wasLike.pendingIfAny(), isLike.pendingIfAny(),
                    "and the same player should be facing the same question");
        }

        @Test
        @DisplayName("a game put back carries on from where it was, not from the beginning")
        void itKeepsGoing() {
            Game before = partWayThrough();
            GameSnapshot kept = snapshotOf(before);

            Game after = Game.restore(kept, DATA, CLOCK, Duration.ofMinutes(2));

            assertNotEquals(GamePhase.BUILDING, after.phase(),
                    "the shipyard had closed before the crash and should still be closed");
            assertEquals(before.history().size(), after.history().size(),
                    "and it should remember the story it was given");
        }

        @Test
        @DisplayName("a game nobody had touched comes back as a game nobody had touched")
        void anUntouchedGame() {
            Game fresh = aGame();

            Game after = Game.restore(snapshotOf(fresh), DATA, CLOCK, Duration.ofMinutes(2));

            assertEquals(GamePhase.BUILDING, after.phase());
            assertTrue(after.history().isEmpty());
        }
    }

    @Nested
    @DisplayName("writing")
    class Writing {

        @Test
        @DisplayName("crashDuringWrite_leavesThePreviousSnapshotIntact")
        void crashDuringWriteLeavesThePreviousSnapshotIntact(@TempDir Path directory)
                throws IOException {
            SnapshotStore store = new SnapshotStore(directory);
            Game first = partWayThrough();
            store.save(snapshotOf(first));
            int savedTurns = first.history().size();

            // What a process killed mid-save leaves behind: a partial temporary file, and the
            // previous snapshot untouched because nothing was ever written over it.
            Path partial = directory.resolve("game-1.writing");
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(partial))) {
                out.writeObject("half a game, and not even the right type");
            }

            GameSnapshot survived = store.load("game-1").orElseThrow(
                    () -> new AssertionError("the previous snapshot was lost"));

            assertEquals(savedTurns, survived.accepted().size(),
                    "the good snapshot should be exactly as it was");
            assertTrue(Files.exists(partial), "and the wreckage is still there to be cleaned up");
            assertEquals(1, store.count(), "which is not counted as a game");
        }

        @Test
        @DisplayName("saving again replaces the old one rather than adding to a pile")
        void savingTwice(@TempDir Path directory) {
            SnapshotStore store = new SnapshotStore(directory);
            Game game = aGame();
            store.save(snapshotOf(game));
            game.apply(PlayerColor.BLUE, new BuildingCommand.DrawFromPool());
            store.save(snapshotOf(game));

            assertEquals(1, store.count());
            assertEquals(1, store.load("game-1").orElseThrow().accepted().size(),
                    "the newer one should have won");
        }

        @Test
        @DisplayName("a finished game is cleaned up")
        void deleting(@TempDir Path directory) {
            SnapshotStore store = new SnapshotStore(directory);
            store.save(snapshotOf(aGame()));
            assertEquals(1, store.count());

            store.delete("game-1");

            assertEquals(0, store.count());
            assertTrue(store.load("game-1").isEmpty());
        }
    }

    @Nested
    @DisplayName("snapshots this build cannot read")
    class Refusing {

        @Test
        @DisplayName("one from another format is refused rather than half understood")
        void anotherFormat(@TempDir Path directory) {
            SnapshotStore store = new SnapshotStore(directory);
            store.save(new GameSnapshot(GameSnapshot.FORMAT + 1, "game-1", GameLevel.LEVEL_II,
                    List.of(new GameSnapshot.Seated("samuele", PlayerColor.BLUE),
                            new GameSnapshot.Seated("chiara", PlayerColor.GREEN)),
                    SEED, List.of()));

            assertTrue(store.load("game-1").isEmpty(),
                    "half a game loaded is worse than no game loaded");
            assertTrue(store.loadAll().isEmpty());
        }

        @Test
        @DisplayName("a file that is not a snapshot at all is refused, and left where it is")
        void rubbish(@TempDir Path directory) throws IOException {
            Files.writeString(directory.resolve("game-1.snapshot"), "this is not a game");
            SnapshotStore store = new SnapshotStore(directory);

            assertTrue(store.load("game-1").isEmpty());
            assertTrue(Files.exists(directory.resolve("game-1.snapshot")),
                    "deleting evidence of a format change is not this class's decision");
        }

        @Test
        @DisplayName("and the good ones alongside it still load")
        void oneBadOneDoesNotSpoilTheRest(@TempDir Path directory) throws IOException {
            SnapshotStore store = new SnapshotStore(directory);
            store.save(snapshotOf(aGame()));
            Files.writeString(directory.resolve("game-2.snapshot"), "rubbish");

            List<GameSnapshot> loaded = store.loadAll();

            assertEquals(1, loaded.size(), "one unreadable file should not lose the others");
            assertEquals("game-1", loaded.get(0).gameId());
        }
    }

    @Nested
    @DisplayName("a snapshot this build can read but cannot replay")
    class SetAside {

        /**
         * A snapshot whose commands are legal to write down and impossible to replay: giving
         * up a flight that has not launched. Stands in for the real case, which is a rules
         * change under a game somebody had already played.
         */
        private GameSnapshot unreplayable() {
            return new GameSnapshot(GameSnapshot.FORMAT, "game-1", GameLevel.LEVEL_II,
                    List.of(new GameSnapshot.Seated("samuele", PlayerColor.BLUE),
                            new GameSnapshot.Seated("chiara", PlayerColor.GREEN)),
                    SEED, List.of(new GameSnapshot.Recorded(PlayerColor.BLUE,
                            new it.polimi.ingsw.common.protocol.FlightCommand.GiveUp())));
        }

        @Test
        @DisplayName("is moved out of the way rather than deleted, so the evidence survives")
        void itIsRenamedRatherThanRemoved(@TempDir Path directory) {
            new SnapshotStore(directory).save(unreplayable());

            new Lobby(ServerSettings.defaults().dealtFrom(DATA).timedBy(CLOCK)
                    .keeping(directory)).close();

            assertFalse(Files.exists(directory.resolve("game-1.snapshot")),
                    "a game that will never replay should stop being read every startup");
            assertTrue(Files.exists(directory.resolve("game-1.snapshot.broken")),
                    "and the one artifact that would explain why should still be there");
        }

        @Test
        @DisplayName("and is not complained about a second time")
        void itIsReportedOnce(@TempDir Path directory) {
            new SnapshotStore(directory).save(unreplayable());
            new Lobby(ServerSettings.defaults().dealtFrom(DATA).timedBy(CLOCK)
                    .keeping(directory)).close();

            Lobby second = new Lobby(ServerSettings.defaults().dealtFrom(DATA).timedBy(CLOCK)
                    .keeping(directory));

            // The point of setting it aside. A warning printed at every single startup for a
            // game that is never coming back is a warning people learn to scroll past.
            assertEquals(List.of(), second.gamesRunning());
            second.close();
        }

        @Test
        @DisplayName("and one dead game does not stop the live ones coming back")
        void theOthersStillReturn(@TempDir Path directory) {
            SnapshotStore store = new SnapshotStore(directory);
            store.save(unreplayable());
            GameSnapshot live = snapshotOf(aGame());
            store.save(new GameSnapshot(live.version(), "game-2", live.level(), live.seats(),
                    live.seed(), live.accepted()));

            Lobby desk = new Lobby(ServerSettings.defaults().dealtFrom(DATA).timedBy(CLOCK)
                    .keeping(directory));

            assertEquals(List.of("game-2"), desk.gamesRunning());
            desk.close();
        }
    }

    @Nested
    @DisplayName("a server that stopped and started again")
    class AcrossARestart {

        /** A desk that keeps its games in a given directory. */
        private Lobby deskAt(Path directory) {
            return new Lobby(ServerSettings.defaults()
                    .dealtFrom(DATA)
                    .shuffledBy(new Random(SEED))
                    .timedBy(CLOCK)
                    .waiting(Duration.ofMinutes(2))
                    .keeping(directory));
        }

        @Test
        @DisplayName("puts its unfinished games back, and holds the seats under the old names")
        void gamesComeBack(@TempDir Path directory) {
            Lobby before = deskAt(directory);
            Clients.aTableOfTwo(before, "samuele", "chiara");
            // A phase change is a point worth keeping, and starting the game is one.
            assertTrue(before.awaitQuiet(Duration.ofSeconds(3)));
            assertEquals(1, before.gamesRunning().size());
            before.close();

            Lobby after = deskAt(directory);
            try {
                assertEquals(1, after.gamesRunning().size(),
                        "the game should have been picked up again");
                assertTrue(after.gamesRunning().contains("game-1"));
            } finally {
                after.close();
            }
        }

        @Test
        @DisplayName("and a player resumes by logging in with the name they had")
        void playersResume(@TempDir Path directory) {
            Lobby before = deskAt(directory);
            Clients.aTableOfTwo(before, "samuele", "chiara");
            assertTrue(before.awaitQuiet(Duration.ofSeconds(3)));
            before.close();

            Lobby after = deskAt(directory);
            try {
                Clients.Client returning = new Clients.Client(after, "samuele");
                assertTrue(after.awaitQuiet(Duration.ofSeconds(3)));

                assertFalse(returning.only(
                                it.polimi.ingsw.common.protocol.LobbyEvent.JoinedGame.class)
                        .isEmpty(), "logging in with an old name should find the old seat");
                assertFalse(returning.only(
                                it.polimi.ingsw.common.protocol.GameEvent.StateChanged.class)
                        .isEmpty(), "and be answered with the whole picture");
            } finally {
                after.close();
            }
        }

        @Test
        @DisplayName("a finished game is not raised from the dead by the next restart")
        void finishedGamesStayFinished(@TempDir Path directory) {
            Lobby before = deskAt(directory);
            List<Clients.Client> table = Clients.aTableOfTwo(before, "samuele", "chiara");
            assertTrue(before.awaitQuiet(Duration.ofSeconds(3)));
            table.forEach(Clients.Client::hangUp);
            assertTrue(before.awaitQuiet(Duration.ofSeconds(3)));
            before.close();

            Lobby after = deskAt(directory);
            try {
                assertTrue(after.gamesRunning().isEmpty(),
                        "a game everybody abandoned should not come back: "
                                + after.gamesRunning());
            } finally {
                after.close();
            }
        }
    }

    @Nested
    @DisplayName("a snapshot describes a real game")
    class Sanity {

        @Test
        @DisplayName("or is refused when it is made, rather than when it is needed")
        void nonsense() {
            assertFalse(new GameSnapshot(GameSnapshot.FORMAT + 1, "game-1", GameLevel.LEVEL_II,
                    List.of(new GameSnapshot.Seated("samuele", PlayerColor.BLUE)),
                    SEED, List.of()).isReadable());

            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> new GameSnapshot(GameSnapshot.FORMAT, "  ", GameLevel.LEVEL_II,
                            List.of(new GameSnapshot.Seated("samuele", PlayerColor.BLUE)),
                            SEED, List.of()));
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> new GameSnapshot(GameSnapshot.FORMAT, "game-1", GameLevel.LEVEL_II,
                            List.of(), SEED, List.of()));
        }
    }
}
