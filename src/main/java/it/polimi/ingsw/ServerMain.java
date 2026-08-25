package it.polimi.ingsw;

/**
 * Entry point of the server process.
 *
 * <p>A server instance hosts games and owns the only authoritative copy of their
 * state. Clients never receive model objects, only projections of them.
 */
public final class ServerMain {

    private ServerMain() {
        // Entry point holder, never instantiated.
    }

    /**
     * Starts the server.
     *
     * @param args command line arguments; the socket and RMI ports are read from
     *             here once the transports exist
     */
    public static void main(String[] args) {
        System.out.println("Galaxy Trucker server — not yet implemented (milestone M4).");
    }
}
