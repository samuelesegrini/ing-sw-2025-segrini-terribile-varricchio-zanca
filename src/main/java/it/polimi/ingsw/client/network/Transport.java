package it.polimi.ingsw.client.network;

/**
 * Which way a client talks to the server.
 *
 * <p>Chosen once, at startup (requirement C4), and never asked about again. Nothing past
 * {@link ServerLink} can find out which was picked, which is the same property the server has
 * and for the same reason: a game where the two halves behaved differently would be two games.
 */
public enum Transport {

    /** Object streams over TCP. */
    SOCKET,

    /** The same messages, through a registry. */
    RMI;

    /**
     * Reads a transport from something a person typed.
     *
     * @param spoken what they wrote, in any case
     * @return the transport
     * @throws IllegalArgumentException if it is neither
     */
    public static Transport of(String spoken) {
        for (Transport transport : values()) {
            if (transport.name().equalsIgnoreCase(spoken)) {
                return transport;
            }
        }
        throw new IllegalArgumentException("there is no " + spoken + " transport; try socket or rmi");
    }
}
