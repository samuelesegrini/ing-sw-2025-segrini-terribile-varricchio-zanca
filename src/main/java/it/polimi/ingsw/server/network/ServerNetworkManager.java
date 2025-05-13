package it.polimi.ingsw.server.network;

import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.message.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the server-side network operations, using a specific
 * ServerNetworkInterface implementation (e.g., SocketServerAdapter).
 * It bridges between the raw network events and the server's EventBus.
 */
public class ServerNetworkManager {
    private static final Logger LOGGER = Logger.getLogger(ServerNetworkManager.class.getName());

    // Store multiple network interfaces
    private final List<ServerNetworkInterface> networkAdapters = new ArrayList<>();
    private final EventBus serverEventBus;
    private final Map<String, ServerNetworkInterface> clientToAdapterMap = new ConcurrentHashMap<>();

    // Callbacks from network adapters (these will be set on each adapter)
    private Consumer<String> globalOnClientConnectedHandler = clientId -> {};
    private Consumer<String> globalOnClientDisconnectedHandler = clientId -> {};

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
        public String getClientId() { return clientId; }
        public Message getOriginalMessage() { return originalMessage; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return "IncomingClientMessage{" +
                    "clientId='" + clientId + '\'' +
                    ", originalMessage=" + originalMessage.getClass().getSimpleName() +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    public ServerNetworkManager(EventBus serverEventBus) {
        this.serverEventBus = Objects.requireNonNull(serverEventBus, "Server EventBus cannot be null");
    }

    public void addNetworkAdapter(ServerNetworkInterface adapter) {
        Objects.requireNonNull(adapter, "Network adapter cannot be null");
        this.networkAdapters.add(adapter);
        setupAdapterCallbacks(adapter);
        LOGGER.info("Added network adapter: " + adapter.getClass().getSimpleName());
    }

    private void setupAdapterCallbacks(ServerNetworkInterface specificAdapter) {
        specificAdapter.setOnClientConnected(clientId -> {
            LOGGER.info("ServerNetworkManager: Client connected via " + specificAdapter.getClass().getSimpleName() + " - ID: " + clientId);
            clientToAdapterMap.put(clientId, specificAdapter); // Store the mapping
            if (globalOnClientConnectedHandler != null) {
                globalOnClientConnectedHandler.accept(clientId); // Call the globally set handler
            }
        });

        specificAdapter.setOnClientDisconnected(clientId -> {
            LOGGER.info("ServerNetworkManager: Client disconnected via " + specificAdapter.getClass().getSimpleName() + " - ID: " + clientId);
            clientToAdapterMap.remove(clientId); // Remove the mapping
            if (globalOnClientDisconnectedHandler != null) {
                globalOnClientDisconnectedHandler.accept(clientId); // Call the globally set handler
            }
        });

        specificAdapter.setOnMessageReceived((clientId, message) -> {
            LOGGER.finer("ServerNetworkManager: Message received via " + specificAdapter.getClass().getSimpleName() + " from " + clientId + ": " + message.getClass().getSimpleName());
            serverEventBus.post(new IncomingClientMessage(clientId, message));
        });
    }

    public void startAdapters(int socketPort, int rmiPort) throws IOException {
        boolean startedAtLeastOne = false;
        for (ServerNetworkInterface adapter : networkAdapters) {
            try {
                if (adapter instanceof SocketServerAdapter) {
                    LOGGER.info("ServerNetworkManager starting Socket adapter on port " + socketPort);
                    adapter.startServer(socketPort);
                    startedAtLeastOne = true;
                } else if (adapter instanceof RMIServerAdapter) {
                    LOGGER.info("ServerNetworkManager starting RMI adapter on port " + rmiPort);
                    adapter.startServer(rmiPort);
                    startedAtLeastOne = true;
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to start adapter " + adapter.getClass().getSimpleName(), e);
                throw e;
            }
        }
        if (!startedAtLeastOne && !networkAdapters.isEmpty()) {
            throw new IOException("No suitable adapters were started, but adapters were configured.");
        }
        if (networkAdapters.isEmpty()){
            LOGGER.warning("ServerNetworkManager: No network adapters configured to start.");
        }
    }

    public void setGlobalOnClientConnected(Consumer<String> handler) {
        this.globalOnClientConnectedHandler = Objects.requireNonNull(handler);
    }

    public void setGlobalOnClientDisconnected(Consumer<String> handler) {
        this.globalOnClientDisconnectedHandler = Objects.requireNonNull(handler);
    }

    public void stop() {
        LOGGER.info("ServerNetworkManager stopping all network adapters.");
        for (ServerNetworkInterface adapter : networkAdapters) {
            try {
                if (adapter.isRunning()) {
                    adapter.stopServer();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error stopping adapter " + adapter.getClass().getSimpleName(), e);
            }
        }
        networkAdapters.clear();
        clientToAdapterMap.clear(); // Clear the mapping on stop
    }

    /**
     * Sends a message to a specific client using the adapter that originally handled their connection.
     * @param clientId The unique ID of the target client.
     * @param message The message to send.
     */
    public void sendMessageToClient(String clientId, Message message) {
        ServerNetworkInterface adapter = clientToAdapterMap.get(clientId);

        if (adapter != null) {
            if (adapter.isRunning()) {
                LOGGER.finer("ServerNetworkManager: Sending message to " + clientId + " via " + adapter.getClass().getSimpleName());
                boolean sent = adapter.sendMessageToClient(clientId, message);
                if (!sent) {
                    LOGGER.warning("ServerNetworkManager: Adapter " + adapter.getClass().getSimpleName() +
                            " reported failure sending message to client " + clientId);
                    // The adapter itself should handle client removal from its internal list if sending fails due to disconnect
                    // The onClientDisconnected callback from the adapter would then trigger removal from clientToAdapterMap
                }
            } else {
                LOGGER.warning("ServerNetworkManager: Adapter for client " + clientId + " (" +
                        adapter.getClass().getSimpleName() + ") is not running. Message not sent.");
                // Client might have disconnected and adapter stopped, but mapping not yet cleared.
                // Or adapter failed to start.
            }
        } else {
            LOGGER.warning("ServerNetworkManager: No adapter mapping found for client ID: " + clientId +
                    ". Message not sent. Client might have already disconnected.");
        }
    }

    /**
     * Broadcasts a message to all currently connected clients, using their respective adapters.
     * @param message The message to broadcast.
     */
    public void broadcastMessage(Message message) {
        LOGGER.finer("ServerNetworkManager: Broadcasting message to " + clientToAdapterMap.size() + " clients: " + message.getClass().getSimpleName());
        for (Map.Entry<String, ServerNetworkInterface> entry : clientToAdapterMap.entrySet()) {
            String clientId = entry.getKey();
            ServerNetworkInterface adapter = entry.getValue();
            if (adapter.isRunning()) {
                // The adapter's sendMessageToClient will handle the specifics for that client
                adapter.sendMessageToClient(clientId, message);
            }
        }
    }

    public boolean isRunning() {
        for (ServerNetworkInterface adapter : networkAdapters) {
            if (adapter.isRunning()) {
                return true;
            }
        }
        return false;
    }
}