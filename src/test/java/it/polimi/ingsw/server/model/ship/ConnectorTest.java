package it.polimi.ingsw.server.model.ship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the welding rules of manual p.5 on {@link Connector}.
 *
 * <p>These four lines decide whether a ship is legal, so each clause of the rule gets
 * its own test rather than being folded into a truth table nobody reads.
 */
class ConnectorTest {

    @Test
    @DisplayName("a single connector never joins a double one, which is the most common building mistake")
    void singleDoesNotJoinDouble() {
        assertFalse(Connector.SINGLE.joinsTo(Connector.DOUBLE));
        assertFalse(Connector.DOUBLE.joinsTo(Connector.SINGLE));
    }

    @Test
    @DisplayName("connectors of the same kind join")
    void sameKindJoins() {
        assertTrue(Connector.SINGLE.joinsTo(Connector.SINGLE));
        assertTrue(Connector.DOUBLE.joinsTo(Connector.DOUBLE));
    }

    @DisplayName("a universal connector joins every real connector")
    @ParameterizedTest(name = "universal joins {0}")
    @EnumSource(value = Connector.class, names = {"SINGLE", "DOUBLE", "UNIVERSAL"})
    void universalJoinsEveryConnector(Connector other) {
        assertTrue(Connector.UNIVERSAL.joinsTo(other));
        assertTrue(other.joinsTo(Connector.UNIVERSAL));
    }

    @DisplayName("a smooth side joins nothing at all, not even another smooth side")
    @ParameterizedTest(name = "plain does not join {0}")
    @EnumSource(Connector.class)
    void plainJoinsNothing(Connector other) {
        assertFalse(Connector.PLAIN.joinsTo(other));
        assertFalse(other.joinsTo(Connector.PLAIN));
    }

    @Test
    @DisplayName("only smooth sides are not connectors, which is what keeps them out of the exposed count")
    void onlyPlainIsNotAConnector() {
        assertFalse(Connector.PLAIN.isConnector());
        assertTrue(Connector.SINGLE.isConnector());
        assertTrue(Connector.DOUBLE.isConnector());
        assertTrue(Connector.UNIVERSAL.isConnector());
    }
}
