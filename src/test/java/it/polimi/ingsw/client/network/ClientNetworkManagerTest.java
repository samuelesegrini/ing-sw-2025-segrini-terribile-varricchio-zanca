package it.polimi.ingsw.client.network;

import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class ClientNetworkManagerTest {

    // Mock implementation of the ClientNetworkInterface for testing
    static class MockClientNetwork implements ClientNetworkInterface {
        private boolean connected = false;
        private Consumer<Message> messageHandler;
        private Runnable disconnectHandler;
        private final List<Message> sentMessages = new ArrayList<>();
        private String lastHost;
        private int lastPort;
        private boolean throwOnConnect = false;

        @Override
        public void connect(String host, int port) throws IOException {
            lastHost = host;
            lastPort = port;
            if (throwOnConnect) {
                throw new IOException("Connection error (test)");
            }
            connected = true;
        }

        @Override
        public void disconnect() {
            if (connected && disconnectHandler != null) {
                disconnectHandler.run();
            }
            connected = false;
        }

        @Override
        public boolean sendMessage(Message message) {
            if (!connected) {
                return false;
            }
            sentMessages.add(message);
            return true;
        }

        @Override
        public void setOnMessageReceived(Consumer<Message> onMessageReceivedHandler) {
            this.messageHandler = onMessageReceivedHandler;
        }

        @Override
        public void setOnDisconnected(Runnable onDisconnectedHandler) {
            this.disconnectHandler = onDisconnectedHandler;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        // Helper methods for testing
        public void simulateMessageFromServer(Message message) {
            if (connected && messageHandler != null) {
                messageHandler.accept(message);
            }
        }

        public void simulateDisconnect() {
            if (connected && disconnectHandler != null) {
                disconnectHandler.run();
            }
            connected = false;
        }

        public List<Message> getSentMessages() {
            return new ArrayList<>(sentMessages);
        }

        public void setThrowOnConnect(boolean throwOnConnect) {
            this.throwOnConnect = throwOnConnect;
        }

        public String getLastHost() {
            return lastHost;
        }

        public int getLastPort() {
            return lastPort;
        }
    }

    // Message class for testing
    static class TestMessage implements Message {
        private static final long serialVersionUID = 1L;
        private final String content;

        public TestMessage(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        @Override
        public String toString() {
            return "TestMessage{content='" + content + "'}";
        }
    }

    // EventBus message handler for testing
    static class TestMessageListener {
        private final List<Message> receivedMessages = new ArrayList<>();
        private CountDownLatch messageLatch;
        private CountDownLatch disconnectLatch;

        public TestMessageListener(int expectedMessages, int expectedDisconnects) {
            messageLatch = new CountDownLatch(expectedMessages);
            disconnectLatch = new CountDownLatch(expectedDisconnects);
        }

        @it.polimi.ingsw.common.event.MessageHandler
        public void handleTestMessage(TestMessage message) {
            receivedMessages.add(message);
            messageLatch.countDown();
        }

        @it.polimi.ingsw.common.event.MessageHandler
        public void handleDisconnect(ClientNetworkManager.ServerDisconnectedSystemEvent event) {
            disconnectLatch.countDown();
        }

        public List<Message> getReceivedMessages() {
            return new ArrayList<>(receivedMessages);
        }

        public boolean awaitMessages(long timeout, TimeUnit unit) throws InterruptedException {
            return messageLatch.await(timeout, unit);
        }

        public boolean awaitDisconnect(long timeout, TimeUnit unit) throws InterruptedException {
            return disconnectLatch.await(timeout, unit);
        }

        public void resetLatches(int expectedMessages, int expectedDisconnects) {
            messageLatch = new CountDownLatch(expectedMessages);
            disconnectLatch = new CountDownLatch(expectedDisconnects);
        }
    }

    private MockClientNetwork mockNetwork;
    private EventBus eventBus;
    private ClientNetworkManager networkManager;
    private TestMessageListener messageListener;

    @BeforeEach
    void setUp() {
        mockNetwork = new MockClientNetwork();
        eventBus = new EventBus(2, "test-network-manager");
        networkManager = new ClientNetworkManager(mockNetwork, eventBus);
        messageListener = new TestMessageListener(1, 1);
        eventBus.register(messageListener);
    }

    @Test
    void constructorThrowsIfNetworkAdapterIsNull() {
        assertThrows(NullPointerException.class, () -> new ClientNetworkManager(null, eventBus));
    }

    @Test
    void constructorThrowsIfEventBusIsNull() {
        assertThrows(NullPointerException.class, () -> new ClientNetworkManager(mockNetwork, null));
    }

    @Test
    void connectPassesHostAndPortToAdapter() throws IOException {
        String host = "testserver.com";
        int port = 12345;
        
        networkManager.connect(host, port);
        
        assertEquals(host, mockNetwork.getLastHost());
        assertEquals(port, mockNetwork.getLastPort());
        assertTrue(mockNetwork.isConnected());
    }

    @Test
    void connectDisconnectsFirstIfAlreadyConnected() throws IOException {
        // Connect first time
        networkManager.connect("first.com", 8080);
        assertTrue(mockNetwork.isConnected());
        
        // Connect again to a different address
        networkManager.connect("second.com", 9090);
        
        assertEquals("second.com", mockNetwork.getLastHost());
        assertEquals(9090, mockNetwork.getLastPort());
        assertTrue(mockNetwork.isConnected());
    }

    @Test
    void connectThrowsIOExceptionOnFailure() {
        mockNetwork.setThrowOnConnect(true);
        
        assertThrows(IOException.class, () -> networkManager.connect("server.com", 8080));
        assertFalse(mockNetwork.isConnected());
    }

    @Test
    void disconnectCallsAdapterDisconnect() throws IOException {
        networkManager.connect("server.com", 8080);
        assertTrue(mockNetwork.isConnected());
        
        networkManager.disconnect();
        
        assertFalse(mockNetwork.isConnected());
    }

    @Test
    void sendMessageDelegatesWhenConnected() throws IOException {
        networkManager.connect("server.com", 8080);
        TestMessage message = new TestMessage("test message");
        
        assertTrue(networkManager.sendMessage(message));
        
        assertEquals(1, mockNetwork.getSentMessages().size());
        assertEquals("test message", ((TestMessage)mockNetwork.getSentMessages().get(0)).getContent());
    }

    @Test
    void sendMessageReturnsFalseWhenNotConnected() {
        TestMessage message = new TestMessage("test message");
        
        assertFalse(networkManager.sendMessage(message));
        assertTrue(mockNetwork.getSentMessages().isEmpty());
    }

    @Test
    void isConnectedMatchesAdapterState() throws IOException {
        assertFalse(networkManager.isConnected());
        
        networkManager.connect("server.com", 8080);
        assertTrue(networkManager.isConnected());
        
        networkManager.disconnect();
        assertFalse(networkManager.isConnected());
    }

    @Test
    void messageFromServerPostedToEventBus() throws IOException, InterruptedException {
        networkManager.connect("server.com", 8080);
        
        TestMessage testMessage = new TestMessage("from server");
        mockNetwork.simulateMessageFromServer(testMessage);
        
        assertTrue(messageListener.awaitMessages(500, TimeUnit.MILLISECONDS));
        assertEquals(1, messageListener.getReceivedMessages().size());
        assertEquals("from server", ((TestMessage)messageListener.getReceivedMessages().get(0)).getContent());
    }
    
    @Test
    void disconnectFromServerPostedToEventBus() throws IOException, InterruptedException {
        networkManager.connect("server.com", 8080);
        
        mockNetwork.simulateDisconnect();
        
        assertTrue(messageListener.awaitDisconnect(500, TimeUnit.MILLISECONDS));
    }
    
    @Test
    void multipleMessagesSentAndReceived() throws IOException, InterruptedException {
        // Setup listener for multiple messages
        messageListener = new TestMessageListener(3, 0);
        eventBus.register(messageListener);
        
        networkManager.connect("server.com", 8080);
        
        // Send messages to server
        networkManager.sendMessage(new TestMessage("outgoing 1"));
        networkManager.sendMessage(new TestMessage("outgoing 2"));
        networkManager.sendMessage(new TestMessage("outgoing 3"));
        
        assertEquals(3, mockNetwork.getSentMessages().size());
        
        // Receive messages from server
        mockNetwork.simulateMessageFromServer(new TestMessage("incoming 1"));
        mockNetwork.simulateMessageFromServer(new TestMessage("incoming 2"));
        mockNetwork.simulateMessageFromServer(new TestMessage("incoming 3"));
        
        assertTrue(messageListener.awaitMessages(500, TimeUnit.MILLISECONDS));
        assertEquals(3, messageListener.getReceivedMessages().size());
    }
} 