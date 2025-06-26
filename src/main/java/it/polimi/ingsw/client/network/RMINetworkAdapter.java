package it.polimi.ingsw.client.network;

import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.common.message.PingMessage;
import it.polimi.ingsw.common.message.PongMessage;
import it.polimi.ingsw.common.network.rmi.IClientRemoteListener;
import it.polimi.ingsw.common.network.rmi.IServerRemote;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RMI-based network adapter implementation.
 */
public class RMINetworkAdapter implements NetworkAdapter {
    private static final Logger LOGGER = Logger.getLogger(RMINetworkAdapter.class.getName());

    private IServerRemote server;
    private String sessionToken;
    private Consumer<Message> messageHandler;
    private ClientRemoteListenerImpl clientListener;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    @Override
    public boolean connect(String host, int port) {
        try {
            // Set RMI system properties for proper callback communication
            System.setProperty("java.rmi.server.hostname", "localhost");
            System.setProperty("java.net.useSystemProxies", "false");
            
            // Get RMI registry
            Registry registry = LocateRegistry.getRegistry(host, port);

            // Look up server
            server = (IServerRemote) registry.lookup(IServerRemote.SERVICE_NAME);

            // Create and export client listener using default port (0 = system assigned)
            clientListener = new ClientRemoteListenerImpl();
            IClientRemoteListener stub = (IClientRemoteListener) UnicastRemoteObject.exportObject(clientListener, 0);
            LOGGER.info("🔗 RMI client listener exported");
            
            LOGGER.info("🔗 RMI client listener exported, registering with server...");
            // Register with server
            sessionToken = server.registerClient(stub);
            LOGGER.info("🎯 RMI client registered with session token: " + sessionToken);

            // Set the message handler on the client listener if we have one
            if (messageHandler != null) {
                clientListener.setMessageHandler(messageHandler);
                LOGGER.info("✅ RMI CLIENT: Set message handler on client listener after connection");
            }

            connected.set(true);
            LOGGER.info("RMI connected to " + host + ":" + port);
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to connect via RMI", e);
            cleanup();
            return false;
        }
    }

    @Override
    public void disconnect() {
        connected.set(false);

        if (server != null && sessionToken != null) {
            try {
                server.unregisterClient(sessionToken);
            } catch (RemoteException e) {
                LOGGER.log(Level.FINE, "Error unregistering from server", e);
            }
        }

        cleanup();
    }

    @Override
    public boolean isConnected() {
        if (!connected.get()) {
            return false;
        }

        // Test connection
        try {
            server.pingServer();
            return true;
        } catch (RemoteException e) {
            connected.set(false);
            return false;
        }
    }

    @Override
    public boolean sendMessage(Message message) {
        if (!isConnected()) {
            return false;
        }

        try {
            server.dispatchClientCommand(sessionToken, message);
            LOGGER.fine("Sent message via RMI: " + message.getClass().getSimpleName());
            return true;

        } catch (RemoteException e) {
            LOGGER.log(Level.WARNING, "Failed to send message via RMI", e);
            disconnect();
            return false;
        }
    }

    @Override
    public void setOnMessageReceived(Consumer<Message> handler) {
        LOGGER.info("📩 RMI CLIENT: Setting message handler");
        this.messageHandler = handler;
        if (clientListener != null) {
            clientListener.setMessageHandler(handler);
            LOGGER.info("✅ RMI CLIENT: Message handler set on client listener");
        } else {
            LOGGER.info("📅 RMI CLIENT: Client listener not yet created, handler will be set on connection");
        }
    }

    private void cleanup() {
        if (clientListener != null) {
            try {
                UnicastRemoteObject.unexportObject(clientListener, true);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Error unexporting client listener", e);
            }
        }

        server = null;
        sessionToken = null;
    }

    /**
     * Implementation of the RMI client listener.
     */
    private class ClientRemoteListenerImpl implements IClientRemoteListener {
        private Consumer<Message> handler;

        public void setMessageHandler(Consumer<Message> handler) {
            LOGGER.info("🎯 RMI CLIENT LISTENER: Setting message handler: " + (handler != null ? "NOT NULL" : "NULL"));
            this.handler = handler;
        }

        @Override
        public void onMessageFromServer(Message message) throws RemoteException {
            LOGGER.info("🔄 RMI CLIENT: Received message: " + message.getClass().getSimpleName());
            
            // Process all messages synchronously through the unified message handler
            if (handler != null) {
                try {
                    handler.accept(message);
                    LOGGER.info("✅ RMI CLIENT: Successfully processed message: " + message.getClass().getSimpleName());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "❌ RMI CLIENT: Failed to process message: " + message.getClass().getSimpleName(), e);
                    throw new RemoteException("Message processing failed", e);
                }
            } else {
                LOGGER.warning("⚠️ RMI CLIENT: No handler set for message: " + message.getClass().getSimpleName());
            }
        }

        @Override
        public void pingClient() throws RemoteException {
            LOGGER.info("🏓 RMI CLIENT: Received direct ping callback");
            // This method can be used by server to ping using direct RMI callback
            // The response is implicit (no exception means client is alive)
        }
    }
}