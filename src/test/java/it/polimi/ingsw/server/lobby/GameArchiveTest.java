package it.polimi.ingsw.server.lobby;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.BuildingCommand;
import it.polimi.ingsw.server.controller.GameController;
import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import it.polimi.ingsw.server.model.game.Seat;
import it.polimi.ingsw.server.persistence.GameSnapshot;
import it.polimi.ingsw.server.persistence.SnapshotStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where a game comes into being, driven without a desk in front of it.
 *
 * <p>The point of the module is that dealing a game and recovering one are the same four steps
 * and used to be written out twice. So these tests do both through the one interface and
 * never mention a {@link Lobby}: a test that reached through a lobby to get here would be
 * testing the thing that used to have two copies of this, which is what the arrangement is
 * meant to make impossible.
 *
 * <p>Components involved: {@link GameArchive}, {@link ServerSettings},
 * {@link it.polimi.ingsw.server.persistence.SnapshotStore}.
 */
class GameArchiveTest {

    private static final GameData DATA = GameDataLoader.loadBundled();
    private static final InstantSource CLOCK =
            InstantSource.fixed(Instant.parse("2026-08-28T10:00:00Z"));
    private static final Duration PATIENCE = Duration.ofSeconds(3);

    private final List<GameController> running = new ArrayList<>();

    @AfterEach
    void closeWhatWasOpened() {
        running.forEach(GameController::close);
        running.clear();
    }

    private GameArchive keepingIn(Path directory) {
        return new GameArchive(ServerSettings.defaults()
                .dealtFrom(DATA)
                .shuffledBy(new Random(20260828L))
                .timedBy(CLOCK)
                .keeping(directory));
    }

    private static List<Seat> twoSeats() {
        return List.of(new Seat("samuele", PlayerColor.BLUE),
                new Seat("chiara", PlayerColor.GREEN));
    }

    private GameController deal(GameArchive archive) {
        GameController controller = archive.deal("game-1", GameLevel.LEVEL_II, twoSeats());
        running.add(controller);
        assertTrue(controller.awaitQuiet(PATIENCE));
        return controller;
    }

    @Nested
    @DisplayName("dealing")
    class Dealing {

        @Test
        @DisplayName("writes the game down before anybody has done anything in it")
        void aDealtGameIsAlreadyRecoverable(@TempDir Path directory) {
            deal(keepingIn(directory));

            // Recoverable from the moment it is dealt, not from the moment somebody first
            // does something: a server that stopped between the two would otherwise lose a
            // table that was already seated.
            assertTrue(Files.exists(directory.resolve("game-1.snapshot")));
        }

        @Test
        @DisplayName("hands back something already running the game")
        void aDealtGameIsRunning(@TempDir Path directory) {
            GameController controller = deal(keepingIn(directory));

            assertEquals("game-1", controller.game().id());
            assertEquals(twoSeats(), controller.seats());
        }
    }

    @Nested
    @DisplayName("recovering")
    class Recovering {

        @Test
        @DisplayName("brings a game back with what was played in it")
        void aGameComesBackWhereItWasLeft(@TempDir Path directory) {
            GameController before = deal(keepingIn(directory));
            before.submit(PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));
            before.submit(PlayerColor.GREEN, new BuildingCommand.FinishBuilding(null));
            assertTrue(before.awaitQuiet(PATIENCE));
            before.close();

            // A different archive over the same directory, which is what starting the jar
            // again amounts to.
            List<GameController> back = keepingIn(directory).recoverAll();
            running.addAll(back);

            assertEquals(1, back.size());
            assertEquals(before.game().phase(), back.get(0).game().phase(),
                    "a recovered game is the same game, not a fresh one with the same name");
            assertEquals(twoSeats(), back.get(0).seats());
        }

        @Test
        @DisplayName("finds nothing when nothing was kept")
        void anEmptyArchiveRecoversNothing(@TempDir Path directory) {
            assertEquals(List.of(), keepingIn(directory).recoverAll());
        }

        @Test
        @DisplayName("sets aside one that will not replay, and still brings back the rest")
        void oneDeadGameDoesNotStopTheOthers(@TempDir Path directory) {
            deal(keepingIn(directory)).close();
            // Legal to write down and impossible to replay: giving up a flight that never
            // launched. Stands in for the real case, a rules change under a played game.
            new SnapshotStore(directory).save(new GameSnapshot(GameSnapshot.FORMAT, "game-2",
                    GameLevel.LEVEL_II,
                    List.of(new GameSnapshot.Seated("samuele", PlayerColor.BLUE),
                            new GameSnapshot.Seated("chiara", PlayerColor.GREEN)),
                    20260828L,
                    List.of(new GameSnapshot.Recorded(PlayerColor.BLUE,
                            new it.polimi.ingsw.common.protocol.FlightCommand.GiveUp()))));

            List<GameController> back = keepingIn(directory).recoverAll();
            running.addAll(back);

            assertEquals(List.of("game-1"), back.stream().map(c -> c.game().id()).toList());
            assertTrue(Files.exists(directory.resolve("game-2.snapshot.broken")),
                    "the evidence is kept, just not read again");
            assertFalse(Files.exists(directory.resolve("game-2.snapshot")));
        }
    }

    @Nested
    @DisplayName("forgetting")
    class Forgetting {

        @Test
        @DisplayName("a finished game does not come back from the dead")
        void aForgottenGameStaysGone(@TempDir Path directory) {
            GameArchive archive = keepingIn(directory);
            deal(archive).close();

            archive.forget("game-1");

            assertFalse(Files.exists(directory.resolve("game-1.snapshot")));
            assertEquals(List.of(), archive.recoverAll());
        }
    }
}
