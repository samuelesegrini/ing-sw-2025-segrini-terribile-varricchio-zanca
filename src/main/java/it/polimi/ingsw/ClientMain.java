package it.polimi.ingsw;

import it.polimi.ingsw.client.Startup;
import it.polimi.ingsw.client.network.ServerLink;
import it.polimi.ingsw.client.state.ClientState;
import it.polimi.ingsw.client.view.UserInterface;
import it.polimi.ingsw.client.view.tui.TextInterface;
import it.polimi.ingsw.common.transport.TransportException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Entry point of the client process.
 *
 * <p>Several clients may run on the same machine. The interface and the transport are both
 * chosen here and nowhere else — requirements C3 and C4 — and after this method nothing in the
 * client can find out what was picked.
 */
public final class ClientMain {

    private ClientMain() {
        // Entry point holder, never instantiated.
    }

    /**
     * Starts a client.
     *
     * @param args the interface, the transport, and where the server is; see {@link Startup}
     */
    public static void main(String[] args) {
        Startup startup;
        try {
            startup = Startup.from(args);
        } catch (IllegalArgumentException wrong) {
            System.err.println(wrong.getMessage());
            System.exit(2);
            return;
        }

        ClientState state = new ClientState();
        ServerLink server;
        try {
            server = ServerLink.connect(startup.transport(), startup.host(), startup.port(), state);
        } catch (TransportException unreachable) {
            System.err.println("Could not reach a server at " + startup.host() + ":"
                    + startup.port() + " over " + startup.transport().name().toLowerCase());
            System.exit(1);
            return;
        }

        UserInterface view = interfaceFor(startup, state, server);
        try (server) {
            view.run();
        }
    }

    private static UserInterface interfaceFor(Startup startup, ClientState state, ServerLink server) {
        if (startup.graphical()) {
            // The windows arrive with M6. Saying so is better than starting a text client
            // somebody did not ask for and leaving them to work out why.
            System.err.println("The graphical interface is not built yet. Try --tui.");
            System.exit(3);
        }
        return new TextInterface(state, server,
                new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
                System.out);
    }
}
