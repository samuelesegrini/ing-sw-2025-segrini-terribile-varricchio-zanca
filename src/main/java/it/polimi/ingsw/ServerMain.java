package it.polimi.ingsw;

import it.polimi.ingsw.common.logging.Tracing;
import it.polimi.ingsw.common.transport.DefaultPorts;
import it.polimi.ingsw.common.transport.TransportException;
import it.polimi.ingsw.server.lobby.ServerSettings;
import it.polimi.ingsw.server.network.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of the server process.
 *
 * <p>A server instance hosts games and owns the only authoritative copy of their state.
 * Clients never receive model objects, only projections of them.
 *
 * <p>It listens two ways at once — a socket and an RMI registry — and nothing below the
 * transports is told which door a player came through. That is requirement S5, and there is
 * nothing to it here because there was nothing left to branch on by the time this was written.
 */
public final class ServerMain {

    private ServerMain() {
        // Entry point holder, never instantiated.
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServerMain.class);

    /** Where games are written down, relative to wherever the server was started. */
    private static final java.nio.file.Path GAMES = java.nio.file.Path.of("games");

    /**
     * Starts the server.
     *
     * @param args the socket port and then the RMI port; either may be omitted, and zero asks
     *             the operating system for a free one
     */
    public static void main(String[] args) {
        // Before anything else has a chance to log. The trace is off unless asked for, and the
        // flag is taken out of the arguments so the ports below parse exactly as they always
        // did, whichever order the two were given in.
        Tracing.toConsole(Tracing.wanted(args, System::getenv));
        String[] ports = Tracing.without(args);

        int socketPort = portFrom(ports, 0, DefaultPorts.SOCKET);
        int rmiPort = portFrom(ports, 1, DefaultPorts.RMI);

        Server server;
        try {
            // The one line that decides this server keeps its games. Requirement AF3 says a
            // server writes game state to disk and resumes after a crash, so it is not
            // something to be asked for: the library defaults to keeping nothing because a
            // test should not write files it did not ask for, and the product says otherwise
            // here, where it can be read.
            server = Server.start(socketPort, rmiPort, ServerSettings.defaults().keeping(GAMES));
        } catch (TransportException unavailable) {
            LOG.error("Could not start: {}", unavailable.getMessage());
            System.exit(1);
            return;
        }

        LOG.info("Galaxy Trucker server");
        LOG.info("  socket  localhost:{}", server.socketPort());
        LOG.info("  rmi     localhost:{}", server.rmiPort());
        // Said out loud, because a feature nobody can see is one nobody can check. Stopping
        // this server and starting it again brings these games back.
        LOG.info("  games   {}", GAMES.toAbsolutePath());
        LOG.info("Ctrl-C to stop.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("stopping");
            server.close();
        }, "shutdown"));
        // Everything that matters runs on daemon threads, so something has to keep the process
        // alive. Waiting on a lock nobody holds is the cheapest way to do nothing for ever.
        try {
            new java.util.concurrent.CountDownLatch(1).await();
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Reads a port from the arguments, falling back when it is missing or not a number.
     *
     * <p>The ports are positional — {@code server.jar [socketPort] [rmiPort]} — so an option
     * that wandered in here is not a port and is said so out loud rather than swallowed. Zero
     * is a port: it asks the operating system for a free one.
     *
     * <p>Package-private so a test can read it. {@code main} itself blocks for ever on purpose,
     * which is the whole of what is left once this is taken out of it.
     *
     * @param args     the arguments, with the trace flag already removed
     * @param index    which port this is
     * @param fallback what to use when there is nothing usable at that index
     * @return the port
     */
    static int portFrom(String[] args, int index, int fallback) {
        if (args.length <= index) {
            return fallback;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException notANumber) {
            LOG.warn("Not a port: {}, using {}", args[index], fallback);
            return fallback;
        }
    }
}
