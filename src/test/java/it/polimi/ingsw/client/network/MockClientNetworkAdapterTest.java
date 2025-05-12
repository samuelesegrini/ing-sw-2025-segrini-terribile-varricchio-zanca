package it.polimi.ingsw.client.network;

import it.polimi.ingsw.common.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class MockClientNetworkAdapterTest {

    // A mock implementation of the ClientNetworkInterface for testing
    static class MockClientNetworkAdapter implements ClientNetworkInterface {
        private boolean connected = false;
        private Consumer<Message> messageHandler;
        private Runnable disconnectHandler;
        private final List<Message> sentMessages = new ArrayList<>();

        @Override
        public void connect(String host, int port) throws IOException {
            if ("invalid".equals(host)) {
                throw new IOException("Invalid host for testing");
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

        // Methods to help with testing
        public void simulateMessageFromServer(Message message) {
            if (connected && messageHandler != null) {
                messageHandler.accept(message);
            }
        }

        public List<Message> getSentMessages() {
            return new ArrayList<>(sentMessages);
        }
    }

    // A simple Message implementation for testing
    static class TestMessage implements Message {
        private static final long serialVersionUID = 1L;
        private final String content;

        public TestMessage(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }
    }

    private MockClientNetworkAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MockClientNetworkAdapter();
    }

    @Test
    void initialStateIsNotConnected() {
        assertFalse(adapter.isConnected(), "Initial state should be disconnected");
    }

    @Test
    void connectSetsConnectedState() throws IOException {
        adapter.connect("localhost", 8080);
        assertTrue(adapter.isConnected(), "Should be connected after connect call");
    }

    @Test
    void connectWithInvalidHostThrowsException() {
        assertThrows(IOException.class, () -> adapter.connect("invalid", 8080));
    }

    @Test
    void disconnectSetsDisconnectedState() throws IOException {
        adapter.connect("localhost", 8080);
        adapter.disconnect();
        assertFalse(adapter.isConnected(), "Should be disconnected after disconnect call");
    }

    @Test
    void disconnectCallsHandler() throws IOException {
        adapter.connect("localhost", 8080);

        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        adapter.setOnDisconnected(() -> handlerCalled.set(true));

        adapter.disconnect();
        assertTrue(handlerCalled.get(), "Disconnect handler should be called");
    }

    @Test
    void cannotSendMessageWhenNotConnected() {
        TestMessage message = new TestMessage("test message");
        assertFalse(adapter.sendMessage(message), "Cannot send message when not connected");
        assertTrue(adapter.getSentMessages().isEmpty(), "No messages should be sent");
    }

    @Test
    void canSendMessageWhenConnected() throws IOException {
        adapter.connect("localhost", 8080);
        TestMessage message = new TestMessage("test message");

        assertTrue(adapter.sendMessage(message), "Should be able to send message when connected");
        assertEquals(1, adapter.getSentMessages().size(), "Message should be added to sent list");
        assertEquals("test message", ((TestMessage)adapter.getSentMessages().get(0)).getContent());
    }

    @Test
    void messageHandlerReceivesMessages() throws IOException {
        adapter.connect("localhost", 8080);

        TestMessage receivedMessage = null;
        final TestMessage[] messageHolder = new TestMessage[1];
        
        adapter.setOnMessageReceived(message -> {
            messageHolder[0] = (TestMessage) message;
        });

        TestMessage serverMessage = new TestMessage("from server");
        adapter.simulateMessageFromServer(serverMessage);

        assertEquals("from server", messageHolder[0].getContent(), 
                "Message handler should receive the message from server");
    }
    
    @Test
    void messageHandlerNotCalledWhenDisconnected() {
        final boolean[] handlerCalled = {false};
        
        adapter.setOnMessageReceived(message -> handlerCalled[0] = true);
        
        // Should not call handler when disconnected
        adapter.simulateMessageFromServer(new TestMessage("test"));
        
        assertFalse(handlerCalled[0], "Message handler should not be called when disconnected");
    }
} 