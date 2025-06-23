package it.polimi.ingsw.server.monitor;

/**
 * Configuration for connection monitoring and ping-pong system.
 */
public class ConnectionConfig {
    public static final long DEFAULT_PING_INTERVAL_MS = 10000; // 10 seconds
    public static final long DEFAULT_PING_TIMEOUT_MS = 5000;   // 5 seconds
    public static final long DEFAULT_CLIENT_TIMEOUT_MS = 15000; // 15 seconds
    
    private final long pingIntervalMs;
    private final long pingTimeoutMs;
    private final long clientTimeoutMs;
    
    /**
     * Creates a configuration with default values.
     */
    public ConnectionConfig() {
        this(DEFAULT_PING_INTERVAL_MS, DEFAULT_PING_TIMEOUT_MS, DEFAULT_CLIENT_TIMEOUT_MS);
    }
    
    /**
     * Creates a configuration with custom values.
     * @param pingIntervalMs How often to send pings (milliseconds)
     * @param pingTimeoutMs How long to wait for pong response (milliseconds)
     * @param clientTimeoutMs How long before considering client disconnected (milliseconds)
     */
    public ConnectionConfig(long pingIntervalMs, long pingTimeoutMs, long clientTimeoutMs) {
        if (pingIntervalMs <= 0 || pingTimeoutMs <= 0 || clientTimeoutMs <= 0) {
            throw new IllegalArgumentException("All timeout values must be positive");
        }
        if (pingTimeoutMs >= pingIntervalMs) {
            throw new IllegalArgumentException("Ping timeout must be less than ping interval");
        }
        if (clientTimeoutMs <= pingTimeoutMs) {
            throw new IllegalArgumentException("Client timeout must be greater than ping timeout");
        }
        
        this.pingIntervalMs = pingIntervalMs;
        this.pingTimeoutMs = pingTimeoutMs;
        this.clientTimeoutMs = clientTimeoutMs;
    }
    
    public long getPingIntervalMs() {
        return pingIntervalMs;
    }
    
    public long getPingTimeoutMs() {
        return pingTimeoutMs;
    }
    
    public long getClientTimeoutMs() {
        return clientTimeoutMs;
    }
    
    @Override
    public String toString() {
        return String.format("ConnectionConfig{pingInterval=%dms, pingTimeout=%dms, clientTimeout=%dms}",
                pingIntervalMs, pingTimeoutMs, clientTimeoutMs);
    }
}