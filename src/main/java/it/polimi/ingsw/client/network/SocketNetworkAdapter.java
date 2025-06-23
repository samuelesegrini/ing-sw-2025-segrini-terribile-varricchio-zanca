package it.polimi.ingsw.client.network;

import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.common.message.PingMessage;
import it.polimi.ingsw.common.message.PongMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Socket-based network adapter implementation.
 */
public class SocketNetworkAdapter implements NetworkAdapter {
    private static final Logger LOGGER = Logger.getLogger(SocketNetworkAdapter.class.getName());

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Message> messageHandler;
    private Thread readerThread;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    @Override
    public boolean connect(String host, int port) {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000);

            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            connected.set(true);

            // Start reader thread
            readerThread = new Thread(this::readMessages);
            readerThread.setName("socket-reader");
            readerThread.setDaemon(true);
            readerThread.start();

            LOGGER.info("Socket connected to " + host + ":" + port);
            return true;

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to connect via socket", e);
            cleanup();
            return false;
        }
    }

    @Override
    public void disconnect() {
        connected.set(false);
        cleanup();
    }

    @Override
    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed();
    }

    @Override
    public boolean sendMessage(Message message) {
        if (!isConnected() || out == null) {
            return false;
        }

        try {
            synchronized (out) {
                out.writeObject(message);
                out.flush();
            }
            return true;

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to send message", e);
            disconnect();
            return false;
        }
    }

    @Override
    public void setOnMessageReceived(Consumer<Message> handler) {
        this.messageHandler = handler;
    }

    private void readMessages() {
        LOGGER.fine("SocketNetworkAdapter starting message reading loop");
        while (connected.get()) {
            try {
                LOGGER.finer("SocketNetworkAdapter waiting for next message...");
                Object obj = in.readObject();
                
                if (obj instanceof Message message) {
                    // Forward all messages to the central handler
                    if (messageHandler != null) {
                        LOGGER.finer("SocketNetworkAdapter forwarding message to handler: " + message.getClass().getSimpleName());
                        LOGGER.finer("Message handler class: " + messageHandler.getClass().getSimpleName());
                        try {
                            messageHandler.accept(message);
                            LOGGER.finer("Successfully called messageHandler.accept() for: " + message.getClass().getSimpleName());
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Exception in messageHandler.accept(): " + e.getMessage(), e);
                        }
                    } else {
                        LOGGER.warning("No message handler available for: " + message.getClass().getSimpleName());
                    }
                } else {
                    LOGGER.warning("Received non-Message object: " + obj.getClass());
                }
            } catch (EOFException | SocketException e) {
                LOGGER.info("Connection closed by server: " + e.getMessage());
                disconnect();
                break;
            } catch (IOException | ClassNotFoundException e) {
                if (connected.get()) {
                    LOGGER.log(Level.SEVERE, "Error reading message - this will break the stream: " + e.getMessage(), e);
                    disconnect();
                }
                break;
            }
        }
        LOGGER.fine("SocketNetworkAdapter message reading loop ended");
    }

    private void cleanup() {
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}

        try {
            if (out != null) out.close();
        } catch (IOException ignored) {}

        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}

        if (readerThread != null) {
            readerThread.interrupt();
        }
    }
}