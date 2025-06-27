package it.polimi.ingsw.server.monitor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class ConnectionConfigTest {

    @Test
    public void testDefaultConstructor() {
        ConnectionConfig config = new ConnectionConfig();

        assertEquals(ConnectionConfig.DEFAULT_PING_INTERVAL_MS, config.getPingIntervalMs());
        assertEquals(ConnectionConfig.DEFAULT_PING_TIMEOUT_MS, config.getPingTimeoutMs());
        assertEquals(ConnectionConfig.DEFAULT_CLIENT_TIMEOUT_MS, config.getClientTimeoutMs());
    }

    @Test
    public void testParameterizedConstructorValid() {
        long pingInterval = 20000;
        long pingTimeout = 8000;
        long clientTimeout = 25000;

        ConnectionConfig config = new ConnectionConfig(pingInterval, pingTimeout, clientTimeout);

        assertEquals(pingInterval, config.getPingIntervalMs());
        assertEquals(pingTimeout, config.getPingTimeoutMs());
        assertEquals(clientTimeout, config.getClientTimeoutMs());
    }

    @Test
    public void testConstructorThrowsExceptionForNegativePingInterval() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig(-1, 5000, 15000);
        });
    }

    @Test
    public void testConstructorThrowsExceptionForZeroPingInterval() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig(0, 5000, 15000);
        });
    }

    @Test
    public void testConstructorThrowsExceptionForNegativePingTimeout() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig(10000, -1, 15000);
        });
    }

    @Test
    public void testConstructorThrowsExceptionForZeroPingTimeout() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig(10000, 0, 15000);
        });
    }

    @Test
    public void testConstructorThrowsExceptionForNegativeClientTimeout() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig(10000, 5000, -1);
        });
    }

    @Test
    public void testConstructorThrowsExceptionForZeroClientTimeout() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig(10000, 5000, 0);
        });
    }

    @Test
    public void testConstructorThrowsExceptionWhenPingTimeoutEqualsInterval() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig(10000, 10000, 15000);
        });
    }

    @Test
    public void testConstructorThrowsExceptionWhenPingTimeoutGreaterThanInterval() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig(10000, 12000, 15000);
        });
    }

    @Test
    public void testConstructorThrowsExceptionWhenClientTimeoutEqualsToPerformance() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig(10000, 5000, 5000);
        });
    }

    @Test
    public void testConstructorThrowsExceptionWhenClientTimeoutLessThanPingTimeout() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ConnectionConfig(10000, 8000, 6000);
        });
    }

    @Test
    public void testGettersReturnCorrectValues() {
        long pingInterval = 12000;
        long pingTimeout = 6000;
        long clientTimeout = 18000;

        ConnectionConfig config = new ConnectionConfig(pingInterval, pingTimeout, clientTimeout);

        assertEquals(pingInterval, config.getPingIntervalMs());
        assertEquals(pingTimeout, config.getPingTimeoutMs());
        assertEquals(clientTimeout, config.getClientTimeoutMs());
    }

    @Test
    public void testToStringWithDefaultValues() {
        ConnectionConfig config = new ConnectionConfig();
        String result = config.toString();

        String expected = String.format("ConnectionConfig{pingInterval=%dms, pingTimeout=%dms, clientTimeout=%dms}",
                ConnectionConfig.DEFAULT_PING_INTERVAL_MS,
                ConnectionConfig.DEFAULT_PING_TIMEOUT_MS,
                ConnectionConfig.DEFAULT_CLIENT_TIMEOUT_MS);

        assertEquals(expected, result);
    }

    @Test
    public void testToStringWithCustomValues() {
        long pingInterval = 25000;
        long pingTimeout = 10000;
        long clientTimeout = 30000;

        ConnectionConfig config = new ConnectionConfig(pingInterval, pingTimeout, clientTimeout);
        String result = config.toString();

        String expected = String.format("ConnectionConfig{pingInterval=%dms, pingTimeout=%dms, clientTimeout=%dms}",
                pingInterval, pingTimeout, clientTimeout);

        assertEquals(expected, result);
    }

    @Test
    public void testConstantsValues() {
        assertEquals(10000L, ConnectionConfig.DEFAULT_PING_INTERVAL_MS);
        assertEquals(5000L, ConnectionConfig.DEFAULT_PING_TIMEOUT_MS);
        assertEquals(15000L, ConnectionConfig.DEFAULT_CLIENT_TIMEOUT_MS);
    }
}