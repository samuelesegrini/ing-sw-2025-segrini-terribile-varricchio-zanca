package it.polimi.ingsw.client.network;

import it.polimi.ingsw.common.message.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocketClientAdapter implements ClientNetworkInterface {
    private static final Logger LOGGER = Logger.getLogger(SocketClientAdapter.class.getName());

    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final ExecutorService listenerExecutor; // For the message listening thread

    // Callbacks
    private Consumer<Message> onMessageReceivedHandler = msg -> {};
    private Runnable onDisconnectedHandler = () -> {};

    public SocketClientAdapter() {
        this.listenerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("socket-client-listener");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void connect(String host, int port) throws IOException {
        if (connected.get()) {
            LOGGER.warning("Already connected. Disconnect first.");
            return;
        }
        try {
            socket = new Socket(host, port);
            // IMPORTANT: Order of OOS/OIS creation.
            // Client creates OIS first, then OOS. (Opposite of typical server)
            // Or ensure server flushes OOS header first.
            // Here, assuming server OOS is flushed first or client creates OIS first.
            // Let's follow: Client OIS then OOS
            ois = new ObjectInputStream(socket.getInputStream());
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush(); // Flush header important for server's OIS creation

            connected.set(true);
            LOGGER.info("Successfully connected to server: " + host + ":" + port);

            // Start the listener thread
            listenerExecutor.submit(this::listenForMessages);
            // No onConnected callback here, but could be added if manager needs it

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to server: " + host + ":" + port, e);
            disconnect(); // Ensure resources are cleaned up on failed connect
            throw e;
        }
    }

    private void listenForMessages() {
        if (!connected.get() || ois == null) return;

        try {
            while (connected.get() && !socket.isClosed() && socket.isConnected()) {
                try {
                    Object receivedObject = ois.readObject(); // Blocking call
                    if (receivedObject instanceof Message) {
                        onMessageReceivedHandler.accept((Message) receivedObject);
                    } else {
                        LOGGER.warning("Received non-Message object from server: " + receivedObject);
                    }
                } catch (ClassNotFoundException e) {
                    LOGGER.log(Level.SEVERE, "Could not deserialize message from server. Class not found.", e);
                    // Consider this a fatal error for the connection
                    disconnect();
                } catch (EOFException | SocketException e) {
                    if (connected.get()) { // Only log if we weren't expecting to disconnect
                        LOGGER.info("Disconnected from server: " + e.getMessage());
                    }
                    disconnect(); // Connection lost
                } catch (IOException e) {
                    if (connected.get()) {
                        LOGGER.log(Level.WARNING, "IOException reading from server: " + e.getMessage(), e);
                    }
                    disconnect(); // Assume connection lost
                }
            }
        } finally {
            // If loop exits for any reason and still "connected", ensure proper disconnect
            if (connected.get()) {
                disconnect();
            }
            LOGGER.info("Client message listener thread finished.");
        }
    }

    @Override
    public void disconnect() {
        if (connected.compareAndSet(true, false)) {
            LOGGER.info("Disconnecting from server...");
            try {
                // Shutdown listener executor first to stop processing new messages
                listenerExecutor.shutdown();
                try {
                    if (!listenerExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                        listenerExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    listenerExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }


                if (ois != null) ois.close();
            } catch (IOException e) { LOGGER.finer("Error closing OIS: " + e.getMessage()); }
            try {
                if (oos != null) oos.close();
            } catch (IOException e) { LOGGER.finer("Error closing OOS: " + e.getMessage()); }
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) { LOGGER.finer("Error closing client socket: " + e.getMessage()); }

            onDisconnectedHandler.run(); // Notify manager
            LOGGER.info("Disconnected.");
        }
    }

    @Override
    public boolean sendMessage(Message message) {
        if (!connected.get() || socket == null || socket.isClosed() || oos == null) {
            LOGGER.warning("Not connected to server or OOS is null. Cannot send message: " + message.getClass().getSimpleName());
            return false;
        }
        try {
            synchronized (oos) { // Synchronize if sendMessage can be called from multiple threads
                oos.writeObject(message);
                oos.flush();
                oos.reset(); // Good practice after sending objects that might be resent with changes
            }
            return true;
        } catch (SocketException se) {
            LOGGER.log(Level.WARNING, "SocketException sending message (server likely disconnected): " + se.getMessage());
            disconnect(); // Server is gone
            return false;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException sending message: " + message.getClass().getSimpleName(), e);
            // Depending on the error, might need to disconnect
            return false;
        }
    }

    @Override
    public void setOnMessageReceived(Consumer<Message> onMessageReceivedHandler) {
        this.onMessageReceivedHandler = Objects.requireNonNull(onMessageReceivedHandler);
    }

    @Override
    public void setOnDisconnected(Runnable onDisconnectedHandler) {
        this.onDisconnectedHandler = Objects.requireNonNull(onDisconnectedHandler);
    }

    @Override
    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed() && socket.isConnected();
    }
}