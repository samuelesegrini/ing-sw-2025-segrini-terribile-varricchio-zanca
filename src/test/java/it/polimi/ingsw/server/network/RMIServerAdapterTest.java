package it.polimi.ingsw.server.network;

import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.common.network.rmi.IClientRemoteListener;
import it.polimi.ingsw.common.network.rmi.IServerRemote;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RMIServerAdapterTest {

    @Mock
    private Registry mockRegistry;
    @Mock
    private IClientRemoteListener mockClientListener;
    @Mock
    private Message mockMessage;
    @Mock
    private Consumer<String> mockConnectedCallback;
    @Mock
    private Consumer<String> mockDisconnectedCallback;
    @Mock
    private BiConsumer<String, Message> mockMessageCallback;

    private RMIServerAdapter adapter;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() throws RemoteException {
        mocks = MockitoAnnotations.openMocks(this);
        adapter = new RMIServerAdapter();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (adapter.isRunning()) {
            adapter.stopServer();
        }
        mocks.close();
    }

    @Test
    void testStartServerSuccess() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);

            adapter.startServer(1099);

            assertTrue(adapter.isRunning());
            verify(mockRegistry).rebind(eq(IServerRemote.SERVICE_NAME), eq(adapter));
        }
    }

    @Test
    void testStartServerRegistryExists() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099))
                    .thenThrow(new RemoteException("Registry exists"));
            locateRegistryMock.when(() -> LocateRegistry.getRegistry(1099)).thenReturn(mockRegistry);

            adapter.startServer(1099);

            assertTrue(adapter.isRunning());
            verify(mockRegistry).rebind(eq(IServerRemote.SERVICE_NAME), eq(adapter));
        }
    }

    @Test
    void testStartServerAlreadyRunning() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);

            adapter.startServer(1099);
            adapter.startServer(1099);

            assertTrue(adapter.isRunning());
            verify(mockRegistry, times(1)).rebind(eq(IServerRemote.SERVICE_NAME), eq(adapter));
        }
    }

    @Test
    void testStartServerFailure() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099))
                    .thenThrow(new RemoteException("Failed"));
            locateRegistryMock.when(() -> LocateRegistry.getRegistry(1099))
                    .thenThrow(new RemoteException("Failed"));

            assertThrows(IOException.class, () -> adapter.startServer(1099));
            assertFalse(adapter.isRunning());
        }
    }

    @Test
    void testStopServerWhenRunning() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class);
             MockedStatic<UnicastRemoteObject> unicastMock = mockStatic(UnicastRemoteObject.class)) {

            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);

            adapter.stopServer();

            assertFalse(adapter.isRunning());
            verify(mockRegistry).unbind(IServerRemote.SERVICE_NAME);
            unicastMock.verify(() -> UnicastRemoteObject.unexportObject(adapter, true));
        }
    }

    @Test
    void testStopServerWhenNotRunning() {
        adapter.stopServer();
        assertFalse(adapter.isRunning());
    }

    @Test
    void testStopServerWithExceptions() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class);
             MockedStatic<UnicastRemoteObject> unicastMock = mockStatic(UnicastRemoteObject.class)) {

            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);

            doThrow(new Exception("Unbind failed")).when(mockRegistry).unbind(IServerRemote.SERVICE_NAME);
            unicastMock.when(() -> UnicastRemoteObject.unexportObject(adapter, true))
                    .thenThrow(new Exception("Unexport failed"));

            adapter.stopServer();

            assertFalse(adapter.isRunning());
        }
    }

    @Test
    void testRegisterClient() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);
            adapter.setOnClientConnected(mockConnectedCallback);

            String sessionToken = adapter.registerClient(mockClientListener);

            assertNotNull(sessionToken);
            assertTrue(sessionToken.startsWith("rmi-sid-"));
            verify(mockConnectedCallback).accept(sessionToken);
        }
    }

    @Test
    void testRegisterClientWhenNotRunning() {
        assertThrows(RemoteException.class, () -> adapter.registerClient(mockClientListener));
    }

    @Test
    void testRegisterClientWithoutCallback() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);

            String sessionToken = adapter.registerClient(mockClientListener);

            assertNotNull(sessionToken);
        }
    }

    @Test
    void testUnregisterClient() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);
            adapter.setOnClientDisconnected(mockDisconnectedCallback);
            String sessionToken = adapter.registerClient(mockClientListener);

            adapter.unregisterClient(sessionToken);

            verify(mockDisconnectedCallback).accept(sessionToken);
        }
    }

    @Test
    void testUnregisterClientNullToken() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);

            adapter.unregisterClient(null);
        }
    }

    @Test
    void testUnregisterClientNonExistentToken() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);

            adapter.unregisterClient("non-existent");
        }
    }

    @Test
    void testDispatchClientCommand() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);
            adapter.setOnMessageReceived(mockMessageCallback);
            String sessionToken = adapter.registerClient(mockClientListener);

            adapter.dispatchClientCommand(sessionToken, mockMessage);

            verify(mockMessageCallback).accept(sessionToken, mockMessage);
        }
    }

    @Test
    void testDispatchClientCommandWhenNotRunning() {
        assertThrows(RemoteException.class, () ->
                adapter.dispatchClientCommand("token", mockMessage));
    }

    @Test
    void testDispatchClientCommandNullToken() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);

            assertThrows(RemoteException.class, () ->
                    adapter.dispatchClientCommand(null, mockMessage));
        }
    }

    @Test
    void testDispatchClientCommandInvalidToken() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);

            assertThrows(RemoteException.class, () ->
                    adapter.dispatchClientCommand("invalid", mockMessage));
        }
    }

    @Test
    void testDispatchClientCommandNullCallback() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);
            String sessionToken = adapter.registerClient(mockClientListener);

            adapter.dispatchClientCommand(sessionToken, mockMessage);
        }
    }

    @Test
    void testPingServer() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);

            adapter.pingServer();
        }
    }

    @Test
    void testPingServerWhenNotRunning() {
        assertThrows(RemoteException.class, () -> adapter.pingServer());
    }

    @Test
    void testSendMessageToClientSuccess() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);
            String sessionToken = adapter.registerClient(mockClientListener);

            boolean result = adapter.sendMessageToClient(sessionToken, mockMessage);

            assertTrue(result);
            verify(mockClientListener).onMessageFromServer(mockMessage);
        }
    }

    @Test
    void testSendMessageToClientWhenNotRunning() {
        boolean result = adapter.sendMessageToClient("token", mockMessage);
        assertFalse(result);
    }

    @Test
    void testSendMessageToClientNonExistent() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);

            boolean result = adapter.sendMessageToClient("non-existent", mockMessage);

            assertFalse(result);
        }
    }

    @Test
    void testSendMessageToClientRemoteException() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);
            adapter.setOnClientDisconnected(mockDisconnectedCallback);
            String sessionToken = adapter.registerClient(mockClientListener);
            doThrow(new RemoteException("Connection failed")).when(mockClientListener)
                    .onMessageFromServer(mockMessage);

            boolean result = adapter.sendMessageToClient(sessionToken, mockMessage);

            assertFalse(result);
            verify(mockDisconnectedCallback).accept(sessionToken);
        }
    }

    @Test
    void testBroadcastMessage() throws Exception {
        try (MockedStatic<LocateRegistry> locateRegistryMock = mockStatic(LocateRegistry.class)) {
            locateRegistryMock.when(() -> LocateRegistry.createRegistry(1099)).thenReturn(mockRegistry);
            adapter.startServer(1099);
            String sessionToken1 = adapter.registerClient(mockClientListener);
            IClientRemoteListener mockClientListener2 = mock(IClientRemoteListener.class);
            String sessionToken2 = adapter.registerClient(mockClientListener2);

            adapter.broadcastMessage(mockMessage);

            verify(mockClientListener).onMessageFromServer(mockMessage);
            verify(mockClientListener2).onMessageFromServer(mockMessage);
        }
    }

    @Test
    void testBroadcastMessageWhenNotRunning() {
        adapter.broadcastMessage(mockMessage);
    }

    @Test
    void testSetOnClientConnected() {
        assertThrows(NullPointerException.class, () -> adapter.setOnClientConnected(null));
        adapter.setOnClientConnected(mockConnectedCallback);
    }

    @Test
    void testSetOnClientDisconnected() {
        assertThrows(NullPointerException.class, () -> adapter.setOnClientDisconnected(null));
        adapter.setOnClientDisconnected(mockDisconnectedCallback);
    }

    @Test
    void testSetOnMessageReceived() {
        assertThrows(NullPointerException.class, () -> adapter.setOnMessageReceived(null));
        adapter.setOnMessageReceived(mockMessageCallback);
    }

    @Test
    void testIsRunning() {
        assertFalse(adapter.isRunning());
    }
}



