package it.polimi.ingsw.client.network;

import it.polimi.ingsw.client.network.ClientNetworkInterface;
import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.common.network.rmi.IClientRemoteListener;
import it.polimi.ingsw.common.network.rmi.IServerRemote;

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RMIClientAdapter extends UnicastRemoteObject implements ClientNetworkInterface, IClientRemoteListener {
    private static final Logger LOGGER = Logger.getLogger(RMIClientAdapter.class.getName());
    private static final long serialVersionUID = 1L;

    private transient IServerRemote serverRemoteStub;
    private transient Consumer<Message> onMessageReceivedCallback;
    private transient Runnable onDisconnectedCallback;
    private String clientSessionToken;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private final transient ExecutorService commandExecutor;
    private final transient ScheduledExecutorService pingScheduler;
    private static final long PING_INTERVAL_SECONDS = 15;
    private static final int PING_MAX_FAILURES = 3;
    private transient int currentPingFailures = 0;

    public RMIClientAdapter() throws RemoteException {
        super(0);
        this.commandExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r); t.setName("client-rmi-command-executor"); t.setDaemon(true); return t;
        });
        this.pingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r); t.setName("client-rmi-ping-scheduler"); t.setDaemon(true); return t;
        });
    }

    @Override
    public void connect(String host, int port) throws IOException {
        if (connected.get()) {
            LOGGER.warning("RMIClientAdapter: Already connected.");
            return;
        }
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            serverRemoteStub = (IServerRemote) registry.lookup(IServerRemote.SERVICE_NAME);
            clientSessionToken = serverRemoteStub.registerClient(this);
            if (clientSessionToken == null || clientSessionToken.isBlank()) {
                throw new IOException("Failed to register with RMI server or received invalid session token.");
            }
            connected.set(true);
            LOGGER.info("RMIClientAdapter: Connected to server. Session Token: " + clientSessionToken);
            startServerPingingTask();
        } catch (NotBoundException | RemoteException e) {
            gracefulShutdown(true);
            throw new IOException("RMI connection/registration failed: " + e.getMessage(), e);
        }
    }

    private void startServerPingingTask() {
        if (!pingScheduler.isShutdown() && pingScheduler.isTerminated()) { // Re-create if shut down
            // pingScheduler = Executors.newSingleThreadScheduledExecutor(...); // This line is problematic, executor is final
            // Better to ensure it's not shut down before calling schedule
        }
        currentPingFailures = 0; // Reset before starting
        pingScheduler.scheduleWithFixedDelay(() -> {
            if (!connected.get()) {
                throw new RuntimeException("Pinging stopped: client not connected.");
            }
            try {
                serverRemoteStub.pingServer();
                currentPingFailures = 0;
            } catch (RemoteException e) {
                currentPingFailures++;
                LOGGER.warning("RMI ping to server failed (" + currentPingFailures + "/" + PING_MAX_FAILURES + ")");
                if (currentPingFailures >= PING_MAX_FAILURES) {
                    LOGGER.severe("Max RMI ping failures. Disconnecting.");
                    gracefulShutdown(true);
                    throw new RuntimeException("Max ping failures, stopping task.");
                }
            }
        }, PING_INTERVAL_SECONDS, PING_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void disconnect() {
        gracefulShutdown(false);
    }

    private void shutdownExecutor(ExecutorService executor, String name) {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow(); // Interrupt tasks
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    LOGGER.warning(name + " executor did not terminate gracefully.");
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void gracefulShutdown(boolean dueToError) {
        if (connected.compareAndSet(true, false)) {
            LOGGER.info("RMIClientAdapter: Disconnecting... Error: " + dueToError);
            shutdownExecutor(pingScheduler, "RMI Ping Scheduler");

            if (serverRemoteStub != null && clientSessionToken != null && !dueToError) {
                try {
                    serverRemoteStub.unregisterClient(clientSessionToken);
                } catch (RemoteException e) {
                    LOGGER.log(Level.WARNING, "RMIClientAdapter: Error unregistering from server", e);
                }
            }
            serverRemoteStub = null;
            clientSessionToken = null;

            try {
                UnicastRemoteObject.unexportObject(this, true);
            } catch (NoSuchObjectException e) { /* Already unexported or never fully */ }

            if (onDisconnectedCallback != null) {
                onDisconnectedCallback.run();
            }
            LOGGER.info("RMIClientAdapter: Disconnected state set.");
        }
        shutdownExecutor(commandExecutor, "RMI Command Executor");
    }

    @Override
    public boolean sendMessage(Message message) {
        if (!isConnected() || serverRemoteStub == null || clientSessionToken == null) return false;
        commandExecutor.submit(() -> {
            try {
                serverRemoteStub.dispatchClientCommand(clientSessionToken, message);
            } catch (RemoteException e) {
                LOGGER.log(Level.SEVERE, "RMI dispatchClientCommand failed for " + message.getClass().getSimpleName(), e);
                gracefulShutdown(true);
            }
        });
        return true;
    }

    @Override
    public void onMessageFromServer(Message message) throws RemoteException {
        if (!connected.get()) return;
        if (onMessageReceivedCallback != null) {
            onMessageReceivedCallback.accept(message);
        }
    }

    @Override
    public void pingClient() throws RemoteException {
        if(!connected.get()) throw new RemoteException("Client is disconnected.");
    }

    @Override
    public void setOnMessageReceived(Consumer<Message> handler) {
        this.onMessageReceivedCallback = Objects.requireNonNull(handler);
    }

    @Override
    public void setOnDisconnected(Runnable handler) {
        this.onDisconnectedCallback = Objects.requireNonNull(handler);
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }
}