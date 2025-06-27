package it.polimi.ingsw.server.monitor;

import it.polimi.ingsw.common.message.PingMessage;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.network.ServerNetworkManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class ConnectionMonitorServiceTest {

    @Mock
    private ServerNetworkManager networkManager;

    @Mock
    private PlayerSessionRegistry playerRegistry;

    @Mock
    private GameSessionManager sessionManager;

    @Mock
    private Consumer<String> timeoutCallback;

    private Map<String, String> networkClientToGamePlayerMap;
    private ConnectionMonitorService monitorService;
    private ConnectionConfig config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        networkClientToGamePlayerMap = new ConcurrentHashMap<>();
        config = new ConnectionConfig(1000, 500, 2000); // Short intervals for testing
    }

    @AfterEach
    public void tearDown() {
        if (monitorService != null) {
            monitorService.stopMonitoring();
        }
    }

    @Test
    public void testConstructorWithDefaultConfig() {
        monitorService = new ConnectionMonitorService(
                networkManager, playerRegistry, sessionManager,
                networkClientToGamePlayerMap, timeoutCallback);

        assertNotNull(monitorService);
    }

    @Test
    public void testConstructorWithCustomConfig() {
        monitorService = new ConnectionMonitorService(
                networkManager, playerRegistry, sessionManager,
                networkClientToGamePlayerMap, timeoutCallback, config);

        assertNotNull(monitorService);
    }

    @Test
    public void testStartMonitoring() {
        monitorService = new ConnectionMonitorService(
                networkManager, playerRegistry, sessionManager,
                networkClientToGamePlayerMap, timeoutCallback, config);

        monitorService.startMonitoring();

        // Verify monitoring is started (no exception thrown)
        assertNotNull(monitorService);
    }

    @Test
    public void testStopMonitoringWithoutStarting() {
        monitorService = new ConnectionMonitorService(
                networkManager, playerRegistry, sessionManager,
                networkClientToGamePlayerMap, timeoutCallback, config);

        // Should not throw exception even if monitoring was never started
        monitorService.stopMonitoring();

        assertNotNull(monitorService);
    }

    @Test
    public void testStopMonitoringAfterStarting() {
        monitorService = new ConnectionMonitorService(
                networkManager, playerRegistry, sessionManager,
                networkClientToGamePlayerMap, timeoutCallback, config);

        monitorService.startMonitoring();
        monitorService.stopMonitoring();

        assertNotNull(monitorService);
    }

    @Test
    public void testCheckConnectionsWithNoClients() throws InterruptedException {
        when(networkManager.getAllConnectedClientIds()).thenReturn(Collections.emptySet());

        monitorService = new ConnectionMonitorService(
                networkManager, playerRegistry, sessionManager,
                networkClientToGamePlayerMap, timeoutCallback, config);

        monitorService.startMonitoring();

        // Wait a bit for the monitoring task to run
        Thread.sleep(1200);

        verify(networkManager, atLeastOnce()).getAllConnectedClientIds();
    }

    @Test
    public void testCheckConnectionsWithClients() throws InterruptedException {
        Set<String> clientIds = new HashSet<>();
        clientIds.add("client1");
        clientIds.add("client2");

        when(networkManager.getAllConnectedClientIds()).thenReturn(clientIds);
        when(networkManager.sendMessageToClient(anyString(), any(PingMessage.class))).thenReturn(true);

        monitorService = new ConnectionMonitorService(
                networkManager, playerRegistry, sessionManager,
                networkClientToGamePlayerMap, timeoutCallback, config);

        monitorService.startMonitoring();

        // Wait for monitoring task to run
        Thread.sleep(1200);

        verify(networkManager, atLeastOnce()).getAllConnectedClientIds();
        verify(networkManager, atLeastOnce()).sendMessageToClient(eq("client1"), any(PingMessage.class));
        verify(networkManager, atLeastOnce()).sendMessageToClient(eq("client2"), any(PingMessage.class));
    }

    @Test
    public void testSendPingSuccess() throws InterruptedException {
        Set<String> clientIds = Set.of("client1");

        when(networkManager.getAllConnectedClientIds()).thenReturn(clientIds);
        when(networkManager.sendMessageToClient(eq("client1"), any(PingMessage.class))).thenReturn(true);

        monitorService = new ConnectionMonitorService(
                networkManager, playerRegistry, sessionManager,
                networkClientToGamePlayerMap, timeoutCallback, config);

        monitorService.startMonitoring();

        Thread.sleep(1200);

        verify(networkManager, atLeastOnce()).sendMessageToClient(eq("client1"), any(PingMessage.class));
    }

    @Test
    public void testSendPingFailure() throws InterruptedException {
        Set<String> clientIds = Set.of("client1");

        when(networkManager.getAllConnectedClientIds()).thenReturn(clientIds);
        when(networkManager.sendMessageToClient(eq("client1"), any(PingMessage.class))).thenReturn(false);

        monitorService = new ConnectionMonitorService(
                networkManager, playerRegistry, sessionManager,
                networkClientToGamePlayerMap, timeoutCallback, config);

        monitorService.startMonitoring();

        Thread.sleep(1200);

        verify(networkManager, atLeastOnce()).sendMessageToClient(eq("client1"), any(PingMessage.class));
    }

    @Test
    public void testHandlePongWithExistingClient() throws InterruptedException {
        Set<String> clientIds = Set.of("client1");

        when(networkManager.getAllConnectedClientIds()).thenReturn(clientIds);
        when(networkManager.sendMessageToClient(eq("client1"), any(PingMessage.class))).thenReturn(true);

        monitorService = new ConnectionMonitorService(
                networkManager, playerRegistry, sessionManager,
                networkClientToGamePlayerMap, timeoutCallback, config);

        monitorService.startMonitoring();

        // Wait for ping to be sent
        Thread.sleep(1200);

        // Handle pong response
        monitorService.handlePong("client1");

        // Verify no timeout callback was called
        verify(timeoutCallback, never()).accept("client1");
    }

    @Test
    public void testHandlePongWithNonExistentClient() {
        monitorService = new ConnectionMonitorService(
                networkManager, playerRegistry, sessionManager,
                networkClientToGamePlayerMap, timeoutCallback, config);

        // Should not throw exception
        monitorService.handlePong("nonexistent-client");

        assertNotNull(monitorService);
    }

    @Test
    public void testClientTimeoutWhenWaitingForPong() throws InterruptedException {
        Set<String> clientIds = Set.of("client1");

        when(networkManager.getAllConnectedClientIds()).thenReturn(clientIds);
        when(networkManager.sendMessageToClient(eq("client1"), any(PingMessage.class))).thenReturn(true);

        // Use very short timeout for testing
        ConnectionConfig shortTimeoutConfig = new ConnectionConfig(1000, 100, 2000);

        monitorService = new ConnectionMonitorService(
                networkManager, playerRegistry, sessionManager,
                networkClientToGamePlayerMap, timeoutCallback, shortTimeoutConfig);

        monitorService.startMonitoring();

        // Wait for ping to be sent and timeout to occur
        Thread.sleep(1200);

        // Should trigger timeout callback
        verify(timeoutCallback, atLeastOnce()).accept("client1");
    }

    @Test
    public void testStopMonitoringInterruption() throws InterruptedException {
        monitorService = new ConnectionMonitorService(
                networkManager, playerRegistry, sessionManager,
                networkClientToGamePlayerMap, timeoutCallback, config);

        monitorService.startMonitoring();

        // Interrupt current thread to test the interrupt handling
        Thread currentThread = Thread.currentThread();
        Thread interruptThread = new Thread(() -> {
            try {
                Thread.sleep(100);
                currentThread.interrupt();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        interruptThread.start();

        // This should handle the interruption gracefully
        monitorService.stopMonitoring();

        interruptThread.join();

        // Clear interrupt flag
        Thread.interrupted();

        assertNotNull(monitorService);
    }

    @Test
    public void testClientConnectionCheckWhenNotWaitingForPong() throws InterruptedException {
        Set<String> clientIds = Set.of("client1");

        when(networkManager.getAllConnectedClientIds())
                .thenReturn(clientIds)
                .thenReturn(clientIds)
                .thenReturn(Collections.emptySet()); // Stop after second check

        when(networkManager.sendMessageToClient(eq("client1"), any(PingMessage.class))).thenReturn(true);

        monitorService = new ConnectionMonitorService(
                networkManager, playerRegistry, sessionManager,
                networkClientToGamePlayerMap, timeoutCallback, config);

        monitorService.startMonitoring();

        // Wait for first ping
        Thread.sleep(1200);

        // Handle pong to reset waiting state
        monitorService.handlePong("client1");

        // Wait for second ping
        Thread.sleep(1100);

        // Should have sent ping twice
        verify(networkManager, atLeast(2)).sendMessageToClient(eq("client1"), any(PingMessage.class));
    }
}