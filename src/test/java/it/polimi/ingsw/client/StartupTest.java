package it.polimi.ingsw.client;

import it.polimi.ingsw.client.network.Transport;
import it.polimi.ingsw.common.transport.DefaultPorts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the two choices requirements C3 and C4 want made before anything else happens.
 *
 * <p>Including the one that is easy to get wrong: a port not given has to follow the transport.
 * Asking somebody launching four clients to remember which of two numbers goes with which of
 * two words is asking for a mistake, and the mistake looks like a server that is not there.
 */
class StartupTest {

    @Test
    @DisplayName("a client with nothing said to it is a text client on a socket")
    void defaults() {
        Startup startup = Startup.from();

        assertFalse(startup.graphical());
        assertEquals(Transport.SOCKET, startup.transport());
        assertEquals("localhost", startup.host());
        assertEquals(DefaultPorts.SOCKET, startup.port());
    }

    @Test
    @DisplayName("the port follows the transport when nobody says otherwise")
    void portFollowsTransport() {
        assertEquals(DefaultPorts.RMI, Startup.from("--rmi").port());
        assertEquals(DefaultPorts.SOCKET, Startup.from("--socket").port());
        assertEquals(9999, Startup.from("--rmi", "--port", "9999").port(),
                "and a port that was given is the one that is used");
    }

    @Test
    @DisplayName("both choices can be made, in either order, long or short")
    void choices() {
        Startup startup = Startup.from("--gui", "-r", "-h", "example.org", "-p", "1234");

        assertTrue(startup.graphical());
        assertEquals(Transport.RMI, startup.transport());
        assertEquals("example.org", startup.host());
        assertEquals(1234, startup.port());
    }

    @Test
    @DisplayName("something that is not an option says so, with how to do it right")
    void nonsense() {
        IllegalArgumentException wrong = assertThrows(IllegalArgumentException.class,
                () -> Startup.from("--telepathy"));

        assertTrue(wrong.getMessage().contains("--telepathy"));
        assertTrue(wrong.getMessage().contains("usage:"), "being told it is wrong is half of it");
    }

    @Test
    @DisplayName("an option with nothing after it is a mistake, not an empty value")
    void missingValues() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> Startup.from("--host")).getMessage().contains("needs a value"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> Startup.from("--port", "soon")).getMessage().contains("needs a number"));
    }

    @Test
    @DisplayName("there is no port zero, and none above sixty-five thousand")
    void impossiblePorts() {
        assertThrows(IllegalArgumentException.class,
                () -> new Startup(false, Transport.SOCKET, "localhost", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Startup(false, Transport.SOCKET, "localhost", 70000));
        assertThrows(NullPointerException.class,
                () -> new Startup(false, null, "localhost", 4321));
    }

    @Test
    @DisplayName("the tracing flags are not Startup's business, and never reach it")
    void tracingArgumentsAreStrippedBeforeStartupSeesThem() {
        // Both jars take a --debug that neither Startup nor the server's port reader knows
        // about. It is taken out before either parses, so the option lives in one place rather
        // than in every parser that might see it — and it works in any position.
        Startup before = Startup.from(it.polimi.ingsw.common.logging.Tracing
                .without(new String[] {"--debug", "--rmi", "--host", "elsewhere"}));
        Startup after = Startup.from(it.polimi.ingsw.common.logging.Tracing
                .without(new String[] {"--rmi", "--host", "elsewhere", "--debug"}));

        assertEquals(before, after);
        assertEquals("elsewhere", before.host());
    }

    @Test
    @DisplayName("and an option nobody knows is still refused")
    void anUnknownOptionIsStillRefused() {
        assertThrows(IllegalArgumentException.class, () -> Startup.from(
                it.polimi.ingsw.common.logging.Tracing.without(new String[] {"--verbose"})));
    }
}
