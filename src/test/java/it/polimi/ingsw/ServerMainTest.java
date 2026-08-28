package it.polimi.ingsw;

import it.polimi.ingsw.common.transport.DefaultPorts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * How the server reads the two ports it is given.
 *
 * <p>The ports are positional — {@code server.jar [socketPort] [rmiPort]} — which is quiet about
 * being got wrong: an option written where a port belongs is not a number, so it falls back, and
 * everything after it slides one place along. Somebody who typed
 * {@code --socket-port 21331 --rmi-port 21332} got the default socket port and 21331 for RMI,
 * which is two surprises rather than one.
 *
 * <p>What is left of {@code main} once this is lifted out is a call to {@code Server.start} and
 * a latch that never counts down, so this is the part worth reading back.
 *
 * <p>Components involved: {@link ServerMain}, {@link DefaultPorts}.
 */
class ServerMainTest {

    @Test
    @DisplayName("no arguments at all means the documented pair")
    void nothingGiven() {
        assertEquals(DefaultPorts.SOCKET, ServerMain.portFrom(new String[0], 0,
                DefaultPorts.SOCKET));
        assertEquals(DefaultPorts.RMI, ServerMain.portFrom(new String[0], 1, DefaultPorts.RMI));
    }

    @Test
    @DisplayName("one port given sets the first and leaves the second alone")
    void onlyTheFirst() {
        String[] args = {"21331"};

        assertEquals(21331, ServerMain.portFrom(args, 0, DefaultPorts.SOCKET));
        assertEquals(DefaultPorts.RMI, ServerMain.portFrom(args, 1, DefaultPorts.RMI));
    }

    @Test
    @DisplayName("both are read in the order they were written")
    void bothGiven() {
        String[] args = {"21331", "21332"};

        assertEquals(21331, ServerMain.portFrom(args, 0, DefaultPorts.SOCKET));
        assertEquals(21332, ServerMain.portFrom(args, 1, DefaultPorts.RMI));
    }

    @Test
    @DisplayName("zero is a port: it asks the operating system for a free one")
    void zeroIsAPort() {
        assertEquals(0, ServerMain.portFrom(new String[]{"0", "0"}, 0, DefaultPorts.SOCKET),
                "zero has to survive, or a test server cannot ask for a free port");
    }

    @Test
    @DisplayName("a negative number is passed on as it is, and refused where ports are opened")
    void negativeIsNotQuietlyMadePositive() {
        // Not clamped, not made positive. -1 is not a port, and the place that knows what a
        // port may be is the transport that tries to open one: it throws, and main prints
        // "Could not start" and stops. Turning -1 into 1 here would start a server on a port
        // nobody asked for.
        assertEquals(-1, ServerMain.portFrom(new String[]{"-1"}, 0, DefaultPorts.SOCKET),
                "a nonsense port should reach the code that can say so, not be repaired here");
    }

    @Test
    @DisplayName("something that is not a number falls back rather than stopping the server")
    void notANumber() {
        String[] args = {"--socket-port", "21331"};

        assertEquals(DefaultPorts.SOCKET, ServerMain.portFrom(args, 0, DefaultPorts.SOCKET),
                "an option written where a port belongs is not a port");
        assertEquals(21331, ServerMain.portFrom(args, 1, DefaultPorts.RMI),
                "and everything after it has slid one place along, which is why the fallback "
                        + "says so on the way past");
    }
}
