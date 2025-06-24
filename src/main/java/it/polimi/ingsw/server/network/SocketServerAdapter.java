package it.polimi.ingsw.server.network;

import it.polimi.ingsw.common.message.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocketServerAdapter implements ServerNetworkInterface {
    private static final Logger LOGGER = Logger.getLogger(SocketServerAdapter.class.getName());

    private final int port;
    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService clientHandlingExecutor; // To handle each client connection
    private final ExecutorService serverAcceptExecutor;   // Single thread for serverSocket.accept()

    // Map of connected clients: clientId -> ClientHandler
    private final Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();

    // Callbacks
    private Consumer<String> onClientConnectedHandler = clientId -> {};
    private Consumer<String> onClientDisconnectedHandler = clientId -> {};
    private BiConsumer<String, Message> onMessageReceivedHandler = (clientId, msg) -> {};

    public SocketServerAdapter() {
        this(-1); // Default constructor indicating port needs to be set by startServer
    }

    public SocketServerAdapter(int port) {
        this.port = port;
        this.clientHandlingExecutor = Executors.newCachedThreadPool(
                r -> {
                    Thread t = new Thread(r);
                    t.setName("socket-client-handler-" + t.getId());
                    t.setDaemon(true);
                    return t;
                }
        );
        this.serverAcceptExecutor = Executors.newSingleThreadExecutor(
                r -> {
                    Thread t = new Thread(r);
                    t.setName("socket-server-acceptor");
                    t.setDaemon(true);
                    return t;
                }
        );
    }


    @Override
    public void startServer(int portToListenOn) throws IOException {
        if (running.get()) {
            LOGGER.warning("Server is already running on port " + (serverSocket != null ? serverSocket.getLocalPort() : "unknown"));
            return;
        }
        try {
            serverSocket = new ServerSocket(portToListenOn);
            running.set(true);
            LOGGER.info("SocketServerAdapter started, listening on port: " + portToListenOn);

            serverAcceptExecutor.submit(() -> {
                while (running.get() && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept(); // Blocking call
                        String clientId = "socket-" + UUID.randomUUID().toString();
                        LOGGER.info("Client connected: " + clientSocket.getInetAddress() + " assigned ID: " + clientId);
                        ClientHandler clientHandler = new ClientHandler(clientSocket, clientId);
                        activeClients.put(clientId, clientHandler);
                        clientHandlingExecutor.submit(clientHandler);
                        onClientConnectedHandler.accept(clientId);
                    } catch (SocketException se) {
                        if (!running.get() || serverSocket.isClosed()) {
                            LOGGER.info("ServerSocket closed, accept loop terminating.");
                        } else {
                            LOGGER.log(Level.SEVERE, "SocketException in accept loop (server might be shutting down or error occurred): " + se.getMessage(), se);
                        }
                    } catch (IOException e) {
                        if (running.get()) { // Only log if we weren't expecting to stop
                            LOGGER.log(Level.SEVERE, "IOException in accept loop: " + e.getMessage(), e);
                        }
                    }
                }
                LOGGER.info("Server accept loop finished.");
            });
        } catch (IOException e) {
            running.set(false);
            LOGGER.log(Level.SEVERE, "Could not start server on port " + portToListenOn, e);
            throw e;
        }
    }

    @Override
    public void stopServer() {
        if (!running.compareAndSet(true, false)) {
            LOGGER.info("Server is not running or already stopping.");
            return;
        }
        LOGGER.info("Stopping SocketServerAdapter...");
        try {
            // Close client connections
            activeClients.values().forEach(ClientHandler::disconnect);
            activeClients.clear();

            // Shutdown executors
            clientHandlingExecutor.shutdown();
            serverAcceptExecutor.shutdown();


            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            // Await termination
            try {
                if (!clientHandlingExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    clientHandlingExecutor.shutdownNow();
                }
                if (!serverAcceptExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                    serverAcceptExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                clientHandlingExecutor.shutdownNow();
                serverAcceptExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while stopping server: " + e.getMessage(), e);
        }
        LOGGER.info("SocketServerAdapter stopped.");
    }

    @Override
    public boolean sendMessageToClient(String clientId, Message message) {
        ClientHandler handler = activeClients.get(clientId);
        if (handler != null) {
            LOGGER.fine("Found client handler for " + clientId + ", delegating send");
            boolean result = handler.sendMessage(message);
            LOGGER.fine("Client handler send result: " + result + " for client " + clientId);
            return result;
        } else {
            LOGGER.warning("No client handler found for client ID: " + clientId + ". Cannot send message.");
            return false;
        }
    }

    @Override
    public void broadcastMessage(Message message) {
        LOGGER.finer("Broadcasting message: " + message.getClass().getSimpleName());
        activeClients.values().forEach(handler -> handler.sendMessage(message));
    }

    @Override
    public void setOnClientConnected(Consumer<String> onClientConnectedHandler) {
        this.onClientConnectedHandler = Objects.requireNonNull(onClientConnectedHandler);
    }

    @Override
    public void setOnClientDisconnected(Consumer<String> onClientDisconnectedHandler) {
        this.onClientDisconnectedHandler = Objects.requireNonNull(onClientDisconnectedHandler);
    }

    @Override
    public void setOnMessageReceived(BiConsumer<String, Message> onMessageReceivedHandler) {
        this.onMessageReceivedHandler = Objects.requireNonNull(onMessageReceivedHandler);
    }

    @Override
    public boolean isRunning() {
        return running.get() && serverSocket != null && !serverSocket.isClosed();
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final String clientId;
        private ObjectOutputStream oos;
        private ObjectInputStream ois;
        private final AtomicBoolean connected = new AtomicBoolean(false);

        ClientHandler(Socket socket, String clientId) {
            this.clientSocket = socket;
            this.clientId = clientId;
            try {
                this.oos = new ObjectOutputStream(socket.getOutputStream());
                this.oos.flush(); // Flush header to prevent deadlock with client's OIS creation
                this.ois = new ObjectInputStream(socket.getInputStream());
                this.connected.set(true);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error initializing streams for client " + clientId, e);
                disconnect();
            }
        }

        @Override
        public void run() {
            if (!connected.get()) return;

            try {
                while (connected.get() && !clientSocket.isClosed() && clientSocket.isConnected()) {
                    try {
                        Object receivedObject = ois.readObject();
                        if (receivedObject instanceof Message) {
                            onMessageReceivedHandler.accept(clientId, (Message) receivedObject);
                        } else {
                            LOGGER.warning("Received non-Message object from client " + clientId + ": " + receivedObject);
                        }
                    } catch (ClassNotFoundException e) {
                        LOGGER.log(Level.SEVERE, "Could not deserialize message from client " + clientId + ". Class not found.", e);
                        disconnect();
                    } catch (EOFException | SocketException e) {
                        LOGGER.info("Client " + clientId + " disconnected (" + e.getMessage() + ").");
                        disconnect();
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "IOException reading from client " + clientId + ": " + e.getMessage(), e);
                        disconnect(); // Assume connection lost
                    }
                }
            } finally {
                // Ensure disconnect is called if loop exits for any reason
                if (connected.get()) { // if not already disconnected by an exception
                    disconnect();
                }
            }
        }

        public boolean sendMessage(Message message) {
            if (!connected.get() || clientSocket.isClosed() || oos == null) {
                LOGGER.warning("Cannot send message, client " + clientId + " is not connected or OOS is null. " +
                              "Connected: " + connected.get() + ", Socket closed: " + clientSocket.isClosed() + 
                              ", OOS null: " + (oos == null));
                return false;
            }
            
            LOGGER.fine("Client " + clientId + " appears connected, attempting to write message to stream");
            try {
                synchronized (oos) {
                    oos.writeObject(message);
                    oos.flush();
                }
                LOGGER.fine("Successfully sent " + message.getClass().getSimpleName() + " to client " + clientId);
                return true;
            } catch (SocketException se) {
                LOGGER.log(Level.WARNING, "SocketException sending message to " + clientId + " (client likely disconnected): " + se.getMessage());
                disconnect(); // Client is gone
                return false;
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "IOException sending message to client " + clientId, e);
                return false;
            }
        }

        public void disconnect() {
            if (connected.compareAndSet(true, false)) {
                LOGGER.info("Disconnecting client: " + clientId);
                activeClients.remove(clientId); // Remove from active list
                try {
                    if (ois != null) ois.close();
                } catch (IOException e) { LOGGER.finer("Error closing OIS for " + clientId + ": " + e.getMessage()); }
                try {
                    if (oos != null) oos.close();
                } catch (IOException e) { LOGGER.finer("Error closing OOS for " + clientId + ": " + e.getMessage()); }
                try {
                    if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
                } catch (IOException e) { LOGGER.finer("Error closing client socket for " + clientId + ": " + e.getMessage()); }
                onClientDisconnectedHandler.accept(clientId); // Notify manager
            }
        }
    }
}