package it.polimi.ingsw.server.network;

import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.common.network.rmi.IClientRemoteListener;
import it.polimi.ingsw.common.network.rmi.IServerRemote;
import it.polimi.ingsw.server.network.ServerNetworkInterface;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RMIServerAdapter extends UnicastRemoteObject implements ServerNetworkInterface, IServerRemote {
    private static final Logger LOGGER = Logger.getLogger(RMIServerAdapter.class.getName());
    private static final long serialVersionUID = 1L;

    private transient Consumer<String> onClientConnectedCallback;
    private transient Consumer<String> onClientDisconnectedCallback;
    private transient BiConsumer<String, Message> onMessageReceivedCallback;

    private final transient Map<String, IClientRemoteListener> rmiClientListeners = new ConcurrentHashMap<>();
    private transient Registry rmiRegistry;
    private int rmiPort;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public RMIServerAdapter() throws RemoteException {
        super(0);
    }

    @Override
    public void startServer(int port) throws IOException {
        if (running.get()) {
            LOGGER.warning("RMI Server Adapter is already running.");
            return;
        }
        this.rmiPort = port;
        try {
            try {
                rmiRegistry = LocateRegistry.createRegistry(rmiPort);
                LOGGER.info("RMI registry created on port: " + rmiPort);
            } catch (RemoteException e) {
                LOGGER.info("RMI registry on port " + rmiPort + " likely already exists. Attempting to get it.");
                rmiRegistry = LocateRegistry.getRegistry(rmiPort);
            }
            rmiRegistry.rebind(IServerRemote.SERVICE_NAME, this);
            running.set(true);
            LOGGER.info("RMIServerAdapter bound in RMI registry as '" + IServerRemote.SERVICE_NAME + "' on port: " + rmiPort);
        } catch (RemoteException e) {
            running.set(false);
            throw new IOException("Failed to start RMI service: " + e.getMessage(), e);
        }
    }

    @Override
    public void stopServer() {
        if (!running.compareAndSet(true, false)) return;
        LOGGER.info("Stopping RMIServerAdapter...");
        if (rmiRegistry != null) {
            try {
                rmiRegistry.unbind(IServerRemote.SERVICE_NAME);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error unbinding RMIServerAdapter from registry", e);
            }
        }
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error unexporting RMIServerAdapter", e);
        }
        rmiClientListeners.clear();
        LOGGER.info("RMIServerAdapter stopped.");
    }

    @Override
    public String registerClient(IClientRemoteListener clientListener) throws RemoteException {
        if (!running.get()) throw new RemoteException("RMI Server Adapter is not running.");
        String clientSessionToken = "rmi-sid-" + UUID.randomUUID().toString();
        rmiClientListeners.put(clientSessionToken, clientListener);
        LOGGER.info("RMI Client listener registered. Session token: " + clientSessionToken);
        if (onClientConnectedCallback != null) {
            onClientConnectedCallback.accept(clientSessionToken);
        }
        return clientSessionToken;
    }

    @Override
    public void unregisterClient(String clientSessionToken) throws RemoteException {
        if (clientSessionToken != null && rmiClientListeners.remove(clientSessionToken) != null) {
            LOGGER.info("RMI Client listener unregistered: " + clientSessionToken);
            if (onClientDisconnectedCallback != null) {
                onClientDisconnectedCallback.accept(clientSessionToken);
            }
        }
    }

    @Override
    public void dispatchClientCommand(String clientSessionToken, Message command) throws RemoteException {
        if (!running.get()) throw new RemoteException("RMI Server Adapter is not running.");
        if (clientSessionToken == null || !rmiClientListeners.containsKey(clientSessionToken)) {
            throw new RemoteException("Client session token " + clientSessionToken + " not recognized.");
        }
        if (onMessageReceivedCallback != null) {
            onMessageReceivedCallback.accept(clientSessionToken, command);
        } else {
            LOGGER.severe("RMIServerAdapter: onMessageReceivedCallback is null. Cannot process command.");
        }
    }

    @Override
    public void pingServer() throws RemoteException {
        if (!running.get()) throw new RemoteException("RMI Server Adapter is not running.");
    }

    @Override
    public boolean sendMessageToClient(String clientId, Message message) {
        if (!running.get()) return false;
        IClientRemoteListener listener = rmiClientListeners.get(clientId);
        if (listener != null) {
            try {
                listener.onMessageFromServer(message);
                return true;
            } catch (RemoteException e) {
                LOGGER.log(Level.WARNING, "RemoteException during RMI callback to " + clientId, e);
                rmiClientListeners.remove(clientId);
                if (onClientDisconnectedCallback != null) {
                    onClientDisconnectedCallback.accept(clientId);
                }
                return false;
            }
        }
        return false;
    }

    @Override
    public void broadcastMessage(Message message) {
        if (!running.get()) return;
        for (String clientToken : List.copyOf(rmiClientListeners.keySet())) {
            sendMessageToClient(clientToken, message);
        }
    }

    @Override
    public void setOnClientConnected(Consumer<String> handler) {
        this.onClientConnectedCallback = Objects.requireNonNull(handler);
    }

    @Override
    public void setOnClientDisconnected(Consumer<String> handler) {
        this.onClientDisconnectedCallback = Objects.requireNonNull(handler);
    }

    @Override
    public void setOnMessageReceived(BiConsumer<String, Message> handler) {
        this.onMessageReceivedCallback = Objects.requireNonNull(handler);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}