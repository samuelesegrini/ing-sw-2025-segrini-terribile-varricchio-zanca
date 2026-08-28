package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.client.state.ClientState;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Builds every window and looks at what came out.
 *
 * <p>This one needs a screen, and the machine that builds this project does not have one — so
 * it starts the toolkit and, if that fails, says why it is skipping rather than passing quietly.
 * A test that silently does nothing on the build machine is worse than no test, because it
 * looks like coverage.
 *
 * <p>Everything that <em>decides</em> anything — which screen, which picture — is a pure
 * function tested next door with no toolkit at all. What is left here is whether the windows
 * are actually built and have the controls a player needs to press.
 *
 * <p>Components involved: {@link Screens}, {@link Screen}, {@link ClientState}.
 */
class ScreensTest {

    private static boolean toolkitStarted;

    @BeforeAll
    static void startTheToolkit() {
        CountDownLatch up = new CountDownLatch(1);
        try {
            Platform.startup(up::countDown);
            toolkitStarted = up.await(10, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyRunning) {
            toolkitStarted = true;
        } catch (UnsupportedOperationException | InterruptedException noScreen) {
            // A build machine with no display. Skipped below, out loud.
            toolkitStarted = false;
            if (noScreen instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } catch (RuntimeException noToolkit) {
            toolkitStarted = false;
        }
    }

    /**
     * Runs something on the JavaFX thread and waits for it.
     *
     * <p>Scene graphs may only be built there, and a test that built one anywhere else would be
     * testing something the running client never does.
     */
    private static <T> T onTheToolkit(java.util.function.Supplier<T> work) {
        AtomicReference<T> result = new AtomicReference<>();
        // Throwable, not RuntimeException. A failed assertion is an Error, and catching only
        // runtime exceptions here meant every assertion inside this block was swallowed by the
        // JavaFX thread and printed to stderr while the test reported success. Found by
        // mutating the code under test and watching nothing fail.
        AtomicReference<Throwable> failed = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(work.get());
            } catch (Throwable broken) {
                failed.set(broken);
            } finally {
                done.countDown();
            }
        });
        try {
            assertTrue(done.await(10, TimeUnit.SECONDS), "the toolkit never got to it");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted waiting for the toolkit");
        }
        if (failed.get() instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (failed.get() instanceof Error error) {
            throw error;
        }
        if (failed.get() != null) {
            throw new AssertionError(failed.get());
        }
        return result.get();
    }

    private static Screens screens() {
        return new Screens(new ClientState(), sent -> { });
    }

    private static List<Node> everythingIn(Parent root) {
        return root.getChildrenUnmodifiable().stream()
                .flatMap(child -> child instanceof Parent nested
                        ? java.util.stream.Stream.concat(java.util.stream.Stream.of(child),
                                everythingIn(nested).stream())
                        : java.util.stream.Stream.of(child))
                .toList();
    }

    @Test
    @DisplayName("every screen builds, including the ones still to be filled in")
    void everyScreenBuilds() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            Screens screens = screens();
            for (Screen screen : Screen.values()) {
                assertNotNull(screens.rootFor(screen), screen + " did not build");
            }
            return null;
        });
    }

    @Test
    @DisplayName("a screen asked for twice is the same screen, not a fresh one")
    void screensAreKept() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            Screens screens = screens();
            // Rebuilding on every change would wipe a half-typed nickname each time somebody
            // else drew a tile.
            assertEquals(screens.rootFor(Screen.LOGIN), screens.rootFor(Screen.LOGIN));
            return null;
        });
    }

    @Test
    @DisplayName("the login screen has somewhere to type a name and something to press")
    void theLoginScreen() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            List<Node> parts = everythingIn(screens().rootFor(Screen.LOGIN));

            assertTrue(parts.stream().anyMatch(node -> node instanceof TextField),
                    "nowhere to type a name");
            assertTrue(parts.stream().anyMatch(node -> node instanceof Button),
                    "nothing to press");
            return null;
        });
    }

    @Test
    @DisplayName("the lobby offers a table for two, a table for four, and a test flight")
    void theLobbyScreen() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            List<String> buttons = everythingIn(screens().rootFor(Screen.LOBBY)).stream()
                    .filter(node -> node instanceof Button)
                    .map(node -> ((Button) node).getText())
                    .toList();

            assertTrue(buttons.stream().anyMatch(text -> text.contains("2")), buttons.toString());
            assertTrue(buttons.stream().anyMatch(text -> text.contains("4")), buttons.toString());
            assertTrue(buttons.stream().anyMatch(text -> text.contains("Test")), buttons.toString());
            assertTrue(buttons.contains("Join"));
            return null;
        });
    }

    @Test
    @DisplayName("an empty name is refused here rather than sent to be refused there")
    void anEmptyName() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            List<it.polimi.ingsw.common.protocol.Command> sent = new java.util.ArrayList<>();
            Screens screens = new Screens(new ClientState(), sent::add);
            Parent login = screens.rootFor(Screen.LOGIN);
            List<Node> parts = everythingIn(login);
            Button play = (Button) parts.stream()
                    .filter(node -> node instanceof Button)
                    .findFirst()
                    .orElseThrow();

            play.fire();

            assertTrue(sent.isEmpty(), "an empty name was sent to the server: " + sent);
            assertTrue(parts.stream()
                    .anyMatch(node -> node instanceof javafx.scene.control.Label label
                            && !label.getText().isBlank()),
                    "and it should be answered on the screen");
            return null;
        });
    }

    @Test
    @DisplayName("a name somebody actually typed is sent")
    void aRealName() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            List<it.polimi.ingsw.common.protocol.Command> sent = new java.util.ArrayList<>();
            Screens screens = new Screens(new ClientState(), sent::add);
            List<Node> parts = everythingIn(screens.rootFor(Screen.LOGIN));
            ((TextField) parts.stream().filter(node -> node instanceof TextField)
                    .findFirst().orElseThrow()).setText("  samuele  ");
            ((Button) parts.stream().filter(node -> node instanceof Button)
                    .findFirst().orElseThrow()).fire();

            assertEquals(1, sent.size(), "expected one login, got " + sent);
            assertEquals(new it.polimi.ingsw.common.protocol.LobbyCommand.Login("samuele"),
                    sent.get(0), "and the spaces around it are not part of anybody's name");
            return null;
        });
    }

    @Test
    @DisplayName("the three screens around the ship are one board, not three")
    void oneBoard() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            Screens screens = screens();

            // Building, repairing and crewing are the same board with different jobs to do to
            // it. Three panes would mean the one being redrawn was not always the one on screen.
            assertEquals(screens.rootFor(Screen.SHIPYARD), screens.rootFor(Screen.REPAIRS));
            assertEquals(screens.rootFor(Screen.REPAIRS), screens.rootFor(Screen.CREW));
            assertNotEquals(screens.rootFor(Screen.SHIPYARD), screens.rootFor(Screen.FLIGHT),
                    "the route is a different picture");
            return null;
        });
    }
}
