package it.polimi.ingsw;

/**
 * Entry point of the client process.
 *
 * <p>Several clients may run on the same machine. The user interface (textual or
 * graphical) and the transport (socket or RMI) are both chosen at startup, as
 * required by sections 2.2.2 of the project requirements.
 */
public final class ClientMain {

    private ClientMain() {
        // Entry point holder, never instantiated.
    }

    /**
     * Starts the client.
     *
     * @param args command line arguments; the server address, the interface type and
     *             the transport are read from here once they exist
     */
    public static void main(String[] args) {
        System.out.println("Galaxy Trucker client — not yet implemented (milestone M4).");
    }
}
