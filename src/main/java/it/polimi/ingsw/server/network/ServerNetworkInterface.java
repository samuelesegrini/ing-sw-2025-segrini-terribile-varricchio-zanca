package it.polimi.ingsw.server.network;

import it.polimi.ingsw.common.message.Message;

import java.io.IOException;
import java.util.function.BiConsumer; // For (clientId, message)
import java.util.function.Consumer;   // For (clientId)

/**
 * Interface for the server-side network layer, abstracting the underlying
 * communication technology (e.g., Sockets, RMI).
 */
public interface ServerNetworkInterface {

    /**
     * Starts the server and begins listening for incoming connections on the specified port.
     * @param port The port number to listen on.
     * @throws IOException If an I/O error occurs when opening the socket or starting the service.
     */
    void startServer(int port) throws IOException;

    /**
     * Stops the server, closes all connections, and releases resources.
     */
    void stopServer();

    /**
     * Sends a message to a specific connected client.
     * @param clientId The unique identifier of the target client.
     * @param message The message to send.
     * @return true if the message was successfully queued or sent, false otherwise (e.g., client not found).
     */
    boolean sendMessageToClient(String clientId, Message message);

    /**
     * Broadcasts a message to all currently connected clients.
     * @param message The message to broadcast.
     */
    void broadcastMessage(Message message);

    /**
     * Sets a handler to be called when a new client connects.
     * The handler will receive the unique ID assigned to the connected client.
     * @param onClientConnectedHandler Consumer that accepts the clientId.
     */
    void setOnClientConnected(Consumer<String> onClientConnectedHandler);

    /**
     * Sets a handler to be called when a client disconnects.
     * The handler will receive the ID of the disconnected client.
     * @param onClientDisconnectedHandler Consumer that accepts the clientId.
     */
    void setOnClientDisconnected(Consumer<String> onClientDisconnectedHandler);

    /**
     * Sets a handler to be called when a message is received from any client.
     * The handler will receive the client's ID and the deserialized Message object.
     * @param onMessageReceivedHandler BiConsumer that accepts clientId and the Message.
     */
    void setOnMessageReceived(BiConsumer<String, Message> onMessageReceivedHandler);

    /**
     * Checks if the server is currently running and listening.
     * @return true if the server is running, false otherwise.
     */
    boolean isRunning();
}