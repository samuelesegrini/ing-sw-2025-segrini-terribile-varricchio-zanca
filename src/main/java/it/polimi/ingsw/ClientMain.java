package it.polimi.ingsw;

import it.polimi.ingsw.client.Startup;
import it.polimi.ingsw.client.network.ServerLink;
import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.client.view.UserInterface;
import it.polimi.ingsw.client.view.gui.GraphicalInterface;
import it.polimi.ingsw.client.view.tui.TextInterface;
import it.polimi.ingsw.common.logging.Tracing;
import it.polimi.ingsw.common.transport.TransportException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of the client process.
 *
 * <p>Several clients may run on the same machine. The interface and the transport are both
 * chosen here and nowhere else — requirements C3 and C4 — and after this method nothing in the
 * client can find out what was picked.
 */
public final class ClientMain {

    private static final Logger LOG = LoggerFactory.getLogger(ClientMain.class);

    private ClientMain() {
        // Entry point holder, never instantiated.
    }

    /**
     * Starts a client.
     *
     * @param args the interface, the transport, and where the server is; see {@link Startup}
     */
    public static void main(String[] args) {
        // First, and to a file. Everything below this line draws on the terminal — the ship,
        // the board, the prompts — and a log line landing in the middle of a frame tears the
        // picture apart. stderr is no escape, being the same terminal.
        Tracing.toFile(Tracing.logFile(args, System::getenv),
                Tracing.wanted(args, System::getenv));

        Startup startup;
        try {
            startup = Startup.from(Tracing.without(args));
        } catch (IllegalArgumentException wrong) {
            // Stays on stderr. It happens before there is an interface to report it, and a
            // player who mistyped an option should not have to open a log file to find out.
            System.err.println(wrong.getMessage());
            LOG.error("could not start: {}", wrong.getMessage());
            System.exit(2);
            return;
        }

        ClientState state = new ClientState();
        ServerLink server;
        try {
            server = ServerLink.connect(startup.transport(), startup.host(), startup.port(), state);
        } catch (TransportException unreachable) {
            // Stays on stderr, for the same reason.
            System.err.println("Could not reach a server at " + startup.host() + ":"
                    + startup.port() + " over " + startup.transport().name().toLowerCase());
            LOG.error("could not reach {}:{} over {}", startup.host(), startup.port(),
                    startup.transport(), unreachable);
            System.exit(1);
            return;
        }

        LOG.info("connected to {}:{} over {}, {} interface", startup.host(), startup.port(),
                startup.transport(), startup.graphical() ? "graphical" : "text");
        UserInterface view = interfaceFor(startup, state, server);
        try (server) {
            view.run();
        }
    }

    private static UserInterface interfaceFor(Startup startup, ClientState state, ServerLink server) {
        if (startup.graphical()) {
            return new GraphicalInterface(state, server);
        }
        return new TextInterface(state, server,
                new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
                System.out);
    }
}
