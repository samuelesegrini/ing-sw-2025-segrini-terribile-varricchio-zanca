package it.polimi.ingsw.client.network;

import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.message.Message;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the client-side network operations, using a specific
 * ClientNetworkInterface implementation (e.g., SocketClientAdapter).
 * It bridges between the raw network events and the client's EventBus.
 */
public class ClientNetworkManager {
    private static final Logger LOGGER = Logger.getLogger(ClientNetworkManager.class.getName());

    private final ClientNetworkInterface networkAdapter;
    private final EventBus clientEventBus;

    /**
     * Event posted to the EventBus when the client disconnects from the server.
     */
    public static class ServerDisconnectedSystemEvent implements Message {
        private static final long serialVersionUID = 1L;
        // Optionally, include a reason for disconnection if known
        public ServerDisconnectedSystemEvent() {}
    }


    public ClientNetworkManager(ClientNetworkInterface networkAdapter, EventBus clientEventBus) {
        this.networkAdapter = Objects.requireNonNull(networkAdapter, "Network adapter cannot be null");
        this.clientEventBus = Objects.requireNonNull(clientEventBus, "Client EventBus cannot be null");
        setupAdapterCallbacks();
    }

    private void setupAdapterCallbacks() {
        networkAdapter.setOnMessageReceived(message -> {
            LOGGER.finer("NetworkManager: Message received from server: " + message.getClass().getSimpleName());
            clientEventBus.post(message);
        });

        networkAdapter.setOnDisconnected(() -> {
            LOGGER.info("NetworkManager: Disconnected from server callback triggered.");
            clientEventBus.post(new ServerDisconnectedSystemEvent());
        });
    }

    public void connect(String host, int port) throws IOException {
        if (networkAdapter.isConnected()) {
            LOGGER.warning("Already connected. Disconnecting first to attempt new connection.");
            networkAdapter.disconnect(); // Ensure clean state before new attempt
        }
        LOGGER.info("ClientNetworkManager attempting to connect to " + host + ":" + port);
        networkAdapter.connect(host, port);
        // Connection success/failure is primarily determined by connect() throwing an exception
        // or isConnected() state. The ServerConnectedEvent idea might be redundant if
        // connect() is blocking and followed by an isConnected() check.
    }

    public void disconnect() {
        LOGGER.info("ClientNetworkManager disconnecting from server.");
        networkAdapter.disconnect(); // Adapter's onDisconnected will trigger our callback, posting ServerDisconnectedSystemEvent
    }

    public boolean sendMessage(Message message) {
        if (!networkAdapter.isConnected()) {
            LOGGER.warning("Cannot send message; not connected to server.");
            return false;
        }
        LOGGER.finer("NetworkManager: Sending message to server: " + message.getClass().getSimpleName());
        return networkAdapter.sendMessage(message);
    }

    public boolean isConnected() {
        return networkAdapter.isConnected();
    }
}