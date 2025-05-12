package it.polimi.ingsw.client.network;

import it.polimi.ingsw.common.message.Message;

import java.io.IOException;
import java.util.function.Consumer; // For Message

/**
 * Interface for the client-side network layer, abstracting the underlying
 * communication technology (e.g., Sockets, RMI) used to connect to the server.
 */
public interface ClientNetworkInterface {

    /**
     * Attempts to connect to the server at the specified host and port.
     * @param host The server's hostname or IP address.
     * @param port The server's port number.
     * @throws IOException If an I/O error occurs during connection.
     */
    void connect(String host, int port) throws IOException;

    /**
     * Disconnects from the server and releases any associated resources.
     */
    void disconnect();

    /**
     * Sends a message to the server.
     * @param message The message object to send.
     * @return true if the message was successfully queued or sent, false if not connected or an error occurred.
     */
    boolean sendMessage(Message message);

    /**
     * Sets a handler to be called when a message is received from the server.
     * The handler will receive the deserialized Message object.
     * @param onMessageReceivedHandler Consumer that accepts the Message.
     */
    void setOnMessageReceived(Consumer<Message> onMessageReceivedHandler);

    /**
     * Sets a handler to be called when the client is disconnected from the server
     * (either intentionally or due to an error).
     * @param onDisconnectedHandler Runnable to execute upon disconnection.
     */
    void setOnDisconnected(Runnable onDisconnectedHandler);

    /**
     * Checks if the client is currently connected to the server.
     * @return true if connected, false otherwise.
     */
    boolean isConnected();
}