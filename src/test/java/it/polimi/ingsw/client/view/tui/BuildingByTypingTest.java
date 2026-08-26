package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.client.network.ServerLink;
import it.polimi.ingsw.client.network.Transport;
import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.protocol.LobbyCommand;
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
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Builds a ship by typing at a real server.
 *
 * <p>The unit tests say the renderers draw the right thing and the coordinates subtract the
 * right way. This says that what a person types turns into a tile welded where they meant it —
 * which is the only claim that matters and the one neither of the others makes.
 *
 * <p>Two clients, because the shipyard is the one phase where everybody acts at once and half
 * of what is on the screen is about the other players.
 */
class BuildingByTypingTest {

    private static final Duration PATIENCE = Duration.ofSeconds(5);

    private Server server;
    private ServerLink other;

    @AfterEach
    void shutDown() {
        if (other != null) {
            other.close();
        }
        if (server != null) {
            server.close();
        }
    }

    /**
     * Opens a two-player game, seats a silent partner, and types a script as the other player.
     *
     * @param typed what a person would type, in order
     * @return everything the client printed
     */
    private String building(String... typed) {
        server = Server.start(0, 0);

        ClientState partnerState = new ClientState();
        other = ServerLink.connect(Transport.SOCKET, "localhost", server.socketPort(), partnerState);
        other.send(new LobbyCommand.Login("chiara"));
        waitFor(partnerState::nickname);
        other.send(new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 2));
        waitFor(partnerState::gameId);

        ClientState mine = new ClientState();
        ServerLink link = ServerLink.connect(
                Transport.SOCKET, "localhost", server.socketPort(), mine);
        ByteArrayOutputStream screen = new ByteArrayOutputStream();
        String script = "name samuele\njoin game-1\n" + String.join("\n", typed) + "\nquit\n";
        new TextInterface(mine, link, new BufferedReader(new StringReader(script)),
                new PrintStream(screen, true, StandardCharsets.UTF_8)).run();
        link.close();
        return screen.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("the shipyard is there to look at, with everything that is public in it")
    void lookingAtTheYard() {
        String screen = building("yard");

        assertTrue(screen.contains("Shipyard"));
        assertTrue(screen.contains("face down"));
        assertTrue(screen.contains("free start spaces"));
    }

    @Test
    @DisplayName("drawing shows what you got, without having to ask again")
    void drawing() {
        String screen = building("draw");

        assertTrue(screen.contains("in hand"),
                "a player who draws a tile wants to see it, not be told to look");
    }

    @Test
    @DisplayName("a tile put down beside the cabin is welded where the player said")
    void buildingASquare() {
        // 7,7 is the starting cabin on a level II board, so 7,8 is the square to its right.
        String screen = building("draw", "put 7 8", "weld", "look");

        assertTrue(screen.contains("@2") || screen.contains("@."),
                "the starting cabin should still be there");
        assertFalse(screen.contains("? which square?"), "7,8 is on the board");
        assertTrue(afterWelding(screen), "something should be welded next to the cabin now");
    }

    @Test
    @DisplayName("a square that is not on the board is refused before it reaches the server")
    void offTheBoard() {
        String screen = building("draw", "put 1 1");

        assertTrue(screen.contains("? which square?"));
        assertTrue(screen.contains("rows 5-9, columns 4-10"),
                "being told it is wrong is half of it; being told what would work is the rest");
    }

    @Test
    @DisplayName("welding nothing is refused by the server, and the player is told why")
    void weldingNothing() {
        String screen = building("weld");

        assertTrue(screen.contains("there is nothing waiting to be welded"));
    }

    @Test
    @DisplayName("a tile can be set aside and taken back")
    void reserving() {
        String screen = building("draw", "keep", "yard");

        assertFalse(screen.contains("✗"), "reserving is allowed on level II: " + screen);
    }

    @Test
    @DisplayName("a tile nobody is holding cannot be put down")
    void placingNothing() {
        String screen = building("put 7 8");

        assertTrue(screen.contains("✗"), "there is no tile in hand to place");
    }

    @Test
    @DisplayName("help in the shipyard is about the shipyard")
    void helpIsAboutTheShipyard() {
        String screen = building("help");

        assertTrue(screen.contains("draw"));
        assertTrue(screen.contains("weld"));
        assertFalse(screen.contains("give up"), "there is no route to leave yet");
    }

    @Test
    @DisplayName("a word that means nothing here is answered, not swallowed")
    void nonsense() {
        String screen = building("polish");

        assertTrue(screen.contains("'polish' is not something you can do in the shipyard"));
    }

    @Test
    @DisplayName("finishing takes a place on the starting line")
    void finishing() {
        String screen = building("done");

        assertFalse(screen.contains("✗"), "an empty ship is a legal ship: " + screen);
    }

    /** Tells whether the ship drawn last has more than the starting cabin on it. */
    private static boolean afterWelding(String screen) {
        int lastBoard = screen.lastIndexOf("     4   5   6   7   8   9   10");
        if (lastBoard < 0) {
            return false;
        }
        String board = screen.substring(lastBoard);
        long occupied = board.lines()
                .filter(line -> line.startsWith("  7 "))
                .flatMap(line -> java.util.stream.Stream.of(line.split("\\s+")))
                .filter(cell -> !cell.isBlank() && !cell.equals(".") && !cell.equals("7"))
                .count();
        return occupied >= 2;
    }

    private static void waitFor(Supplier<Optional<?>> ready) {
        long deadline = System.nanoTime() + PATIENCE.toNanos();
        while (System.nanoTime() < deadline && ready.get().isEmpty()) {
            try {
                // A sleep rather than a spin: a build machine has two cores and other tests
                // are using them.
                Thread.sleep(1);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
