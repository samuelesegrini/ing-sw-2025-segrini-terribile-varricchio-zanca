package it.polimi.ingsw.client.network;

import it.polimi.ingsw.common.message.Message;

import java.io.IOException;
import java.util.function.Consumer; // For Message

/**
 * Base interface for network adapters (Socket and RMI).
 */
public interface NetworkAdapter {
    String DEFAULT_HOST = "localhost";
    int DEFAULT_SOCKET_PORT = 12345;
    int DEFAULT_RMI_PORT = 1099;

    boolean connect(String host, int port);
    void disconnect();
    boolean isConnected();
    boolean sendMessage(Message message);
    void setOnMessageReceived(Consumer<Message> handler);
}
