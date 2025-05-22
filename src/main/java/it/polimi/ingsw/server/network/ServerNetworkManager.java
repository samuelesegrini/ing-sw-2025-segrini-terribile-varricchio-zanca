package it.polimi.ingsw.server.network;

import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.message.Command;
import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.common.message.system.ErrorMessage;
import it.polimi.ingsw.server.controller.CommandDispatcher;

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
 * Manages server-side network adapters (e.g., Socket, RMI), and routes incoming
 * client commands to a CommandDispatcher. It also provides methods for sending
 * messages to clients.
 */
public class ServerNetworkManager {
    private static final Logger LOGGER = Logger.getLogger(ServerNetworkManager.class.getName());

    private final List<ServerNetworkInterface> networkAdapters = new ArrayList<>();
    private final EventBus serverEventBus;
    private CommandDispatcher commandDispatcher;

    // Maps a networkClientId to the specific adapter that handles it.
    private final Map<String, ServerNetworkInterface> clientToAdapterMap = new ConcurrentHashMap<>();

    private Consumer<String> globalOnClientConnectedHandler = clientId -> {
        LOGGER.finer("Default onClientConnected called for: " + clientId);
    };
    private Consumer<String> globalOnClientDisconnectedHandler = clientId -> {
        LOGGER.finer("Default onClientDisconnected called for: " + clientId);
    };

    /**
     * Constructs a ServerNetworkManager.
     * @param serverEventBus The main server event bus, potentially for system-level or non-command messages.
     */
    public ServerNetworkManager(EventBus serverEventBus) {
        this.serverEventBus = Objects.requireNonNull(serverEventBus, "Server EventBus cannot be null");
    }

    /**
     * Sets the CommandDispatcher that will handle incoming client commands.
     * This should be called after CommandDispatcher is initialized.
     * @param commandDispatcher The CommandDispatcher instance.
     */
    public void setCommandDispatcher(CommandDispatcher commandDispatcher) {
        this.commandDispatcher = Objects.requireNonNull(commandDispatcher, "CommandDispatcher cannot be null");
        LOGGER.info("CommandDispatcher has been set for ServerNetworkManager.");
    }

    /**
     * Adds a network adapter (e.g., SocketServerAdapter, RMIServerAdapter) to be managed.
     * @param adapter The network adapter instance.
     */
    public void addNetworkAdapter(ServerNetworkInterface adapter) {
        Objects.requireNonNull(adapter, "Network adapter cannot be null");
        this.networkAdapters.add(adapter);
        setupAdapterCallbacks(adapter); // Configure how this adapter interacts with SNM
        LOGGER.info("Added network adapter: " + adapter.getClass().getSimpleName());
    }

    /**
     * Sets up the necessary callbacks for a given network adapter.
     * This tells the adapter what to do when it connects/disconnects a client
     * or receives a message.
     * @param specificAdapter The adapter to configure.
     */
    private void setupAdapterCallbacks(ServerNetworkInterface specificAdapter) {
        // When an adapter connects a client:
        specificAdapter.setOnClientConnected(networkClientId -> {
            LOGGER.info("Client connected via " + specificAdapter.getClass().getSimpleName() + ". Assigned Network ID: " + networkClientId);
            clientToAdapterMap.put(networkClientId, specificAdapter);
            if (globalOnClientConnectedHandler != null) {
                globalOnClientConnectedHandler.accept(networkClientId);
            }
        });

        specificAdapter.setOnClientDisconnected(networkClientId -> {
            LOGGER.info("Client disconnected via " + specificAdapter.getClass().getSimpleName() + ". Network ID: " + networkClientId);
            clientToAdapterMap.remove(networkClientId);
            if (globalOnClientDisconnectedHandler != null) {
                globalOnClientDisconnectedHandler.accept(networkClientId);
            }
        });

        // When an adapter receives a message:
        specificAdapter.setOnMessageReceived((networkClientId, message) -> {
            LOGGER.finer("Message received by ServerNetworkManager via " +
                    specificAdapter.getClass().getSimpleName() + " from " + networkClientId +
                    ": " + message.getClass().getSimpleName());

            if (message instanceof Command) {
                if (this.commandDispatcher != null) {
                    this.commandDispatcher.dispatch((Command) message, networkClientId);
                } else {
                    LOGGER.severe("CommandDispatcher is not set in ServerNetworkManager. " +
                            "Cannot dispatch command: " + message.getClass().getSimpleName() + " from " + networkClientId);
                    specificAdapter.sendMessageToClient(networkClientId,
                            new ErrorMessage("Server internal configuration error: Command dispatcher not available.",
                                    ErrorMessage.ErrorType.SERVER_INTERNAL));
                }
            } else {
                LOGGER.warning("Received non-Command message from client " + networkClientId +
                        ": " + message.getClass().getSimpleName() + ". Current policy is to ignore or log.");
            }
        });
    }

    /**
     * Starts all configured network adapters.
     * @param socketPort The port for Socket-based adapters.
     * @param rmiPort The port for RMI-based adapters.
     * @throws IOException if any adapter fails to start.
     */
    public void startAdapters(int socketPort, int rmiPort) throws IOException {
        if (networkAdapters.isEmpty()){
            LOGGER.warning("ServerNetworkManager: No network adapters configured to start.");
            return;
        }
        boolean startedAtLeastOne = false;
        List<String> errors = new ArrayList<>();
        for (ServerNetworkInterface adapter : networkAdapters) {
            try {
                if (adapter instanceof SocketServerAdapter) {
                    LOGGER.info("Starting Socket adapter on port " + socketPort);
                    adapter.startServer(socketPort);
                    startedAtLeastOne = true;
                } else if (adapter instanceof RMIServerAdapter) {
                    LOGGER.info("Starting RMI adapter on port " + rmiPort);
                    adapter.startServer(rmiPort);
                    startedAtLeastOne = true;
                } else {
                    LOGGER.warning("Unknown adapter type, cannot determine port: " + adapter.getClass().getSimpleName());
                }
            } catch (IOException e) {
                String errorMsg = "Failed to start adapter " + adapter.getClass().getSimpleName() + ": " + e.getMessage();
                LOGGER.log(Level.SEVERE, errorMsg, e);
                errors.add(errorMsg);
            }
        }
        if (!startedAtLeastOne && !networkAdapters.isEmpty()) {
            throw new IOException("No network adapters were successfully started. Errors: " + String.join("; ", errors));
        }
        if (!errors.isEmpty()) {
            LOGGER.warning("Some network adapters failed to start: " + String.join("; ", errors));
        }
    }

    /**
     * Sets the global handler to be called when any client connects through any adapter.
     * @param handler The consumer for the networkClientId.
     */
    public void setGlobalOnClientConnected(Consumer<String> handler) {
        this.globalOnClientConnectedHandler = Objects.requireNonNull(handler);
    }

    /**
     * Sets the global handler to be called when any client disconnects from any adapter.
     * @param handler The consumer for the networkClientId.
     */
    public void setGlobalOnClientDisconnected(Consumer<String> handler) {
        this.globalOnClientDisconnectedHandler = Objects.requireNonNull(handler);
    }

    /**
     * Stops all managed network adapters and clears internal mappings.
     */
    public void stop() {
        LOGGER.info("ServerNetworkManager stopping all network adapters...");
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
        clientToAdapterMap.clear();
        LOGGER.info("All network adapters stopped and resources cleared.");
    }

    /**
     * Sends a message to a specific client.
     * It uses the adapter that originally handled the client's connection.
     * @param networkClientId The unique ID of the target client.
     * @param message The message to send.
     * @return true if the message was successfully queued/sent by an adapter, false otherwise.
     */
    public boolean sendMessageToClient(String networkClientId, Message message) {
        Objects.requireNonNull(networkClientId, "networkClientId cannot be null for sendMessageToClient");
        Objects.requireNonNull(message, "message cannot be null for sendMessageToClient");

        ServerNetworkInterface adapter = clientToAdapterMap.get(networkClientId);

        if (adapter != null) {
            if (adapter.isRunning()) {
                LOGGER.finer("Sending message to " + networkClientId + " (" + message.getClass().getSimpleName() + ") via " + adapter.getClass().getSimpleName());
                return adapter.sendMessageToClient(networkClientId, message);
            } else {
                LOGGER.warning("Adapter for client " + networkClientId + " (" + adapter.getClass().getSimpleName() + ") is not running. Message not sent.");
            }
        } else {
            LOGGER.warning("No adapter mapping found for client ID: " + networkClientId + ". Message not sent. Client might have disconnected.");
        }
        return false;
    }

    /**
     * Broadcasts a message to all currently connected clients, using their respective adapters.
     * @param message The message to broadcast.
     */
    public void broadcastMessageToAllClients(Message message) {
        Objects.requireNonNull(message, "message cannot be null for broadcastMessageToAllClients");
        LOGGER.finer("Broadcasting message to " + clientToAdapterMap.size() + " clients: " + message.getClass().getSimpleName());
        if (clientToAdapterMap.isEmpty()) {
            LOGGER.info("No clients connected to broadcast message to.");
            return;
        }
        for (String clientId : List.copyOf(clientToAdapterMap.keySet())) {
            sendMessageToClient(clientId, message);
        }
    }

    /**
     * Broadcasts a message to all clients connected to a specific game session.
     * Requires external mapping of session to clients.
     * @param message The message to send.
     * @param targetClientIds A list of networkClientIds who are in the target session.
     */
    public void broadcastMessageToSessionClients(Message message, List<String> targetClientIds) {
        Objects.requireNonNull(message, "message cannot be null for broadcastMessageToSessionClients");
        Objects.requireNonNull(targetClientIds, "targetClientIds cannot be null");
        if (targetClientIds.isEmpty()) return;

        LOGGER.finer("Broadcasting message to " + targetClientIds.size() + " clients in a session: " + message.getClass().getSimpleName());
        for (String clientId : targetClientIds) {
            sendMessageToClient(clientId, message);
        }
    }

    /**
     * Checks if the ServerNetworkManager has any adapter currently running.
     * @return true if at least one adapter is running, false otherwise.
     */
    public boolean isRunning() {
        for (ServerNetworkInterface adapter : networkAdapters) {
            if (adapter.isRunning()) {
                return true;
            }
        }
        return false;
    }
}