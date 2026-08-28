package it.polimi.ingsw.client.view.gui;

import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.AdventureCardType;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.Hit;
import it.polimi.ingsw.common.game.HitKind;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.ScoreSheet;
import it.polimi.ingsw.common.game.ShipAttributes;
import it.polimi.ingsw.common.game.ValidationReport;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.FlightCommand;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.view.FlightView;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.common.protocol.view.PlayerView;
import it.polimi.ingsw.common.protocol.view.ShipView;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The flight and the ledger, built and pressed.
 *
 * <p>What each button answers is {@link PromptChoices}' business and is checked next door with
 * no toolkit. What is checked here is that pressing one sends that answer and nothing else, and
 * that the ledger shows the lines the manual scores separately.
 *
 * <p>Components involved: {@link FlightPane}, {@link LedgerPane}, {@link GameView}.
 */
class FlightAndLedgerTest {

    private static final Position CABIN = new Position(2, 3);
    private static final Position HOLD = new Position(2, 4);
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
        // Throwable: a failed assertion is an Error, and catching only runtime exceptions here
        // would swallow every assertion in this file.
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

    private static ShipView ship() {
        return new ShipView(5, 7, 5, 4, Set.of(CABIN, HOLD), Map.of(), List.of(), List.of(), 0,
                new ShipAttributes(0, 0, 0), ValidationReport.legal());
    }

    private static ClientState flying(PlayerPrompt asking, List<ScoreSheet> scores,
                                      GamePhase phase) {
        Map<PlayerColor, Integer> where = new LinkedHashMap<>();
        where.put(PlayerColor.RED, 9);
        where.put(PlayerColor.BLUE, 6);
        GameView game = new GameView("game-1", GameLevel.LEVEL_II, phase, PlayerColor.RED,
                List.of(new PlayerView("samuele", PlayerColor.RED, true, false, 0, ship()),
                        new PlayerView("chiara", PlayerColor.BLUE, true, false, 0, ship())),
                null,
                new FlightView(24, where, List.of(PlayerColor.RED, PlayerColor.BLUE),
                        new AdventureCardIdentity("pirates_lvl2", AdventureCardType.PIRATES,
                                CardLevel.LEVEL_II, false), 7),
                asking, scores);
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

    private static List<Button> buttonsIn(Parent root) {
        return everythingIn(root).stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
                .toList();
    }

    private static String textIn(Parent root) {
        return everythingIn(root).stream()
                .filter(node -> node instanceof Label)
                .map(node -> ((Label) node).getText())
                .reduce((left, right) -> left + " | " + right)
                .orElse("");
    }

    @Test
    @DisplayName("the card on the table is shown as the card, not as its name")
    void theCard() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            FlightPane pane = new FlightPane(
                    flying(null, List.of(), GamePhase.FLIGHT), sent -> { },
                    new TileImages(Artwork.bundled()));
            pane.redraw();

            assertTrue(everythingIn(pane).stream().anyMatch(node -> node instanceof ImageView),
                    "the card artwork was not drawn");
            assertTrue(textIn(pane).contains("pirates"));
            return null;
        });
    }

    @Test
    @DisplayName("the route says where everybody is and who is in front")
    void theRoute() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            FlightPane pane = new FlightPane(
                    flying(null, List.of(), GamePhase.FLIGHT), sent -> { },
                    new TileImages(Artwork.bundled()));
            pane.redraw();

            String text = textIn(pane);
            assertTrue(text.contains("space 9"), text);
            assertTrue(text.contains("leading"), text);
            assertTrue(text.contains("3 behind"), "how far back decides whether to spend a battery");
            return null;
        });
    }

    @Test
    @DisplayName("pressing an answer sends that answer, and only it")
    void pressingAnAnswer() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            List<Command> sent = new ArrayList<>();
            PlayerPrompt shot = new PlayerPrompt.ChooseDefence(PlayerColor.RED,
                    new Hit(HitKind.SMALL_METEOR, Direction.NORTH, 7), CABIN, Set.of(HOLD));
            FlightPane pane = new FlightPane(flying(shot, List.of(), GamePhase.FLIGHT), sent::add,
                    new TileImages(Artwork.bundled()));
            pane.redraw();

            buttonsIn(pane).stream()
                    .filter(button -> button.getText().equals("Take the hit"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no way to take the hit"))
                    .fire();

            assertEquals(1, sent.size(), "expected one answer, got " + sent);
            assertEquals(new FlightCommand.Answer(
                            PlayerChoice.DefenceChosen.none(PlayerColor.RED)), sent.get(0));
            return null;
        });
    }

    @Test
    @DisplayName("a shot nothing can stop offers no way to stop it")
    void noFalseChoices() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            PlayerPrompt shot = new PlayerPrompt.ChooseDefence(PlayerColor.RED,
                    new Hit(HitKind.HEAVY_FIRE, Direction.NORTH, 7), CABIN, Set.of());
            FlightPane pane = new FlightPane(flying(shot, List.of(), GamePhase.FLIGHT),
                    sent -> { }, new TileImages(Artwork.bundled()));
            pane.redraw();

            List<String> answers = buttonsIn(pane).stream()
                    .map(Button::getText)
                    .filter(text -> !text.equals("Leave the route"))
                    .toList();
            assertEquals(List.of("Take the hit"), answers,
                    "a button the server would refuse teaches the wrong game");
            return null;
        });
    }

    @Test
    @DisplayName("somebody else's turn says whose, and offers nothing to press")
    void waitingForSomebodyElse() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            PlayerPrompt theirs = new PlayerPrompt.TakeOrLeave(PlayerColor.BLUE, "salvage", 1);
            FlightPane pane = new FlightPane(flying(theirs, List.of(), GamePhase.FLIGHT),
                    sent -> { }, new TileImages(Artwork.bundled()));
            pane.redraw();

            assertTrue(textIn(pane).contains("Waiting for chiara"), textIn(pane));
            assertFalse(buttonsIn(pane).stream()
                            .anyMatch(button -> button.getText().equals("Take it")),
                    "another ship's decision is not this player's to make");
            return null;
        });
    }

    @Test
    @DisplayName("the ledger shows every line the manual scores separately")
    void theLedger() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            List<ScoreSheet> scores = List.of(
                    new ScoreSheet(PlayerColor.RED, true, 4, 4, 9, 12, 1),
                    new ScoreSheet(PlayerColor.BLUE, false, 0, 0, 2, 3, 6));
            LedgerPane pane = new LedgerPane(flying(null, scores, GamePhase.FINISHED));
            pane.redraw();

            String text = textIn(pane);
            assertTrue(text.contains("prettiest"), text);
            assertTrue(text.contains("-1"), "losses are a subtraction, not folded into a total");
            assertTrue(text.contains("-6"), text);
            assertTrue(text.contains("samuele wins with"), text);
            return null;
        });
    }

    @Test
    @DisplayName("a bonus that does not apply is a dash, which is not the same as a nought")
    void notApplicable() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            LedgerPane pane = new LedgerPane(flying(null,
                    List.of(new ScoreSheet(PlayerColor.RED, false, 0, 0, 2, 3, 0)),
                    GamePhase.FINISHED));
            pane.redraw();

            assertTrue(textIn(pane).contains("—"),
                    "'not applicable' and 'you earned nothing' are different statements");
            return null;
        });
    }

    @Test
    @DisplayName("a ledger with nothing in it yet says the scores are being worked out")
    void beforeTheScores() {
        assumeTrue(toolkitStarted, "no display on this machine, so no windows to build");

        onTheToolkit(() -> {
            LedgerPane pane = new LedgerPane(flying(null, List.of(), GamePhase.SCORING));
            pane.redraw();

            assertTrue(textIn(pane).contains("being worked out"), textIn(pane));
            return null;
        });
    }
}
