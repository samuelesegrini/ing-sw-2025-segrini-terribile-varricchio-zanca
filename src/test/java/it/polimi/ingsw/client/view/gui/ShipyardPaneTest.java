package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.common.game.ShipAttributes;
import it.polimi.ingsw.common.game.ValidationReport;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.view.BuildingView;
import it.polimi.ingsw.common.protocol.view.CellView;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.common.protocol.view.PlayerView;
import it.polimi.ingsw.common.protocol.view.ShipView;
import it.polimi.ingsw.common.protocol.view.TileView;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Builds the shipyard with a ship on it and looks at what came out.
 *
 * <p>The valuable one here is the last: a click on a square the ship does not reach must be
 * answered by the board rather than sent to be answered by the server. What is sent is
 * collected in a list and the assertion is that the list is empty.
 *
 * <p>Two earlier versions of that test proved nothing. The first passed a null connection and
 * called that the assertion — but JavaFX swallows what an event handler throws, so a failed
 * send looks exactly like no send. The second fired a mouse event at the pane, which does not
 * reach the handler at all, because the handler is on the board inside it. Both passed with the
 * guards removed, which is how they were found.
 *
 * <p>Components involved: {@link ShipyardPane}, {@link Artwork}, {@link TileImages}.
 */
class ShipyardPaneTest {

    private static final Position CABIN = new Position(2, 3);
    private static boolean toolkitStarted;

    @BeforeAll
    static void startTheToolkit() {
        CountDownLatch up = new CountDownLatch(1);
        try {
            Platform.startup(up::countDown);
            toolkitStarted = up.await(10, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyRunning) {
            toolkitStarted = true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            toolkitStarted = false;
        } catch (RuntimeException noScreen) {
            toolkitStarted = false;
        }
    }

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
            assertTrue(done.await(15, TimeUnit.SECONDS), "the toolkit never got to it");
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

    private static TileView tile(String id) {
        Map<Direction, Connector> connectors = new EnumMap<>(Direction.class);
        for (Direction side : Direction.values()) {
            connectors.put(side, Connector.UNIVERSAL);
        }
        return new TileView(id, ComponentKind.BATTERY, Rotation.NONE, connectors);
    }

    /** A client that has been told about a game in the shipyard, with one tile welded down. */
    private static ClientState building() {
        ShipView ship = new ShipView(5, 7, 5, 4,
                Set.of(CABIN, new Position(2, 4)),
                Map.of(CABIN, new CellView(tile("battery_L-S"), 0, List.of(), 0, null)),
                List.of(), List.of(), 0,
                new ShipAttributes(0, 0, 0), ValidationReport.legal());
        GameView game = new GameView("game-1", GameLevel.LEVEL_II, GamePhase.BUILDING,
                PlayerColor.RED,
                List.of(new PlayerView("samuele", PlayerColor.RED, true, false, 0, ship),
                        new PlayerView("chiara", PlayerColor.BLUE, true, false, 0, ship)),
                new BuildingView(120, List.of(), null, null, List.of(), 0, 3, 60,
                        Set.of(), List.of(1, 2)),
                null, null, List.of());

        ClientState state = new ClientState();
        state.apply(new GameEvent.StateChanged(game));
        return state;
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
    @DisplayName("the board is drawn with the cardboard behind it and the welded tiles on it")
    void theBoardDraws() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            ShipyardPane pane = new ShipyardPane(building(), sent -> { },
                    new TileImages(Artwork.bundled()));
            pane.redraw();

            long pictures = everythingIn(pane).stream()
                    .filter(node -> node instanceof ImageView)
                    .count();
            assertTrue(pictures >= 2,
                    "expected the cardboard and at least one tile, found " + pictures);
            return null;
        });
    }

    @Test
    @DisplayName("everybody else's ship can be looked at, which is what requirement G6 asks for")
    void otherShips() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            ShipyardPane pane = new ShipyardPane(building(), sent -> { },
                    new TileImages(Artwork.bundled()));
            pane.redraw();

            List<String> buttons = everythingIn(pane).stream()
                    .filter(node -> node instanceof javafx.scene.control.Button)
                    .map(node -> ((javafx.scene.control.Button) node).getText())
                    .toList();
            assertTrue(buttons.contains("chiara"), "no way to look at the other ship: " + buttons);
            assertTrue(buttons.contains("Your ship"), "and no way back");
            return null;
        });
    }

    @Test
    @DisplayName("a click the ship does not reach is answered by the board, not sent to the server")
    void clickingOffTheShip() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            List<it.polimi.ingsw.common.protocol.Command> sent = new java.util.ArrayList<>();
            ShipyardPane pane = new ShipyardPane(building(), sent::add,
                    new TileImages(Artwork.bundled()));
            pane.redraw();

            pane.clickedTheBoard(5, 5);

            assertTrue(sent.isEmpty(), "a click in the margin was sent to the server: " + sent);
            assertTrue(everythingIn(pane).stream()
                            .anyMatch(node -> node instanceof Label label
                                    && label.getText().contains("not a square")),
                    "and it should be answered on the screen");
            return null;
        });
    }

    @Test
    @DisplayName("a click on a square the ship does reach is sent")
    void clickingOnTheShip() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            List<it.polimi.ingsw.common.protocol.Command> sent = new java.util.ArrayList<>();
            ShipyardPane pane = new ShipyardPane(building(), sent::add,
                    new TileImages(Artwork.bundled()));
            pane.redraw();

            // The middle of the square next to the starting cabin, which is in the outline.
            double[] at = new BoardGeometry(760, 551, 5, 7).boundsOf(new Position(2, 4));
            pane.clickedTheBoard(at[0] + at[2] / 2, at[1] + at[3] / 2);

            assertTrue(sent.size() == 1, "expected one command, got " + sent);
            assertTrue(sent.get(0) instanceof
                            it.polimi.ingsw.common.protocol.BuildingCommand.PlaceInHand,
                    "an empty square takes a placement, not " + sent.get(0));
            return null;
        });
    }
}
