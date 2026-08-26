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
 */
class PlayingAWholeGameTest {

    private static final Duration PATIENCE = Duration.ofSeconds(20);

    private Server server;
    private ServerLink partner;
    private Thread partnerLoop;

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
            boolean built = false;
            boolean crewed = false;
            while (!Thread.currentThread().isInterrupted()) {
                Optional<GameView> seen = state.game();
                if (seen.isEmpty()) {
                    Thread.onSpinWait();
                    continue;
                }
                GameView game = seen.orElseThrow();
                if (game.phase() == GamePhase.FINISHED) {
                    return;
                }
                if (game.phase() == GamePhase.BUILDING && !built) {
                    built = true;
                    partner.send(new it.polimi.ingsw.common.protocol.BuildingCommand
                            .FinishBuilding(null));
                } else if (game.phase() == GamePhase.CREW_PLACEMENT && !crewed) {
                    crewed = true;
                    partner.send(new PreparationCommand.FinishPreparation());
                } else {
                    game.pendingIfAny()
                            .filter(prompt -> prompt.player() == game.you())
                            .ifPresent(prompt -> partner.send(
                                    new FlightCommand.Answer(Answers.simplestTo(prompt))));
                }
                Thread.onSpinWait();
            }
        }, "the-other-player");
        partnerLoop.setDaemon(true);
        partnerLoop.start();
    }

    @Test
    @DisplayName("somebody types their way from an empty shipyard to the final ledger")
    void aWholeGameByTyping() {
        server = Server.start(0, 0);
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
        for (int turn = 0; turn < 300; turn++) {
            script.append("leave\npower\nhit\ndone\n");
        }
        script.append("scores\nquit\n");

        ByteArrayOutputStream screen = new ByteArrayOutputStream();
        new TextInterface(mine, link, new BufferedReader(new StringReader(script.toString())),
                new PrintStream(screen, true, StandardCharsets.UTF_8)).run();
        link.close();

        String printed = screen.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Shipyard"), "the shipyard was never drawn");
        assertTrue(printed.contains("Route"), "the route was never drawn");
        assertTrue(printed.contains("Final ledger"),
                "the flight never reached the ledger:\n" + tail(printed));
        assertTrue(printed.contains("wins with"));
    }

    @Test
    @DisplayName("the route can be looked at whenever, and says who plays first")
    void lookingAtTheRoute() {
        server = Server.start(0, 0);
        ClientState theirs = new ClientState();
        partner = ServerLink.connect(Transport.SOCKET, "localhost", server.socketPort(), theirs);
        seatThePartner(theirs);

        ClientState mine = new ClientState();
        ServerLink link = ServerLink.connect(
                Transport.SOCKET, "localhost", server.socketPort(), mine);
        ByteArrayOutputStream screen = new ByteArrayOutputStream();
        new TextInterface(mine, link,
                new BufferedReader(new StringReader(
                        "name samuele\njoin game-1\ndone\ndone\nroute\nquit\n")),
                new PrintStream(screen, true, StandardCharsets.UTF_8)).run();
        link.close();

        String printed = screen.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("spaces"));
        assertTrue(printed.contains("cards left"));
        assertTrue(printed.contains("leading"), "somebody is always in front");
    }

    @Test
    @DisplayName("asking for a ledger before there is one says so rather than showing an empty table")
    void noLedgerYet() {
        server = Server.start(0, 0);
        ClientState theirs = new ClientState();
        partner = ServerLink.connect(Transport.SOCKET, "localhost", server.socketPort(), theirs);
        seatThePartner(theirs);

        ClientState mine = new ClientState();
        ServerLink link = ServerLink.connect(
                Transport.SOCKET, "localhost", server.socketPort(), mine);
        ByteArrayOutputStream screen = new ByteArrayOutputStream();
        new TextInterface(mine, link,
                new BufferedReader(new StringReader("name samuele\njoin game-1\nscores\nquit\n")),
                new PrintStream(screen, true, StandardCharsets.UTF_8)).run();
        link.close();

        String printed = screen.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("nothing has been settled yet"));
        assertFalse(printed.contains("Final ledger"));
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
            Thread.onSpinWait();
        }
        throw new AssertionError("that never happened within " + PATIENCE);
    }
}
