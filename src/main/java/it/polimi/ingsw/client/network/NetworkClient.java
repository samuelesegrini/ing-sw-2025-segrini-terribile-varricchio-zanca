package it.polimi.ingsw.client.network;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.common.message.request.Request;
import it.polimi.ingsw.common.message.response.Response;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main network client that manages connections and message handling.
 */
public class NetworkClient {
    private static final Logger LOGGER = Logger.getLogger(NetworkClient.class.getName());

    private final ClientModel model;
    private NetworkAdapter adapter;
    private Consumer<Message> messageHandler;
    private final Map<UUID, CompletableFuture<Response>> pendingRequests;
    private final ExecutorService messageExecutor;

    public NetworkClient(ClientModel model) {
        this.model = model;
        this.pendingRequests = new ConcurrentHashMap<>();
        this.messageExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("client-message-executor");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Sets the message handler for incoming messages.
     */
    public void setMessageHandler(Consumer<Message> handler) {
        this.messageHandler = handler;
    }

    /**
     * Connects to the server using the specified protocol.
     */
    public CompletableFuture<Boolean> connect(String host, int port, boolean useSocket) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        messageExecutor.submit(() -> {
            try {
                // Close existing connection if any
                if (adapter != null && adapter.isConnected()) {
                    adapter.disconnect();
                }

                // Create appropriate adapter
                if (useSocket) {
                    adapter = new SocketNetworkAdapter();
                } else {
                    adapter = new RMINetworkAdapter();
                }

                // Set message callback
                LOGGER.fine("Setting message handler callback for adapter: " + adapter.getClass().getSimpleName());
                adapter.setOnMessageReceived(this::handleIncomingMessage);
                LOGGER.fine("Message handler callback set successfully");

                // Connect
                boolean connected = adapter.connect(host, port);
                future.complete(connected);

                if (connected) {
                    LOGGER.info("Connected to server at " + host + ":" + port);
                } else {
                    LOGGER.warning("Failed to connect to server");
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Connection error", e);
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Disconnects from the server.
     */
    public void disconnect() {
        if (adapter != null) {
            adapter.disconnect();
            adapter = null;
        }

        // Cancel all pending requests
        pendingRequests.values().forEach(future ->
                future.completeExceptionally(new IllegalStateException("Disconnected"))
        );
        pendingRequests.clear();
    }

    /**
     * Sends a request and waits for a response.
     */
    public CompletableFuture<Response> sendRequest(Request request) {
        if (adapter == null || !adapter.isConnected()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Not connected to server")
            );
        }

        LOGGER.info("Sending request: " + request.getClass().getSimpleName() + 
                   " with correlation ID: " + request.getCorrelationId());

        CompletableFuture<Response> future = new CompletableFuture<>();
        pendingRequests.put(request.getCorrelationId(), future);

        LOGGER.fine("Added request to pending requests. Total pending: " + pendingRequests.size());

        // Set timeout
        future.orTimeout(30, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    if (ex instanceof TimeoutException) {
                        LOGGER.warning("Request timed out: " + request.getClass().getSimpleName() + 
                                     " with correlation ID: " + request.getCorrelationId());
                        pendingRequests.remove(request.getCorrelationId());
                    }
                    throw new CompletionException(ex);
                });

        // Send request
        boolean sent = adapter.sendMessage(request);
        if (!sent) {
            LOGGER.warning("Failed to send request: " + request.getClass().getSimpleName());
            pendingRequests.remove(request.getCorrelationId());
            future.completeExceptionally(new IllegalStateException("Failed to send request"));
        } else {
            LOGGER.info("Successfully sent request: " + request.getClass().getSimpleName());
        }

        return future;
    }

    /**
     * Sends a message to the server.
     */
    public boolean sendMessage(Message message) {
        if (adapter == null || !adapter.isConnected()) {
            LOGGER.warning("Cannot send message: not connected");
            return false;
        }
        return adapter.sendMessage(message);
    }

    /**
     * Handles incoming messages from the server.
     */
    private void handleIncomingMessage(Message message) {
        LOGGER.fine("NetworkClient.handleIncomingMessage called with: " + message.getClass().getSimpleName());
        
        // Handle responses immediately on current thread to avoid deadlocks
        if (message instanceof Response response) {
            LOGGER.fine("Processing response immediately on current thread: " + response.getCorrelationId());
            LOGGER.finer("Pending requests count: " + pendingRequests.size());
            LOGGER.finer("Pending request IDs: " + pendingRequests.keySet());
            
            CompletableFuture<Response> future = pendingRequests.remove(response.getCorrelationId());
            if (future != null) {
                LOGGER.fine("Found matching future for response, completing it immediately");
                future.complete(response);
            } else {
                LOGGER.warning("No matching future found for response correlation ID: " + response.getCorrelationId());
                LOGGER.warning("Response type: " + response.getClass().getSimpleName());
            }
        }
        
        // Forward other messages to message handler via executor to avoid blocking
        if (messageHandler != null) {
            if (message instanceof Response) {
                // For responses, also forward to handler but don't wait
                LOGGER.fine("Forwarding response to messageHandler on executor thread");
                messageExecutor.submit(() -> {
                    try {
                        messageHandler.accept(message);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error in messageHandler for response", e);
                    }
                });
            } else {
                // For non-responses, use executor
                LOGGER.fine("Forwarding non-response to messageHandler on executor thread: " + message.getClass().getSimpleName());
                messageExecutor.submit(() -> {
                    try {
                        messageHandler.accept(message);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error in messageHandler for " + message.getClass().getSimpleName(), e);
                    }
                });
            }
        } else {
            LOGGER.warning("No messageHandler set for: " + message.getClass().getSimpleName());
        }
    }
    
    private void processMessageDirectly(Message message) {
        try {
            LOGGER.fine("Direct processing: " + message.getClass().getSimpleName());
            
            if (message instanceof Response response) {
                LOGGER.fine("Direct processing response with correlation ID: " + response.getCorrelationId());
                CompletableFuture<Response> future = pendingRequests.remove(response.getCorrelationId());
                if (future != null) {
                    LOGGER.fine("Direct completion of future for response");
                    future.complete(response);
                } else {
                    LOGGER.warning("Direct processing: No matching future found for correlation ID: " + response.getCorrelationId());
                }
            }
            
            if (messageHandler != null) {
                messageHandler.accept(message);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in direct message processing", e);
        }
    }

    /**
     * Shuts down the network client.
     */
    public void shutdown() {
        disconnect();
        messageExecutor.shutdown();
        try {
            if (!messageExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                messageExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            messageExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}