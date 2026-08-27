package it.polimi.ingsw.server.controller;

import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.BuildingCommand;
import it.polimi.ingsw.common.protocol.Command;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.FlightCommand;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.PreparationCommand;
import it.polimi.ingsw.common.transport.Channel;
import it.polimi.ingsw.common.transport.ChannelListener;
import it.polimi.ingsw.common.transport.LocalChannel;
import it.polimi.ingsw.server.model.game.Game;
import it.polimi.ingsw.server.model.game.Games;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that one game is one thread, and that what comes back is addressed to the right
 * people.
 *
 * <p>Two things here are worth more than the rest. That a command for the wrong phase produces
 * an error a client can read rather than a silence it has to interpret; and that commands
 * arriving at the same moment from different threads resolve as though they had arrived one
 * after another. The second is the whole reason there is not a lock anywhere in the model, and
 * it is not something you can tell by reading the code.
 *
 * <p>The connections are {@link LocalChannel}s. No sockets, no registry, no ports — which is
 * the point of having written one: the controller cannot tell the difference, and neither can
 * this test.
 */
class GameControllerTest {

    private static final Duration PATIENCE = Duration.ofSeconds(2);

    private final List<AutoCloseable> open = new ArrayList<>();

    @AfterEach
    void closeEverything() {
        Collections.reverse(open);
        open.forEach(closeable -> {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // Tearing down a fixture.
            }
        });
        open.clear();
    }

    /** A client end that keeps what it was sent. */
    private static final class Watcher implements ChannelListener<Event> {

        private final List<Event> events = new CopyOnWriteArrayList<>();

        @Override
        public void received(Event event) {
            events.add(event);
        }

        @Override
        public void closed(String reason) {
            // Nothing here depends on being told.
        }

        List<Event> events() {
            return List.copyOf(events);
        }

        <E extends Event> List<E> only(Class<E> kind) {
            return events().stream().filter(kind::isInstance).map(kind::cast).toList();
        }

        void forget() {
            events.clear();
        }
    }

    /** A controller with both players connected, and a way to see what each was sent. */
    private record Table(GameController controller, Watcher atRed, Watcher atBlue,
                         Channel<Command, Event> red, Channel<Command, Event> blue) {

        void send(PlayerColor player, Command command) {
            (player == PlayerColor.RED ? red : blue).send(command);
        }

        void settle() {
            assertTrue(controller.awaitQuiet(PATIENCE), "the game never caught up");
        }
    }

    private Table seated() {
        return seated(Games.levelTwo());
    }

    private Table seated(Game game) {
        GameController controller = new GameController(game);
        open.add(controller);

        Watcher atRed = new Watcher();
        Watcher atBlue = new Watcher();
        // Wired exactly the way a socket or a registry wires a player to a game: the
        // controller is handed the channel while it is being built and says what to do with
        // what arrives on it.
        LocalChannel.Pair<Command, Event> red = LocalChannel.connect(Command.class, Event.class,
                channel -> atRed, channel -> controller.bind(PlayerColor.RED, channel));
        LocalChannel.Pair<Command, Event> blue = LocalChannel.connect(Command.class, Event.class,
                channel -> atBlue, channel -> controller.bind(PlayerColor.BLUE, channel));

        Table table = new Table(controller, atRed, atBlue, red.near(), blue.near());
        table.settle();
        return table;
    }

    @Nested
    @DisplayName("what comes back")
    class Answers {

        @Test
        @DisplayName("joining is answered with the whole picture")
        void bindingSendsTheState() {
            Table table = seated();

            assertFalse(table.atRed().only(GameEvent.StateChanged.class).isEmpty(),
                    "a client that has just connected knows nothing until it is told");
            assertEquals(PlayerColor.RED,
                    table.atRed().only(GameEvent.StateChanged.class).get(0).state().you(),
                    "and the state is addressed to them");
        }

        @Test
        @DisplayName("an accepted command is followed by the whole picture, for everybody")
        void everybodyIsToldTheTruth() {
            Table table = seated();
            table.atRed().forget();
            table.atBlue().forget();

            table.send(PlayerColor.RED, new BuildingCommand.DrawFromPool());
            table.settle();

            assertEquals(1, table.atRed().only(GameEvent.StateChanged.class).size());
            assertEquals(1, table.atBlue().only(GameEvent.StateChanged.class).size(),
                    "the blue player has to know the heap got smaller");
        }

        @Test
        @DisplayName("the state each player is sent is built for them")
        void statesArePrivate() {
            Table table = seated();
            table.atRed().forget();
            table.atBlue().forget();

            table.send(PlayerColor.RED, new BuildingCommand.DrawFromPool());
            table.settle();

            assertTrue(lastState(table.atRed()).buildingIfAny().orElseThrow()
                    .handIfAny().isPresent(), "the red player is holding a tile");
            assertTrue(lastState(table.atBlue()).buildingIfAny().orElseThrow()
                    .handIfAny().isEmpty(), "and the blue player is not");
        }

        @Test
        @DisplayName("a command for the wrong phase is refused with an error, not with silence")
        void commandForTheWrongPhaseIsRejectedWithAnError() {
            Table table = seated();
            table.atRed().forget();
            table.atBlue().forget();

            table.send(PlayerColor.RED, new FlightCommand.GiveUp());
            table.settle();

            List<GameEvent.Rejected> refusals = table.atRed().only(GameEvent.Rejected.class);
            assertEquals(1, refusals.size());
            assertEquals("GiveUp", refusals.get(0).command());
            assertEquals("the ships are still being built", refusals.get(0).reason());
        }

        @Test
        @DisplayName("a refusal goes to the sender and to nobody else")
        void refusalsArePrivate() {
            Table table = seated();
            table.atRed().forget();
            table.atBlue().forget();

            table.send(PlayerColor.RED, new FlightCommand.GiveUp());
            table.settle();

            assertTrue(table.atBlue().events().isEmpty(),
                    "the blue player has nothing to learn from somebody else's mistake");
        }

        @Test
        @DisplayName("a refused command is not followed by a state, because nothing changed")
        void refusalsChangeNothing() {
            Table table = seated();
            table.atRed().forget();

            table.send(PlayerColor.RED, new FlightCommand.GiveUp());
            table.settle();

            assertTrue(table.atRed().only(GameEvent.StateChanged.class).isEmpty());
        }

        @Test
        @DisplayName("a phase change is announced before the state that shows it")
        void phasesAreAnnounced() {
            Table table = seated();
            table.atRed().forget();

            table.send(PlayerColor.RED, new BuildingCommand.FinishBuilding(null));
            table.send(PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));
            table.settle();

            List<GameEvent.PhaseBegan> phases = table.atRed().only(GameEvent.PhaseBegan.class);
            assertEquals(List.of(GamePhase.CREW_PLACEMENT),
                    phases.stream().map(GameEvent.PhaseBegan::phase).toList(),
                    "two legal ships pass straight through validation");
        }

        @Test
        @DisplayName("the end of a game is announced once")
        void theEndIsAnnouncedOnce() {
            Table table = seated();
            table.send(PlayerColor.RED, new BuildingCommand.FinishBuilding(null));
            table.send(PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));
            table.send(PlayerColor.RED, new PreparationCommand.FinishPreparation());
            table.send(PlayerColor.BLUE, new PreparationCommand.FinishPreparation());
            table.send(PlayerColor.RED, new FlightCommand.GiveUp());
            table.send(PlayerColor.BLUE, new FlightCommand.GiveUp());
            table.settle();
            // The hourglass tick is what notices there is nobody left to fly.
            waitFor(() -> table.controller().game().phase() == GamePhase.FINISHED);
            table.settle();

            assertEquals(1, table.atRed().only(GameEvent.GameEnded.class).size());
            assertTrue(lastState(table.atRed()).scoresIfAny().isPresent(),
                    "the ledger travels in the final state");
        }

        private static it.polimi.ingsw.common.protocol.view.GameView lastState(Watcher watcher) {
            List<GameEvent.StateChanged> states = watcher.only(GameEvent.StateChanged.class);
            assertFalse(states.isEmpty(), "nothing was ever sent");
            return states.get(states.size() - 1).state();
        }
    }

    @Nested
    @DisplayName("one thread")
    class OneAtATime {

        @Test
        @DisplayName("commands sent at the same moment resolve in arrival order")
        void concurrentDrawsResolveInArrivalOrder() throws InterruptedException {
            Table table = seated();
            table.atRed().forget();
            int racers = 32;
            CountDownLatch go = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(racers);

            // Every one of these asks the red player to take a tile, and a player can hold one
            // tile. Under a race two threads could both find the hand empty and both fill it;
            // serialized, exactly one succeeds and the other thirty-one are told why.
            for (int racer = 0; racer < racers; racer++) {
                Thread thread = new Thread(() -> {
                    try {
                        go.await();
                        table.send(PlayerColor.RED, new BuildingCommand.DrawFromPool());
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
            table.settle();

            assertEquals(1, table.atRed().only(GameEvent.StateChanged.class).size(),
                    "one draw should have succeeded");
            assertEquals(racers - 1, table.atRed().only(GameEvent.Rejected.class).size(),
                    "and every other should have been told there is already a tile in hand");
        }
    }

    @Nested
    @DisplayName("connections")
    class Connections {

        @Test
        @DisplayName("a player who drops is reported, and the game carries on")
        void dropping() {
            Table table = seated();
            table.atBlue().forget();

            table.red().close();
            table.settle();

            assertFalse(table.controller().isAttached(PlayerColor.RED));
            assertEquals(List.of(false), table.atBlue().only(GameEvent.ConnectionChanged.class)
                    .stream().map(GameEvent.ConnectionChanged::connected).toList());

            table.send(PlayerColor.BLUE, new BuildingCommand.DrawFromPool());
            table.settle();
            assertFalse(table.atBlue().only(GameEvent.StateChanged.class).isEmpty(),
                    "the game does not stop because somebody's laptop did");
        }

        @Test
        @DisplayName("coming back is the same operation as arriving")
        void reconnecting() {
            Table table = seated();
            table.red().close();
            table.settle();

            Watcher again = new Watcher();
            LocalChannel.connect(Command.class, Event.class, channel -> again,
                    channel -> table.controller().bind(PlayerColor.RED, channel));
            table.settle();

            assertTrue(table.controller().isAttached(PlayerColor.RED));
            assertFalse(again.only(GameEvent.StateChanged.class).isEmpty(),
                    "a returning player is sent the state, which is all they need");
        }

        @Test
        @DisplayName("binding twice hangs up on the connection that was replaced")
        void replacingAConnection() {
            Table table = seated();

            Watcher again = new Watcher();
            LocalChannel.connect(Command.class, Event.class, channel -> again,
                    channel -> table.controller().bind(PlayerColor.RED, channel));
            table.settle();

            assertFalse(table.red().isOpen(),
                    "a game talking to a laptop that is not there is worse than one that is not");
        }

        @Test
        @DisplayName("there is no such player")
        void strangers() {
            Table table = seated();

            assertThrows(IllegalArgumentException.class,
                    () -> table.controller().isAttached(PlayerColor.GREEN));
        }
    }

    @Nested
    @DisplayName("writing the game down")
    class Keeping {

        @Test
        @DisplayName("a keeper that fails still lets the batch end with the state")
        void aFailingKeeperStillSendsTheState() {
            AtomicInteger attempts = new AtomicInteger();
            GameController controller = new GameController(Games.levelTwo(), game -> {
                attempts.incrementAndGet();
                throw new UncheckedIOException("the disk is full",
                        new java.io.IOException("no space left on device"));
            });
            open.add(controller);
            Watcher atRed = new Watcher();
            Watcher atBlue = new Watcher();
            LocalChannel.connect(Command.class, Event.class, channel -> atRed,
                    channel -> controller.bind(PlayerColor.RED, channel));
            LocalChannel.connect(Command.class, Event.class, channel -> atBlue,
                    channel -> controller.bind(PlayerColor.BLUE, channel));
            assertTrue(controller.awaitQuiet(PATIENCE));

            // Finishing both ships changes the phase, and a phase change is a save point — so
            // the second of these is a batch in which the keeper is called half way through
            // publish(). The first is not, which is why the assertion below is about the
            // content of the last state and not about how many arrived: a count still grows
            // on the strength of the earlier batch and proves nothing.
            controller.submit(PlayerColor.RED, new BuildingCommand.FinishBuilding(null));
            controller.submit(PlayerColor.BLUE, new BuildingCommand.FinishBuilding(null));
            assertTrue(controller.awaitQuiet(PATIENCE));

            assertNotEquals(GamePhase.BUILDING, controller.game().phase(),
                    "the phase should have moved on, or this test is not exercising a save point");
            assertTrue(attempts.get() > 0, "the keeper should have been called");
            // "Every batch ends with the state" is the rule the whole protocol rests on. An
            // exception out of the keeper abandoned the rest of publish(), so both players
            // were sent a PhaseBegan and then left holding a state that still said BUILDING.
            for (Watcher watcher : List.of(atRed, atBlue)) {
                List<GameEvent.StateChanged> states = watcher.only(GameEvent.StateChanged.class);
                assertFalse(states.isEmpty());
                assertEquals(controller.game().phase(),
                        states.get(states.size() - 1).state().phase(),
                        "the last thing a client was told has to be what is now true");
            }
        }

        @Test
        @DisplayName("closing waits for what was queued, so nothing is written afterwards")
        void closingWaitsForTheQueue() {
            AtomicInteger kept = new AtomicInteger();
            GameController controller = new GameController(Games.levelTwo(), game -> {
                try {
                    // Long enough that a close which only asked the queue to stop would
                    // return before this ever ran — which is exactly what it used to do, and
                    // why a snapshot could land in a directory its caller had already deleted.
                    Thread.sleep(150);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
                kept.incrementAndGet();
            });
            open.add(controller);

            controller.close();

            assertEquals(1, kept.get(),
                    "close() should mean the game has stopped, not that it has been asked to");
        }
    }

    private static void waitFor(java.util.function.BooleanSupplier condition) {
        long deadline = System.nanoTime() + PATIENCE.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                // A sleep rather than a spin: a build machine has two cores and other tests
                // are using them.
                Thread.sleep(1);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertNotNull(null, "the game never got there");
    }
}
