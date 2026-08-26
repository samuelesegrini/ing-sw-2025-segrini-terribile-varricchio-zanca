package it.polimi.ingsw.client;

import it.polimi.ingsw.client.network.Transport;
import it.polimi.ingsw.common.transport.DefaultPorts;

import java.util.List;
import java.util.Locale;

/**
 * The two questions a client is asked before anything else, and where the answers come from.
 *
 * <p>Requirements C3 and C4 want the interface and the transport chosen at startup, and C5
 * wants the server's address to be something a player can give. All three are command line
 * arguments, with defaults, because a person launching four clients on one machine to try
 * something should not have to answer four sets of questions first.
 *
 * <pre>
 *   java -jar client.jar --tui --socket --host localhost --port 4321
 * </pre>
 *
 * @param graphical   whether to draw windows rather than text
 * @param transport   how to reach the server
 * @param host        where it is
 * @param port        which port, matching the transport
 */
public record Startup(boolean graphical, Transport transport, String host, int port) {

    /** Where a server is unless somebody says otherwise. */
    public static final String DEFAULT_HOST = "localhost";

    /**
     * Reads the choices from the command line.
     *
     * <p>A port not given follows the transport, since the two travel together and asking a
     * player to remember which of two numbers goes with which of two words is asking for a
     * mistake.
     *
     * @param args what was typed after the jar
     * @return the choices
     * @throws IllegalArgumentException if an argument is not one of the choices, or a value is
     *                                  missing
     */
    public static Startup from(String... args) {
        boolean graphical = false;
        Transport transport = Transport.SOCKET;
        String host = DEFAULT_HOST;
        int port = 0;

        List<String> given = List.of(args);
        for (int at = 0; at < given.size(); at++) {
            String argument = given.get(at).toLowerCase(Locale.ROOT);
            switch (argument) {
                case "--tui", "-t" -> graphical = false;
                case "--gui", "-g" -> graphical = true;
                case "--socket", "-s" -> transport = Transport.SOCKET;
                case "--rmi", "-r" -> transport = Transport.RMI;
                case "--host", "-h" -> host = valueAfter(given, at++, "--host");
                case "--port", "-p" -> port = numberAfter(given, at++, "--port");
                default -> throw new IllegalArgumentException(
                        "there is no " + given.get(at) + " option; " + usage());
            }
        }
        return new Startup(graphical, transport,
                host, port != 0 ? port : defaultPortFor(transport));
    }

    /**
     * Validates the choices.
     *
     * @throws NullPointerException     if the transport or the host is {@code null}
     * @throws IllegalArgumentException if the port is not one
     */
    public Startup {
        if (transport == null || host == null) {
            throw new NullPointerException("a client needs a transport and a host");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("there is no port " + port);
        }
    }

    /**
     * Returns how to launch a client, for when somebody gets it wrong.
     *
     * @return one line of usage
     */
    public static String usage() {
        return "usage: [--tui|--gui] [--socket|--rmi] [--host <name>] [--port <number>]";
    }

    private static int defaultPortFor(Transport transport) {
        return transport == Transport.SOCKET ? DefaultPorts.SOCKET : DefaultPorts.RMI;
    }

    private static String valueAfter(List<String> given, int at, String option) {
        if (at + 1 >= given.size()) {
            throw new IllegalArgumentException(option + " needs a value; " + usage());
        }
        return given.get(at + 1);
    }

    private static int numberAfter(List<String> given, int at, String option) {
        String value = valueAfter(given, at, option);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException notANumber) {
            throw new IllegalArgumentException(option + " needs a number, not " + value);
        }
    }
}
