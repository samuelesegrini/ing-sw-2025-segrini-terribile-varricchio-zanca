package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.client.network.ServerLink;
import it.polimi.ingsw.client.network.Transport;
import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.common.game.Answers;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.protocol.FlightCommand;
import it.polimi.ingsw.common.protocol.LobbyCommand;
import it.polimi.ingsw.common.protocol.PreparationCommand;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.server.network.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A person types their way to the final ledger.
 *
 * <p>Everything else in this package is a pure function tested on a projection. This is the one
 * that says the pieces fit: a real server, a real socket, a real terminal made of two streams,
 * and a script of the words somebody would actually type.
 *
 * <p>The partner is driven programmatically rather than by typing, because two scripted
 * terminals racing each other through a flight is a test about timing rather than about the
 * interface. What matters here is that the <em>typed</em> side reaches the end.
 *
 * <p>There was a third test here that sat down three times on one connection — finish the ship,
 * wait, declare the crew, wait, look at the route — to watch the route mid-flight. It passed
 * here every time and timed out on the build machine every time, and every assertion in it was
 * already made twice: {@code aWholeGameByTyping} below shows the route is drawn during a real
 * flight, and {@code FlightRenderersTest} checks what it says. A test that only fails where
 * nobody can watch it, and proves nothing the others do not, is worth less than the time spent
 * on it.
 *
 * <p>Components involved: {@link TextInterface}, {@link Server}, {@link ClientState}.
 */
class PlayingAWholeGameTest {

    private static final Duration PATIENCE = Duration.ofSeconds(20);

    /** The same shuffle and the same dice every run, so the flight is the same flight. */
    private static final long SEED = 20260826L;

    private Server server;
    private ServerLink partner;
    private Thread partnerLoop;

    /**
     * A server whose game is the same one every time.
     *
     * <p>{@code Server.start(0, 0)} shuffles from an unseeded generator, so the cards differ on
     * every run — and a script of typed answers that happens to cover one flight will not cover
     * the next. That is fine for a test about connecting and fatal for a test about playing.
     */
    private static Server reproducible() {
        return Server.start(0, 0, it.polimi.ingsw.server.lobby.ServerSettings.defaults()
                .dealtFrom(it.polimi.ingsw.server.data.GameDataLoader.loadBundled())
                .shuffledBy(new java.util.Random(SEED)));
    }

    @AfterEach
    void shutDown() {
        if (partnerLoop != null) {
            partnerLoop.interrupt();
        }
        if (partner != null) {
            partner.close();
        }
        if (server != null) {
            server.close();
        }
    }

    /**
     * Seats a partner who answers everything as dully as the rules allow.
     *
     * <p>Not typed at: a second scripted terminal would make this a test about two scripts
     * keeping pace with each other.
     */
    private void seatThePartner(ClientState state) {
        partner.send(new LobbyCommand.Login("chiara"));
        waitFor(state::nickname);
        partner.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
        waitFor(state::gameId);

        partnerLoop = new Thread(() -> {
            // Driven by what the phase is rather than by what has already been said. A flag
            // saying "I have finished building" is a claim about a command that may have been
            // refused, or lost, or arrived while the game was somewhere else; the phase is a
            // fact. Saying so twice is refused harmlessly, and never saying it at all leaves
            // the fleet on the ground for ever.
            GamePhase said = null;
            while (!Thread.currentThread().isInterrupted()) {
                Optional<GameView> seen = state.game();
                if (seen.isEmpty()) {
                    pause();
                    continue;
                }
                GameView game = seen.orElseThrow();
                if (game.phase() == GamePhase.FINISHED) {
                    return;
                }
                if (game.phase() == GamePhase.BUILDING && said != GamePhase.BUILDING) {
                    said = GamePhase.BUILDING;
                    partner.send(new it.polimi.ingsw.common.protocol.BuildingCommand
                            .FinishBuilding(null));
                } else if (game.phase() == GamePhase.CREW_PLACEMENT
                        && said != GamePhase.CREW_PLACEMENT) {
                    said = GamePhase.CREW_PLACEMENT;
                    partner.send(new PreparationCommand.FinishPreparation());
                } else {
                    game.pendingIfAny()
                            .filter(prompt -> prompt.player() == game.you())
                            .ifPresent(prompt -> partner.send(
                                    new FlightCommand.Answer(Answers.simplestTo(prompt))));
                }
                pause();
            }
        }, "the-other-player");
        partnerLoop.setDaemon(true);
        partnerLoop.start();
    }

    @Test
    @DisplayName("somebody types their way from an empty shipyard to the final ledger")
    void aWholeGameByTyping() {
        server = reproducible();
        ClientState theirs = new ClientState();
        partner = ServerLink.connect(Transport.SOCKET, "localhost", server.socketPort(), theirs);
        seatThePartner(theirs);

        ClientState mine = new ClientState();
        ServerLink link = ServerLink.connect(
                Transport.RMI, "localhost", server.rmiPort(), mine);

        // Everything a person would type to get from nothing to the end of a flight. The
        // answers are the dull ones — decline offers, power nothing, take the hits — because
        // reaching the ledger is the point, not playing well.
        StringBuilder script = new StringBuilder("""
                name samuele
                join game-1
                yard
                done
                done
                route
                """);
        for (int turn = 0; turn < 120; turn++) {
            // One of each answer a card can want. Whichever question is outstanding, exactly one
            // of these is read as an answer and the rest are told they are not — which is the
            // point of reading a line against the prompt rather than against a table of verbs.
            //
            // 'route' is in here rather than once before the loop: typed at a fixed point it is
            // a race with the other player finishing their ship, and a slower machine loses it.
            script.append("leave\npower\nhit\ndone\nkeep 0\nplanet 0\ncrew 7 7\nroute\n");
        }
        script.append("scores\nquit\n");

        ByteArrayOutputStream screen = new ByteArrayOutputStream();
        new TextInterface(mine, link, new BufferedReader(new StringReader(script.toString())),
                new PrintStream(screen, true, StandardCharsets.UTF_8)).run();
        link.close();

        String printed = screen.toString(StandardCharsets.UTF_8);
        // Printed when the client first has a game and never again — not off PhaseBegan, which
        // is never sent for the phase a game opens in, and not off every state, which would
        // repeat it all game.
        assertEquals(1, printed.lines().filter(line -> line.startsWith("── game-1 —")).count(),
                "the line naming the game belongs at the start of it, once");
        assertTrue(printed.contains("Shipyard"), "the shipyard was never drawn");
        assertTrue(printed.contains("Route"), "the route was never drawn");
        assertTrue(printed.contains("Final ledger"),
                "the flight never reached the ledger:\n" + tail(printed));
        assertTrue(printed.contains("wins with"));
    }

    @Test
    @DisplayName("asking for a ledger before there is one says so rather than showing an empty table")
    void noLedgerYet() {
        server = reproducible();
        ClientState theirs = new ClientState();
        partner = ServerLink.connect(Transport.SOCKET, "localhost", server.socketPort(), theirs);
        seatThePartner(theirs);

        ClientState mine = new ClientState();
        ServerLink link = ServerLink.connect(
                Transport.SOCKET, "localhost", server.socketPort(), mine);
        ByteArrayOutputStream screen = new ByteArrayOutputStream();

        // Sit down, then wait for the game to actually be there before asking it anything. The
        // client stops waiting once the server has been quiet for a moment, and joining is
        // answered in two goes — a seat, then the board once the table is full — so a script
        // typed straight through can ask for the ledger while it is still in the lobby.
        type(mine, link, screen, "name samuele\njoin game-1\n");
        waitUntil(() -> mine.game().isPresent());
        type(mine, link, screen, "scores\nquit\n");
        link.close();

        String printed = screen.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("nothing has been settled yet"));
        assertFalse(printed.contains("Final ledger"));
    }

    /**
     * Types a script at a client, leaving the connection open for the next one.
     *
     * <p>The end of the input is the end of the sitting, not the end of the client: a script
     * without a {@code quit} returns from the loop with everything still connected.
     */
    private static void type(ClientState state, ServerLink link, ByteArrayOutputStream screen,
                             String script) {
        new TextInterface(state, link, new BufferedReader(new StringReader(script)),
                new PrintStream(screen, true, StandardCharsets.UTF_8)).run();
    }

    private static String tail(String screen) {
        String[] lines = screen.split("\n");
        int from = Math.max(0, lines.length - 25);
        return String.join("\n", java.util.Arrays.copyOfRange(lines, from, lines.length));
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
            pause();
        }
        throw new AssertionError("that never happened within " + PATIENCE);
    }

    /**
     * Yields for a moment.
     *
     * <p>A sleep rather than a spin. This partner runs for a whole flight, and a hot loop that
     * long takes a core to itself — which on a two-core build machine is half the machine, and
     * turns a one-second test into one that never finishes.
     */
    private static void pause() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
