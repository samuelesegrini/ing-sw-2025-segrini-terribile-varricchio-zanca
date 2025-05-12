package it.polimi.ingsw.server.network;

import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.message.Message;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the server-side network operations, using a specific
 * ServerNetworkInterface implementation (e.g., SocketServerAdapter).
 * It bridges between the raw network events and the server's EventBus.
 */
public class ServerNetworkManager {
    private static final Logger LOGGER = Logger.getLogger(ServerNetworkManager.class.getName());

    private final ServerNetworkInterface networkAdapter;
    private final EventBus serverEventBus; // The main server EventBus or a game-specific one

    /**
     * Wrapper message to post to the EventBus, including the clientId.
     */
    public static class IncomingClientMessage implements Message {
        private static final long serialVersionUID = 1L;
        private final String clientId;
        private final Message originalMessage;
        private final long timestamp;


        public IncomingClientMessage(String clientId, Message originalMessage) {
            this.clientId = clientId;
            this.originalMessage = originalMessage;
            this.timestamp = System.currentTimeMillis();
        }

        public String getClientId() {
            return clientId;
        }

        public Message getOriginalMessage() {
            return originalMessage;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return "IncomingClientMessage{" +
                    "clientId='" + clientId + '\'' +
                    ", originalMessage=" + originalMessage.getClass().getSimpleName() +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    // You might also define outgoing message wrappers if needed, or handle via specific event handlers
    // e.g., public static class SendToClientEvent implements Message { /* ... */ }

    public ServerNetworkManager(ServerNetworkInterface networkAdapter, EventBus serverEventBus) {
        this.networkAdapter = Objects.requireNonNull(networkAdapter, "Network adapter cannot be null");
        this.serverEventBus = Objects.requireNonNull(serverEventBus, "Server EventBus cannot be null");

        // Register this manager to listen for events that require sending messages (optional here,
        // can be done by GameLogicController or PlayerClientProxy)
        // serverEventBus.register(this);

        setupAdapterCallbacks();
    }

    private void setupAdapterCallbacks() {
        networkAdapter.setOnClientConnected(clientId -> {
            LOGGER.info("NetworkManager: Client connected - " + clientId);
            // Optionally, post a specific system event to the serverEventBus
            // serverEventBus.post(new ClientConnectedSystemEvent(clientId));
        });

        networkAdapter.setOnClientDisconnected(clientId -> {
            LOGGER.info("NetworkManager: Client disconnected - " + clientId);
            // Optionally, post a specific system event to the serverEventBus
            // serverEventBus.post(new ClientDisconnectedSystemEvent(clientId));
        });

        networkAdapter.setOnMessageReceived((clientId, message) -> {
            LOGGER.finer("NetworkManager: Message received from " + clientId + ": " + message.getClass().getSimpleName());
            // Wrap the message with clientId and post to the EventBus
            // so that other components (e.g., GameLogicController) can handle it
            // and know who sent it.
            serverEventBus.post(new IncomingClientMessage(clientId, message));
        });
    }

    public void start(int port) throws IOException {
        LOGGER.info("ServerNetworkManager starting network adapter on port " + port);
        networkAdapter.startServer(port);
    }

    public void stop() {
        LOGGER.info("ServerNetworkManager stopping network adapter.");
        networkAdapter.stopServer();
    }

    /**
     * Sends a message to a specific client.
     * This method would typically be called by a component that has processed an event
     * from the EventBus and determined a response needs to be sent.
     * @param clientId The ID of the client.
     * @param message The message to send.
     */
    public void sendMessageToClient(String clientId, Message message) {
        if (!networkAdapter.isRunning()) {
            LOGGER.warning("Cannot send message; server network adapter is not running.");
            return;
        }
        LOGGER.finer("NetworkManager: Sending message to " + clientId + ": " + message.getClass().getSimpleName());
        networkAdapter.sendMessageToClient(clientId, message);
    }

    /**
     * Broadcasts a message to all connected clients.
     * @param message The message to broadcast.
     */
    public void broadcastMessage(Message message) {
        if (!networkAdapter.isRunning()) {
            LOGGER.warning("Cannot broadcast message; server network adapter is not running.");
            return;
        }
        LOGGER.finer("NetworkManager: Broadcasting message: " + message.getClass().getSimpleName());
        networkAdapter.broadcastMessage(message);
    }
    /**
     * Checks if the underlying network adapter is currently running.
     * @return true if the network adapter is running, false otherwise.
     */
    public boolean isRunning() {
        return networkAdapter.isRunning();
    }
}