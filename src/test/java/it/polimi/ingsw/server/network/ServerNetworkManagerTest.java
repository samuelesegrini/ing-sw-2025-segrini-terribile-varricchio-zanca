package it.polimi.ingsw.server.network;

import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.common.message.PongMessage;
import it.polimi.ingsw.common.message.request.Request;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.server.controller.CommandDispatcher;
import it.polimi.ingsw.server.monitor.ConnectionMonitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServerNetworkManagerTest {

    @Mock
    private CommandDispatcher mockCommandDispatcher;
    @Mock
    private ConnectionMonitorService mockConnectionMonitor;
    @Mock
    private SocketServerAdapter mockSocketAdapter;
    @Mock
    private RMIServerAdapter mockRMIAdapter;
    @Mock
    private ServerNetworkInterface mockUnknownAdapter;
    @Mock
    private Request mockRequest;
    @Mock
    private Message mockMessage;
    @Mock
    private PongMessage mockPongMessage;
    @Mock
    private Consumer<String> mockConnectedHandler;
    @Mock
    private Consumer<String> mockDisconnectedHandler;

    private ServerNetworkManager manager;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        manager = new ServerNetworkManager();
        when(mockRequest.getCorrelationId()).thenReturn("test-correlation-id");
    }

    @Test
    void testSetCommandDispatcher() {
        manager.setCommandDispatcher(mockCommandDispatcher);
        assertThrows(NullPointerException.class, () -> manager.setCommandDispatcher(null));
    }

    @Test
    void testSetConnectionMonitor() {
        manager.setConnectionMonitor(mockConnectionMonitor);
        manager.setConnectionMonitor(null);
    }

    @Test
    void testAddNetworkAdapter() {
        manager.addNetworkAdapter(mockSocketAdapter);
        assertThrows(NullPointerException.class, () -> manager.addNetworkAdapter(null));

        ArgumentCaptor<Consumer<String>> connectedCaptor = ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<Consumer<String>> disconnectedCaptor = ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<BiConsumer<String, Message>> messageCaptor = ArgumentCaptor.forClass(BiConsumer.class);

        verify(mockSocketAdapter).setOnClientConnected(connectedCaptor.capture());
        verify(mockSocketAdapter).setOnClientDisconnected(disconnectedCaptor.capture());
        verify(mockSocketAdapter).setOnMessageReceived(messageCaptor.capture());
    }

    @Test
    void testSetupAdapterCallbacksClientConnected() {
        manager.setGlobalOnClientConnected(mockConnectedHandler);
        manager.addNetworkAdapter(mockSocketAdapter);

        ArgumentCaptor<Consumer<String>> connectedCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(mockSocketAdapter).setOnClientConnected(connectedCaptor.capture());

        connectedCaptor.getValue().accept("client1");
        verify(mockConnectedHandler).accept("client1");
        assertTrue(manager.getAllConnectedClientIds().contains("client1"));
    }

    @Test
    void testSetupAdapterCallbacksClientDisconnected() {
        manager.setGlobalOnClientDisconnected(mockDisconnectedHandler);
        manager.addNetworkAdapter(mockSocketAdapter);

        ArgumentCaptor<Consumer<String>> connectedCaptor = ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<Consumer<String>> disconnectedCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(mockSocketAdapter).setOnClientConnected(connectedCaptor.capture());
        verify(mockSocketAdapter).setOnClientDisconnected(disconnectedCaptor.capture());

        connectedCaptor.getValue().accept("client1");
        disconnectedCaptor.getValue().accept("client1");

        verify(mockDisconnectedHandler).accept("client1");
        assertFalse(manager.getAllConnectedClientIds().contains("client1"));
    }

    @Test
    void testSetupAdapterCallbacksNullGlobalHandlers() {
        manager.addNetworkAdapter(mockSocketAdapter);

        ArgumentCaptor<Consumer<String>> connectedCaptor = ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<Consumer<String>> disconnectedCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(mockSocketAdapter).setOnClientConnected(connectedCaptor.capture());
        verify(mockSocketAdapter).setOnClientDisconnected(disconnectedCaptor.capture());

        connectedCaptor.getValue().accept("client1");
        disconnectedCaptor.getValue().accept("client1");
    }

    @Test
    void testSetupAdapterCallbacksRequestWithDispatcher() {
        manager.setCommandDispatcher(mockCommandDispatcher);
        manager.addNetworkAdapter(mockSocketAdapter);

        ArgumentCaptor<BiConsumer<String, Message>> messageCaptor = ArgumentCaptor.forClass(BiConsumer.class);
        verify(mockSocketAdapter).setOnMessageReceived(messageCaptor.capture());

        messageCaptor.getValue().accept("client1", mockRequest);
        verify(mockCommandDispatcher).dispatch(mockRequest, "client1");
    }

    @Test
    void testSetupAdapterCallbacksRequestWithoutDispatcher() {
        manager.addNetworkAdapter(mockSocketAdapter);

        ArgumentCaptor<BiConsumer<String, Message>> messageCaptor = ArgumentCaptor.forClass(BiConsumer.class);
        verify(mockSocketAdapter).setOnMessageReceived(messageCaptor.capture());

        messageCaptor.getValue().accept("client1", mockRequest);

        ArgumentCaptor<Message> errorCaptor = ArgumentCaptor.forClass(Message.class);
        verify(mockSocketAdapter).sendMessageToClient(eq("client1"), errorCaptor.capture());
        assertTrue(errorCaptor.getValue() instanceof ErrorResponse);
        ErrorResponse error = (ErrorResponse) errorCaptor.getValue();
        assertEquals("test-correlation-id", error.getCorrelationId());
        assertEquals(ErrorResponse.INTERNAL_ERROR, error.getErrorCode());
    }

    @Test
    void testSetupAdapterCallbacksPongMessage() {
        manager.setConnectionMonitor(mockConnectionMonitor);
        manager.addNetworkAdapter(mockSocketAdapter);

        ArgumentCaptor<BiConsumer<String, Message>> messageCaptor = ArgumentCaptor.forClass(BiConsumer.class);
        verify(mockSocketAdapter).setOnMessageReceived(messageCaptor.capture());

        messageCaptor.getValue().accept("client1", mockPongMessage);
        verify(mockConnectionMonitor).handlePong("client1");
    }

    @Test
    void testSetupAdapterCallbacksPongWithoutMonitor() {
        manager.addNetworkAdapter(mockSocketAdapter);

        ArgumentCaptor<BiConsumer<String, Message>> messageCaptor = ArgumentCaptor.forClass(BiConsumer.class);
        verify(mockSocketAdapter).setOnMessageReceived(messageCaptor.capture());

        messageCaptor.getValue().accept("client1", mockPongMessage);
    }

    @Test
    void testSetupAdapterCallbacksUnsupportedMessage() {
        manager.addNetworkAdapter(mockSocketAdapter);

        ArgumentCaptor<BiConsumer<String, Message>> messageCaptor = ArgumentCaptor.forClass(BiConsumer.class);
        verify(mockSocketAdapter).setOnMessageReceived(messageCaptor.capture());

        messageCaptor.getValue().accept("client1", mockMessage);
    }

    @Test
    void testStartAdaptersNoAdapters() throws IOException {
        manager.startAdapters(8080, 1099);
    }

    @Test
    void testStartAdaptersSocketAdapter() throws IOException {
        manager.addNetworkAdapter(mockSocketAdapter);

        manager.startAdapters(8080, 1099);

        verify(mockSocketAdapter).startServer(8080);
    }

    @Test
    void testStartAdaptersRMIAdapter() throws IOException {
        manager.addNetworkAdapter(mockRMIAdapter);

        manager.startAdapters(8080, 1099);

        verify(mockRMIAdapter).startServer(1099);
    }

    @Test
    void testStartAdaptersUnknownAdapter() throws IOException {
        manager.addNetworkAdapter(mockUnknownAdapter);

        manager.startAdapters(8080, 1099);
    }

    @Test
    void testStartAdaptersWithException() throws IOException {
        manager.addNetworkAdapter(mockSocketAdapter);
        doThrow(new IOException("Connection failed")).when(mockSocketAdapter).startServer(8080);

        assertThrows(IOException.class, () -> manager.startAdapters(8080, 1099));
    }

    @Test
    void testStartAdaptersPartialFailure() throws IOException {
        manager.addNetworkAdapter(mockSocketAdapter);
        manager.addNetworkAdapter(mockRMIAdapter);
        doThrow(new IOException("Connection failed")).when(mockSocketAdapter).startServer(8080);

        manager.startAdapters(8080, 1099);

        verify(mockRMIAdapter).startServer(1099);
    }

    @Test
    void testSetGlobalOnClientConnected() {
        manager.setGlobalOnClientConnected(mockConnectedHandler);
        assertThrows(NullPointerException.class, () ->
                manager.setGlobalOnClientConnected(null));
    }

    @Test
    void testSetGlobalOnClientDisconnected() {
        manager.setGlobalOnClientDisconnected(mockDisconnectedHandler);
        assertThrows(NullPointerException.class, () ->
                manager.setGlobalOnClientDisconnected(null));
    }

    @Test
    void testStop() {
        manager.addNetworkAdapter(mockSocketAdapter);
        manager.addNetworkAdapter(mockRMIAdapter);
        when(mockSocketAdapter.isRunning()).thenReturn(true);
        when(mockRMIAdapter.isRunning()).thenReturn(false);

        manager.stop();

        verify(mockSocketAdapter).stopServer();
        verify(mockRMIAdapter, never()).stopServer();
        assertTrue(manager.getAllConnectedClientIds().isEmpty());
    }

    @Test
    void testStopWithException() {
        manager.addNetworkAdapter(mockSocketAdapter);
        when(mockSocketAdapter.isRunning()).thenReturn(true);
        doThrow(new RuntimeException("Stop failed")).when(mockSocketAdapter).stopServer();

        manager.stop();

        verify(mockSocketAdapter).stopServer();
    }

    @Test
    void testSendMessageToClientSuccess() {
        manager.addNetworkAdapter(mockSocketAdapter);
        simulateClientConnection("client1", mockSocketAdapter);
        when(mockSocketAdapter.isRunning()).thenReturn(true);
        when(mockSocketAdapter.sendMessageToClient("client1", mockMessage)).thenReturn(true);

        boolean result = manager.sendMessageToClient("client1", mockMessage);

        assertTrue(result);
        verify(mockSocketAdapter).sendMessageToClient("client1", mockMessage);
    }

    @Test
    void testSendMessageToClientNullParameters() {
        assertThrows(NullPointerException.class, () ->
                manager.sendMessageToClient(null, mockMessage));
        assertThrows(NullPointerException.class, () ->
                manager.sendMessageToClient("client1", null));
    }

    @Test
    void testSendMessageToClientNoAdapter() {
        boolean result = manager.sendMessageToClient("client1", mockMessage);
        assertFalse(result);
    }

    @Test
    void testSendMessageToClientAdapterNotRunning() {
        manager.addNetworkAdapter(mockSocketAdapter);
        simulateClientConnection("client1", mockSocketAdapter);
        when(mockSocketAdapter.isRunning()).thenReturn(false);

        boolean result = manager.sendMessageToClient("client1", mockMessage);

        assertFalse(result);
        verify(mockSocketAdapter, never()).sendMessageToClient(anyString(), any());
    }

    @Test
    void testBroadcastMessageToAllClients() {
        manager.addNetworkAdapter(mockSocketAdapter);
        simulateClientConnection("client1", mockSocketAdapter);
        simulateClientConnection("client2", mockSocketAdapter);
        when(mockSocketAdapter.isRunning()).thenReturn(true);
        when(mockSocketAdapter.sendMessageToClient(anyString(), any())).thenReturn(true);

        manager.broadcastMessageToAllClients(mockMessage);

        verify(mockSocketAdapter).sendMessageToClient("client1", mockMessage);
        verify(mockSocketAdapter).sendMessageToClient("client2", mockMessage);
    }

    @Test
    void testBroadcastMessageToAllClientsNullMessage() {
        assertThrows(NullPointerException.class, () ->
                manager.broadcastMessageToAllClients(null));
    }

    @Test
    void testBroadcastMessageToAllClientsNoClients() {
        manager.broadcastMessageToAllClients(mockMessage);
    }

    @Test
    void testBroadcastMessageToSessionClients() {
        manager.addNetworkAdapter(mockSocketAdapter);
        simulateClientConnection("client1", mockSocketAdapter);
        simulateClientConnection("client2", mockSocketAdapter);
        when(mockSocketAdapter.isRunning()).thenReturn(true);
        when(mockSocketAdapter.sendMessageToClient(anyString(), any())).thenReturn(true);

        List<String> targetClients = Arrays.asList("client1", "client2");
        manager.broadcastMessageToSessionClients(mockMessage, targetClients);

        verify(mockSocketAdapter).sendMessageToClient("client1", mockMessage);
        verify(mockSocketAdapter).sendMessageToClient("client2", mockMessage);
    }

    @Test
    void testBroadcastMessageToSessionClientsNullParameters() {
        assertThrows(NullPointerException.class, () ->
                manager.broadcastMessageToSessionClients(null, Arrays.asList("client1")));
        assertThrows(NullPointerException.class, () ->
                manager.broadcastMessageToSessionClients(mockMessage, null));
    }

    @Test
    void testBroadcastMessageToSessionClientsEmptyList() {
        manager.broadcastMessageToSessionClients(mockMessage, Arrays.asList());
    }

    @Test
    void testIsRunningTrue() {
        manager.addNetworkAdapter(mockSocketAdapter);
        when(mockSocketAdapter.isRunning()).thenReturn(true);

        assertTrue(manager.isRunning());
    }

    @Test
    void testIsRunningFalse() {
        manager.addNetworkAdapter(mockSocketAdapter);
        when(mockSocketAdapter.isRunning()).thenReturn(false);

        assertFalse(manager.isRunning());
    }

    @Test
    void testIsRunningNoAdapters() {
        assertFalse(manager.isRunning());
    }

    @Test
    void testGetAllConnectedClientIds() {
        manager.addNetworkAdapter(mockSocketAdapter);
        simulateClientConnection("client1", mockSocketAdapter);
        simulateClientConnection("client2", mockSocketAdapter);

        assertEquals(2, manager.getAllConnectedClientIds().size());
        assertTrue(manager.getAllConnectedClientIds().contains("client1"));
        assertTrue(manager.getAllConnectedClientIds().contains("client2"));
    }

    private void simulateClientConnection(String clientId, ServerNetworkInterface adapter) {
        ArgumentCaptor<Consumer<String>> connectedCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(adapter).setOnClientConnected(connectedCaptor.capture());
        connectedCaptor.getValue().accept(clientId);
    }
}