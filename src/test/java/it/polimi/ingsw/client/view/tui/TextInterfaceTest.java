package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.client.network.ServerLink;
import it.polimi.ingsw.client.network.Transport;
import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.server.network.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives a whole session by typing at it.
 *
 * <p>A real server on a real socket, and a client whose terminal is two streams. Nothing is
 * mocked, because the thing worth checking is that what a person types turns into what the
 * server does — and a mock of the server would only prove that the client talks to the mock.
 *
 * <p>What is <em>not</em> tested here is how anything looks. That is
 * {@link ShipRendererTest}'s job, on projections, with no server in sight. Splitting the two is
 * the point of the renderers being pure functions: this file can be short and that one can be
 * exhaustive.
 */
class TextInterfaceTest {

    private Server server;

    @AfterEach
    void shutDown() {
        if (server != null) {
            server.close();
        }
    }

    /**
     * Types a script at a client and returns everything it printed.
     *
     * @param typed the lines a person would type, in order
     * @return the whole screen
     */
    private String session(String... typed) {
        server = Server.start(0, 0);
        ClientState state = new ClientState();
        ServerLink link = ServerLink.connect(
                Transport.SOCKET, "localhost", server.socketPort(), state);

        ByteArrayOutputStream screen = new ByteArrayOutputStream();
        new TextInterface(state, link,
                new BufferedReader(new StringReader(String.join("\n", typed) + "\n")),
                new PrintStream(screen, true, StandardCharsets.UTF_8)).run();
        link.close();
        return screen.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("a client opens with something to type")
    void opening() {
        String screen = session("quit");

        assertTrue(screen.contains("Galaxy Trucker"));
        assertTrue(screen.contains("help"), "somebody who has never seen this needs a way in");
        assertTrue(screen.contains("type 'name <nickname>' to begin"));
    }

    @Test
    @DisplayName("help shows what can be typed here, not everything in the game")
    void helpIsAboutNow() {
        String screen = session("help", "quit");

        assertTrue(screen.contains("In the lobby"));
        assertTrue(screen.contains("name <nickname>"));
        assertFalse(screen.contains("weld"),
                "a reference that listed every command in the game would be one a player had "
                        + "to filter in their head");
    }

    @Test
    @DisplayName("claiming a name, and being told it is yours")
    void loggingIn() {
        String screen = session("name samuele", "quit");

        assertTrue(screen.contains("you are samuele"));
    }

    @Test
    @DisplayName("a name somebody else has is refused, and the reason is printed")
    void nameCollision() {
        server = Server.start(0, 0);
        ClientState taken = new ClientState();
        ServerLink first = ServerLink.connect(
                Transport.SOCKET, "localhost", server.socketPort(), taken);
        first.send(new it.polimi.ingsw.common.protocol.LobbyCommand.Login("samuele"));
        waitUntil(taken::nickname);

        ClientState mine = new ClientState();
        ServerLink second = ServerLink.connect(
                Transport.SOCKET, "localhost", server.socketPort(), mine);
        ByteArrayOutputStream screen = new ByteArrayOutputStream();
        new TextInterface(mine, second,
                new BufferedReader(new StringReader("name samuele\nname chiara\nquit\n")),
                new PrintStream(screen, true, StandardCharsets.UTF_8)).run();
        first.close();

        String printed = screen.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("somebody is already called samuele"),
                "a refusal a player cannot see is a client that has hung");
        assertTrue(printed.contains("you are chiara"), "and they can simply try another");
    }

    @Test
    @DisplayName("opening a table and listing it")
    void openingATable() {
        String screen = session("name samuele", "new 3", "quit");

        assertTrue(screen.contains("you have a seat at game-1"));
        assertTrue(screen.contains("waiting at game-1 for the others"));
    }

    @Test
    @DisplayName("a table needs a number of players, and says so rather than guessing")
    void openingATableBadly() {
        String screen = session("name samuele", "new", "new 9", "quit");

        assertTrue(screen.contains("how many players?"));
        assertFalse(screen.contains("you have a seat"), "neither attempt should have opened one");
    }

    @Test
    @DisplayName("asking what games there are prints them, rather than needing a second command")
    void listingGames() {
        String screen = session("name samuele", "games", "quit");

        assertTrue(screen.contains("No games are waiting for players"),
                "asking and then having to ask again to see the answer is not asking");
    }

    @Test
    @DisplayName("a table somebody opened shows up in the listing, with who is at it")
    void listingAnOpenTable() {
        server = Server.start(0, 0);
        ClientState host = new ClientState();
        ServerLink first = ServerLink.connect(
                Transport.SOCKET, "localhost", server.socketPort(), host);
        first.send(new it.polimi.ingsw.common.protocol.LobbyCommand.Login("samuele"));
        waitUntil(host::nickname);
        first.send(new it.polimi.ingsw.common.protocol.LobbyCommand.CreateGame(
                it.polimi.ingsw.common.game.GameLevel.LEVEL_II, 3));
        waitUntil(host::gameId);

        ClientState mine = new ClientState();
        ServerLink second = ServerLink.connect(
                Transport.SOCKET, "localhost", server.socketPort(), mine);
        ByteArrayOutputStream screen = new ByteArrayOutputStream();
        new TextInterface(mine, second,
                new BufferedReader(new StringReader("name chiara\ngames\nquit\n")),
                new PrintStream(screen, true, StandardCharsets.UTF_8)).run();
        first.close();

        String printed = screen.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("game-1"));
        assertTrue(printed.contains("1/3"), "how full a table is decides whether to join it");
        assertTrue(printed.contains("samuele"));
    }

    @Test
    @DisplayName("quitting is not an error, and does not report itself as one")
    void quittingIsClean() {
        String screen = session("name samuele", "quit");

        assertFalse(screen.contains("✗"),
                "telling somebody their deliberate exit went wrong is a poor last impression");
    }

    @Test
    @DisplayName("a word that means nothing here is answered, not ignored")
    void nonsense() {
        String screen = session("name samuele", "weld", "quit");

        assertTrue(screen.contains("'weld' is not something you can do here"));
    }

    @Test
    @DisplayName("an empty line does nothing at all")
    void blankLines() {
        String screen = session("", "   ", "quit");

        assertFalse(screen.contains("is not something you can do"));
    }

    private static void waitUntil(java.util.function.Supplier<java.util.Optional<?>> ready) {
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline && ready.get().isEmpty()) {
            Thread.onSpinWait();
        }
    }
}
