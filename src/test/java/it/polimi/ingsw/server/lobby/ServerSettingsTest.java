package it.polimi.ingsw.server.lobby;

import it.polimi.ingsw.server.persistence.Snapshots;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * What a server is set up with, and what it gets if nobody says.
 *
 * <p>The default is the interesting part. Four {@code Lobby} constructors used to end in a
 * nullable {@code Path} whose {@code null} meant "keep no games", and the shipped server could
 * not reach the one that kept any — so what a forgetful caller gets is worth pinning rather
 * than assuming.
 *
 * <p>Components involved: {@link ServerSettings}, {@link Snapshots}, {@link Lobby}.
 */
class ServerSettingsTest {

    @Nested
    @DisplayName("what a caller gets without asking")
    class Defaults {

        @Test
        @DisplayName("keeps no games, so a test never writes into whatever directory it ran in")
        void defaultsKeepNothing() {
            // Not merely "somewhere harmless": the same object every time, so nothing can be
            // written and nothing can be recovered from another test's leftovers.
            assertSame(Snapshots.NONE, ServerSettings.defaults().snapshots());
        }

        @Test
        @DisplayName("carries games on when somebody drops, which is the baseline rule")
        void defaultsCarryOn() {
            assertEquals(DisconnectionPolicy.GAME_CARRIES_ON,
                    ServerSettings.defaults().onDisconnection());
        }

        @Test
        @DisplayName("waits the game's own timeout for a last player")
        void defaultsWaitTheGamesTimeout() {
            assertEquals(it.polimi.ingsw.server.model.game.Game.DEFAULT_SOLO_TIMEOUT,
                    ServerSettings.defaults().soloTimeout());
        }
    }

    @Nested
    @DisplayName("refining one setting at a time")
    class Refining {

        @Test
        @DisplayName("keeping a directory produces settings that keep games")
        void keepingADirectory(@TempDir Path directory) {
            ServerSettings kept = ServerSettings.defaults().keeping(directory);

            assertNotSame(Snapshots.NONE, kept.snapshots());
        }

        @Test
        @DisplayName("changing one setting leaves the rest alone")
        void refiningIsNarrow() {
            ServerSettings before = ServerSettings.defaults();
            Random shuffle = new Random(20260827L);

            ServerSettings after = before.shuffledBy(shuffle).waiting(Duration.ofSeconds(30));

            assertSame(shuffle, after.random());
            assertEquals(Duration.ofSeconds(30), after.soloTimeout());
            assertSame(before.data(), after.data());
            assertSame(before.clock(), after.clock());
            assertSame(before.snapshots(), after.snapshots());
        }

        @Test
        @DisplayName("and leaves the settings it was refined from untouched")
        void refiningDoesNotMutate() {
            ServerSettings before = ServerSettings.defaults();

            before.waiting(Duration.ofSeconds(30));

            // A launcher that offered somebody a choice and then let them go back would be
            // holding a value that had already changed under it.
            assertEquals(it.polimi.ingsw.server.model.game.Game.DEFAULT_SOLO_TIMEOUT,
                    before.soloTimeout());
        }
    }

    @Nested
    @DisplayName("refusing settings that make no sense")
    class Refusing {

        @Test
        @DisplayName("keeping nothing is Snapshots.NONE, never null")
        void nullSnapshotsAreRefused() {
            ServerSettings valid = ServerSettings.defaults();

            assertThrows(NullPointerException.class, () -> new ServerSettings(valid.data(),
                    valid.random(), valid.clock(), valid.onDisconnection(), valid.soloTimeout(),
                    null));
        }

        @Test
        @DisplayName("a game cannot wait no time at all for its last player")
        void anEmptyTimeoutIsRefused() {
            assertThrows(IllegalArgumentException.class,
                    () -> ServerSettings.defaults().waiting(Duration.ZERO));
        }
    }
}
