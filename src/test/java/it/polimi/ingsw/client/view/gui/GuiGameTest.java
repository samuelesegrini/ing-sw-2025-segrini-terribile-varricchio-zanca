package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.client.network.ServerLink;
import it.polimi.ingsw.client.network.Transport;
import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import it.polimi.ingsw.common.protocol.PreparationCommand;
import it.polimi.ingsw.server.network.Server;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A game played by pressing the buttons, against a server that is really running.
 *
 * <p>Everything else about the windows is checked without a game behind it: {@link SceneRouter}
 * decides which screen to show and is a pure function, {@link ScreensTest} builds each window and
 * looks at the controls in it. Neither would notice a button wired to nothing, a screen that
 * never follows the state it is given, or a command the server refuses — and a whole game played
 * through the terminal turned up four such faults in one sitting, so the same question is worth
 * asking of the other interface.
 *
 * <p>So this presses the real controls, sends what they send to a real {@link Server}, and reads
 * the screen the router picks afterwards. No robot and no window: the scene graph is built on
 * the toolkit thread and the buttons are fired directly, which is what a click ends up doing.
 *
 * <p>Components involved: {@link Screens}, {@link SceneRouter}, {@link Screen},
 * {@link ClientState}, {@link ServerLink}, {@link Server}.
 */
class GuiGameTest {

    private static final Duration PATIENCE = Duration.ofSeconds(10);

    private static boolean toolkitStarted;

    private Server server;
    private ServerLink partner;
    private ServerLink link;

    @BeforeAll
    static void startTheToolkit() {
        CountDownLatch up = new CountDownLatch(1);
        try {
            Platform.startup(up::countDown);
            toolkitStarted = up.await(10, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyRunning) {
            toolkitStarted = true;
        } catch (UnsupportedOperationException | InterruptedException noScreen) {
            toolkitStarted = false;
            if (noScreen instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } catch (RuntimeException noToolkit) {
            toolkitStarted = false;
        }
    }

    @AfterEach
    void shutDown() {
        if (link != null) {
            link.close();
        }
        if (partner != null) {
            partner.close();
        }
        if (server != null) {
            server.close();
        }
    }

    @Test
    @DisplayName("a player types a name, opens a table and reaches the shipyard, all by pressing")
    void fromTheLoginBoxToTheShipyard() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to press");

        server = Server.start(0, 0);
        ClientState mine = new ClientState();
        link = ServerLink.connect(Transport.SOCKET, "localhost", server.socketPort(), mine);
        Screens screens = onTheToolkit(() -> new Screens(mine, link::send));

        typeInto(screens, Screen.LOGIN, "samuele");
        press(screens, Screen.LOGIN, "Play");
        waitFor(mine::nickname);
        assertEquals("samuele", mine.nickname().orElseThrow(),
                "the login box sent a name the server never agreed to");

        press(screens, Screen.LOBBY, "Open a table for 2");
        waitFor(mine::gameId);

        // Somebody has to sit down opposite, or the shipyard never opens.
        ClientState theirs = new ClientState();
        partner = ServerLink.connect(Transport.SOCKET, "localhost", server.socketPort(), theirs);
        partner.send(new LobbyCommand.Login("chiara"));
        waitFor(theirs::nickname);
        partner.send(new LobbyCommand.JoinGame(mine.gameId().orElseThrow()));
        waitUntil(() -> mine.game().isPresent());

        assertEquals(Screen.SHIPYARD, onTheToolkit(() -> SceneRouter.screenFor(mine.nickname(), mine.game())),
                "two players are seated and the shipyard should be open");
        assertNotNull(onTheToolkit(() -> screens.rootFor(Screen.SHIPYARD)));
    }

    @Test
    @DisplayName("pressing 'Draw from the heap' really does put a tile in the player's hand")
    void drawingATile() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to press");

        Screens screens = seatedInAGame();
        assertTrue(inHand().isEmpty(), "nothing should be in hand before the button is pressed");

        press(screens, Screen.SHIPYARD, "Draw from the heap");
        waitUntil(() -> inHand().isPresent());

        assertTrue(inHand().isPresent(),
                "the draw button is wired to nothing, or the server refused what it sent");
    }

    @Test
    @DisplayName("the window follows the game into crewing")
    void theScreenFollowsThePhase() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to press");

        Screens screens = seatedInAGame();
        reachCrewPlacement(screens);

        assertEquals(Screen.CREW,
                onTheToolkit(() -> SceneRouter.screenFor(state.nickname(), state.game())),
                "the game is being crewed and the window still shows something else");
        assertNotNull(onTheToolkit(() -> screens.rootFor(Screen.CREW)));
    }

    @Test
    @DisplayName("and something on the crewing screen can actually launch the fleet")
    void crewingCanBeFinished() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to press");

        Screens screens = seatedInAGame();
        reachCrewPlacement(screens);
        // The partner is ready, so the fleet launches the moment this player is.
        partner.send(new PreparationCommand.FinishPreparation());

        pressEverythingOn(screens, Screen.CREW);
        waitUntil(() -> phase() == GamePhase.FLIGHT);

        assertEquals(GamePhase.FLIGHT, phase(),
                "nothing on the crewing screen moved the game on. The phase is mandatory, so a "
                        + "player with only this window is stuck here for good; the controls "
                        + "shown are the shipyard's, under a heading about crewing. The buttons "
                        + "pressed were "
                        + onTheToolkit(() -> labelsOn(screens, Screen.CREW)));
    }

    @Test
    @DisplayName("a ship in two pieces is offered a way to choose one")
    void repairsOfferAChoice() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to press");

        // Built rather than played: a ship only comes apart when the connectors of a tile
        // disagree with its neighbour's, and which tile the shuffle deals is not this test's
        // business. What is its business is the window shown once one has.
        ClientState broken = new ClientState();
        List<it.polimi.ingsw.common.protocol.Command> sent = new java.util.ArrayList<>();
        broken.apply(new it.polimi.ingsw.common.protocol.GameEvent.StateChanged(
                aShipInTwoPieces()));
        Screens screens = onTheToolkit(() -> new Screens(broken, sent::add));

        showing(screens, Screen.REPAIRS);
        List<String> offered = onTheToolkit(() -> labelsOn(screens, Screen.REPAIRS));

        assertTrue(offered.stream().anyMatch(label -> label.startsWith("Keep the piece")),
                "the ship is in two pieces and the window offers no way to choose one, so the "
                        + "player is stuck in a phase they must pass through. Offered: " + offered);

        String keep = offered.stream().filter(label -> label.startsWith("Keep the piece"))
                .findFirst().orElseThrow();
        press(screens, Screen.REPAIRS, keep);

        assertTrue(sent.stream().anyMatch(PreparationCommand.KeepPiece.class::isInstance),
                "the button reads like it keeps a piece and sends " + sent);
    }

    @Test
    @DisplayName("and the shipyard's own controls are put away while it is being checked over")
    void theShipyardControlsStandDownDuringRepairs() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to press");

        ClientState broken = new ClientState();
        broken.apply(new it.polimi.ingsw.common.protocol.GameEvent.StateChanged(
                aShipInTwoPieces()));
        Screens screens = onTheToolkit(() -> new Screens(broken, sent -> { }));

        showing(screens, Screen.REPAIRS);
        List<Button> showing = onTheToolkit(() -> controlsOf(screens, Screen.REPAIRS).stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(GuiGameTest::onScreen)
                .toList());

        assertTrue(showing.stream().noneMatch(button ->
                        "Draw from the heap".equals(button.getText())),
                "the shipyard closed a while ago and its buttons are still on show: "
                        + showing.stream().map(Button::getText).toList());
    }

    /** A level II ship whose two components do not reach each other. */
    private static it.polimi.ingsw.common.protocol.view.GameView aShipInTwoPieces() {
        var cabin = new it.polimi.ingsw.common.game.Position(2, 3);
        var adrift = new it.polimi.ingsw.common.game.Position(4, 0);
        var tile = new it.polimi.ingsw.common.protocol.view.TileView("cabin_UUUU",
                it.polimi.ingsw.common.game.ComponentKind.CABIN,
                it.polimi.ingsw.common.game.Rotation.NONE,
                Map.of(it.polimi.ingsw.common.game.Direction.NORTH,
                                it.polimi.ingsw.common.game.Connector.UNIVERSAL,
                        it.polimi.ingsw.common.game.Direction.EAST,
                                it.polimi.ingsw.common.game.Connector.UNIVERSAL,
                        it.polimi.ingsw.common.game.Direction.SOUTH,
                                it.polimi.ingsw.common.game.Connector.UNIVERSAL,
                        it.polimi.ingsw.common.game.Direction.WEST,
                                it.polimi.ingsw.common.game.Connector.UNIVERSAL));
        var ship = new it.polimi.ingsw.common.protocol.view.ShipView(5, 7, 5, 4,
                java.util.Set.of(cabin, adrift),
                Map.of(cabin, new it.polimi.ingsw.common.protocol.view.CellView(
                                tile, 0, List.of(), 2, null),
                        adrift, new it.polimi.ingsw.common.protocol.view.CellView(
                                tile, 0, List.of(), 0, null)),
                List.of(java.util.Set.of(cabin), java.util.Set.of(adrift)),
                List.of(), 0,
                new it.polimi.ingsw.common.game.ShipAttributes(0, 0, 2),
                new it.polimi.ingsw.common.game.ValidationReport(List.of()));
        return new it.polimi.ingsw.common.protocol.view.GameView("game-1", GameLevel.LEVEL_II,
                GamePhase.VALIDATION, it.polimi.ingsw.common.game.PlayerColor.BLUE,
                List.of(new it.polimi.ingsw.common.protocol.view.PlayerView("samuele",
                        it.polimi.ingsw.common.game.PlayerColor.BLUE, true, false, 0, ship)),
                null, null, null, List.of());
    }

    /**
     * Finds a square on the ship's outline with no neighbour already welded down.
     *
     * <p>A tile put there is its own piece whatever tile it turns out to be, which is how this
     * reaches the validation phase without depending on the shuffle.
     *
     * @return the square
     */
    private it.polimi.ingsw.common.game.Position somewhereItTouchesNothing() {
        var ship = myShip();
        return ship.outline().stream()
                .filter(cell -> !ship.cells().containsKey(cell))
                .filter(cell -> ship.cells().keySet().stream().noneMatch(taken ->
                        Math.abs(taken.row() - cell.row())
                                + Math.abs(taken.column() - cell.column()) == 1))
                .findFirst()
                .orElseThrow(() -> new AssertionError("nowhere on this board stands alone"));
    }

    private it.polimi.ingsw.common.protocol.view.ShipView myShip() {
        return state.game()
                .flatMap(seen -> seen.players().stream()
                        .filter(player -> player.colour() == seen.you())
                        .findFirst())
                .orElseThrow()
                .ship();
    }

    private int piecesOfMyShip() {
        return state.game()
                .flatMap(seen -> seen.players().stream()
                        .filter(player -> player.colour() == seen.you())
                        .findFirst())
                .map(player -> player.ship().pieces().size())
                .orElse(0);
    }

    /** Gets both ships out of the shipyard, which a starting cabin alone is allowed to do. */
    private void reachCrewPlacement(Screens screens) {
        press(screens, Screen.SHIPYARD, "Finished building");
        partner.send(new it.polimi.ingsw.common.protocol.BuildingCommand.FinishBuilding(null));
        waitUntil(() -> phase() == GamePhase.CREW_PLACEMENT);
    }

    // ---------------------------------------------------------------- the table

    private ClientState state;

    /**
     * Gets a player seated in a started two-player game, with the windows built.
     *
     * @return the screens, ready to press
     */
    private Screens seatedInAGame() {
        server = Server.start(0, 0);
        state = new ClientState();
        link = ServerLink.connect(Transport.SOCKET, "localhost", server.socketPort(), state);
        Screens screens = onTheToolkit(() -> new Screens(state, link::send));

        typeInto(screens, Screen.LOGIN, "samuele");
        press(screens, Screen.LOGIN, "Play");
        waitFor(state::nickname);
        press(screens, Screen.LOBBY, "Open a table for 2");
        waitFor(state::gameId);

        ClientState theirs = new ClientState();
        partner = ServerLink.connect(Transport.SOCKET, "localhost", server.socketPort(), theirs);
        partner.send(new LobbyCommand.Login("chiara"));
        waitFor(theirs::nickname);
        partner.send(new LobbyCommand.JoinGame(state.gameId().orElseThrow()));
        waitUntil(() -> state.game().isPresent());
        return screens;
    }

    private GamePhase phase() {
        return state.game().map(seen -> seen.phase()).orElse(null);
    }

    private Optional<?> inHand() {
        return state.game()
                .flatMap(it.polimi.ingsw.common.protocol.view.GameView::buildingIfAny)
                .flatMap(it.polimi.ingsw.common.protocol.view.BuildingView::handIfAny);
    }

    // ---------------------------------------------------------------- pressing

    /**
     * Says whether a node is actually on show.
     *
     * <p>Not {@link Node#isVisible()}: that is the node's own flag, and a button inside a box
     * that has been hidden still reports true. A player sees it only if nothing above it is
     * hidden either.
     *
     * @param node the node
     * @return whether a player would see it
     */
    private static boolean onScreen(Node node) {
        for (Node walking = node; walking != null; walking = walking.getParent()) {
            if (!walking.isVisible()) {
                return false;
            }
        }
        return true;
    }

    /** Builds a screen and refreshes it, which is what the window does when it comes up. */
    private static void showing(Screens screens, Screen screen) {
        onTheToolkit(() -> {
            screens.rootFor(screen);
            screens.update(screen);
            return null;
        });
    }

    private static void pressEverythingOn(Screens screens, Screen screen) {
        onTheToolkit(() -> {
            controlsOf(screens, screen).stream()
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .forEach(Button::fire);
            return null;
        });
    }

    private static List<String> labelsOn(Screens screens, Screen screen) {
        return controlsOf(screens, screen).stream()
                .filter(Button.class::isInstance)
                .map(node -> ((Button) node).getText())
                .toList();
    }

    private static void press(Screens screens, Screen screen, String label) {
        onTheToolkit(() -> {
            Button button = controlsOf(screens, screen).stream()
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .filter(candidate -> label.equals(candidate.getText()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "no button reading '" + label + "' on " + screen + "; there is "
                                    + controlsOf(screens, screen).stream()
                                    .filter(Button.class::isInstance)
                                    .map(node -> ((Button) node).getText())
                                    .toList()));
            button.fire();
            return null;
        });
    }

    private static void typeInto(Screens screens, Screen screen, String text) {
        onTheToolkit(() -> {
            TextField field = controlsOf(screens, screen).stream()
                    .filter(TextField.class::isInstance)
                    .map(TextField.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no box to type into on " + screen));
            field.setText(text);
            return null;
        });
    }

    private static List<Node> controlsOf(Screens screens, Screen screen) {
        return everythingIn(screens.rootFor(screen));
    }

    private static List<Node> everythingIn(Parent root) {
        return root.getChildrenUnmodifiable().stream()
                .flatMap(child -> child instanceof Parent nested
                        ? java.util.stream.Stream.concat(java.util.stream.Stream.of(child),
                                everythingIn(nested).stream())
                        : java.util.stream.Stream.of(child))
                .toList();
    }

    private static <T> T onTheToolkit(Supplier<T> work) {
        AtomicReference<T> result = new AtomicReference<>();
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

    private static void waitFor(Supplier<Optional<?>> ready) {
        waitUntil(() -> ready.get().isPresent());
    }

    private static void waitUntil(BooleanSupplier condition) {
        long deadline = System.nanoTime() + PATIENCE.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
