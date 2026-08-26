package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.client.network.ServerLink;
import it.polimi.ingsw.client.network.Transport;
import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.protocol.BuildingCommand;
import it.polimi.ingsw.common.protocol.LobbyCommand;
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
 * Getting a ship from the shipyard onto the starting line, by typing.
 *
 * <p>These two phases had no commands at all until a test tried to play through them and found
 * the game stuck in crew placement with nothing that would move it. Every screen was drawn and
 * every renderer was tested; there was simply no way to say anything.
 *
 * <p>That is the shape of gap a renderer test cannot find and a whole-game test can.
 */
class RepairAndCrewByTypingTest {

    private static final Duration PATIENCE = Duration.ofSeconds(10);

    private Server server;
    private ServerLink partner;

    @AfterEach
    void shutDown() {
        if (partner != null) {
            partner.close();
        }
        if (server != null) {
            server.close();
        }
    }

    /**
     * Opens a two-player game with a partner who finishes building at once, and types a script.
     *
     * @param typed what a person would type, after joining
     * @return everything the client printed
     */
    private String playing(String... typed) {
        server = Server.start(0, 0);

        ClientState theirs = new ClientState();
        partner = ServerLink.connect(Transport.SOCKET, "localhost", server.socketPort(), theirs);
        partner.send(new LobbyCommand.Login("chiara"));
        waitFor(theirs::nickname);
        partner.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
        waitFor(theirs::gameId);

        ClientState mine = new ClientState();
        ServerLink link = ServerLink.connect(
                Transport.SOCKET, "localhost", server.socketPort(), mine);
        ByteArrayOutputStream screen = new ByteArrayOutputStream();

        Thread partnerFinishes = new Thread(() -> {
            waitUntil(() -> theirs.game().isPresent());
            partner.send(new BuildingCommand.FinishBuilding(null));
            // And crews, or the fleet never launches and half of these tests never get to
            // whatever they are about.
            waitUntil(() -> theirs.game()
                    .map(seen -> seen.phase() == GamePhase.CREW_PLACEMENT)
                    .orElse(false));
            partner.send(new it.polimi.ingsw.common.protocol.PreparationCommand
                    .FinishPreparation());
        }, "the-other-player");
        partnerFinishes.setDaemon(true);
        partnerFinishes.start();

        String script = "name samuele\njoin game-1\n" + String.join("\n", typed) + "\nquit\n";
        new TextInterface(mine, link, new BufferedReader(new StringReader(script)),
                new PrintStream(screen, true, StandardCharsets.UTF_8)).run();
        link.close();
        return screen.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("two ships with nothing wrong with them go straight from the shipyard to crewing")
    void reachingCrewPlacement() {
        String screen = playing("done", "look");

        assertTrue(screen.contains("crew placement"),
                "a ship that is only a starting cabin is a legal ship");
    }

    @Test
    @DisplayName("people go into a cabin one command at a time")
    void boardingPeople() {
        // 7,7 is the starting cabin on a level II board.
        String screen = playing("done", "crew 7 7", "look");

        assertFalse(screen.contains("✗"), "boarding the starting cabin is legal: " + screen);
        assertTrue(screen.contains("@2"), "and the board should show two people in it");
    }

    @Test
    @DisplayName("an alien needs life support welded to the cabin, and the refusal says so")
    void aliensNeedLifeSupport() {
        String screen = playing("done", "alien 7 7 p");

        assertTrue(screen.contains("✗"),
                "a ship with no life support has no berth for an alien");
    }

    @Test
    @DisplayName("an alien nobody can name is asked for again rather than guessed at")
    void unknownAlien() {
        String screen = playing("done", "alien 7 7 mauve");

        assertTrue(screen.contains("which alien?"));
    }

    @Test
    @DisplayName("declaring ready fills the rest with people and launches")
    void launching() {
        // 'route' is repeated because typing it once, right after declaring ready, is a race
        // with the other player doing the same. A slower machine loses that race.
        String screen = playing("done", "done", "route", "route", "route", "route");

        assertTrue(screen.contains("Route"), "the fleet should have launched:\n" + screen);
        assertTrue(screen.contains("cards left"));
    }

    @Test
    @DisplayName("a word that means nothing while crewing is answered with what does")
    void nonsenseWhileCrewing() {
        String screen = playing("done", "weld");

        assertTrue(screen.contains("the ships are being crewed"));
        assertTrue(screen.contains("'crew <row> <col>'"),
                "being told it is wrong is half of it; being told what works is the rest");
    }

    @Test
    @DisplayName("a square that is not on the board is refused before it reaches the server")
    void offTheBoard() {
        String screen = playing("done", "crew 1 1");

        assertTrue(screen.contains("? which square?"));
        assertTrue(screen.contains("rows 5-9"));
    }

    @Test
    @DisplayName("choosing a piece of a whole ship says there is nothing to choose")
    void nothingToChoose() {
        // Reachable only during validation, and a legal ship skips straight past it — so this
        // is asked while crewing, where 'keep' is not a command at all.
        String screen = playing("done", "keep 0");

        assertTrue(screen.contains("the ships are being crewed"));
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
