package it.polimi.ingsw;

import it.polimi.ingsw.common.transport.DefaultPorts;
import it.polimi.ingsw.common.transport.TransportException;
import it.polimi.ingsw.server.network.Server;

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

    /**
     * Starts the server.
     *
     * @param args the socket port and then the RMI port; either may be omitted, and zero asks
     *             the operating system for a free one
     */
    public static void main(String[] args) {
        int socketPort = portFrom(args, 0, DefaultPorts.SOCKET);
        int rmiPort = portFrom(args, 1, DefaultPorts.RMI);

        Server server;
        try {
            server = Server.start(socketPort, rmiPort);
        } catch (TransportException unavailable) {
            System.err.println("Could not start: " + unavailable.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("Galaxy Trucker server");
        System.out.println("  socket  localhost:" + server.socketPort());
        System.out.println("  rmi     localhost:" + server.rmiPort());
        System.out.println("Ctrl-C to stop.");

        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "shutdown"));
        // Everything that matters runs on daemon threads, so something has to keep the process
        // alive. Waiting on a lock nobody holds is the cheapest way to do nothing for ever.
        try {
            new java.util.concurrent.CountDownLatch(1).await();
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
        }
    }

    private static int portFrom(String[] args, int index, int fallback) {
        if (args.length <= index) {
            return fallback;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException notANumber) {
            System.err.println("Not a port: " + args[index] + ", using " + fallback);
            return fallback;
        }
    }
}
