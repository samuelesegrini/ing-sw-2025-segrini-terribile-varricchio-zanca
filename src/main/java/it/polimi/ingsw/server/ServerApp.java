package it.polimi.ingsw.server;

import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.server.controller.LoginController;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.network.ServerNetworkInterface;
import it.polimi.ingsw.server.network.ServerNetworkManager;
import it.polimi.ingsw.server.network.SocketServerAdapter;
import it.polimi.ingsw.server.network.RMIServerAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ServerApp {
    private static final Logger ROOT_LOGGER = Logger.getLogger("it.polimi.ingsw.server");
    private static final int DEFAULT_SOCKET_PORT = 12345;
    private static final int DEFAULT_RMI_PORT = 1099;

    public static void main(String[] args) {
        // --- Logging Setup ---
        ROOT_LOGGER.setLevel(Level.INFO);
        ConsoleHandler logHandler = new ConsoleHandler();
        logHandler.setFormatter(new SimpleFormatter());
        logHandler.setLevel(Level.INFO);
        if (ROOT_LOGGER.getHandlers().length == 0) {
            ROOT_LOGGER.addHandler(logHandler);
        }
        ROOT_LOGGER.setUseParentHandlers(false);
        Logger.getLogger(EventBus.class.getName()).setLevel(Level.INFO);
        Logger.getLogger(RMIServerAdapter.class.getName()).setLevel(Level.INFO);
        Logger.getLogger(SocketServerAdapter.class.getName()).setLevel(Level.INFO);


        // --- Port Configuration (Optional: from args) ---
        int socketPort = DEFAULT_SOCKET_PORT;
        int rmiPort = DEFAULT_RMI_PORT;
        // Example: args[0] = socket_port, args[1] = rmi_port
        if (args.length >= 1) {
            try { socketPort = Integer.parseInt(args[0]); }
            catch (NumberFormatException e) { ROOT_LOGGER.warning("Invalid Socket port arg, using default: " + socketPort); }
        }
        if (args.length >= 2) {
            try { rmiPort = Integer.parseInt(args[1]); }
            catch (NumberFormatException e) { ROOT_LOGGER.warning("Invalid RMI port arg, using default: " + rmiPort); }
        }

        // --- Core Components Initialization ---
        EventBus serverEventBus = new EventBus(4, "server-main-eb");
        ServerNetworkManager networkManager = new ServerNetworkManager(serverEventBus);

        // --- Initialize and Add Socket Adapter ---
        try {
            ServerNetworkInterface socketAdapter = new SocketServerAdapter();
            networkManager.addNetworkAdapter(socketAdapter);
            ROOT_LOGGER.info("Socket adapter configured.");
        } catch (Exception e) { // Catch broad exception for adapter instantiation if any
            ROOT_LOGGER.log(Level.SEVERE, "Failed to create SocketServerAdapter.", e);
        }

        // --- Initialize and Add RMI Adapter ---
        try {
            ServerNetworkInterface rmiAdapter = new RMIServerAdapter(); // Can throw RemoteException
            networkManager.addNetworkAdapter(rmiAdapter);
            ROOT_LOGGER.info("RMI adapter configured.");
        } catch (RemoteException e) {
            ROOT_LOGGER.log(Level.SEVERE, "Failed to create RMIServerAdapter. RMI may not be available.", e);
        } catch (Exception e) {
            ROOT_LOGGER.log(Level.SEVERE, "Unexpected error creating RMIServerAdapter.", e);
        }


        GameSessionManager sessionManager = new GameSessionManager(serverEventBus, networkManager);
        LoginController loginController = new LoginController(networkManager, serverEventBus, sessionManager);

        // Set the global connection handlers on ServerNetworkManager
        // These will be invoked by SNM when any of its adapters report a connection/disconnection.
        networkManager.setGlobalOnClientConnected(clientId -> {
            ROOT_LOGGER.info("ServerApp (via SNM): Client connected. Network ID: " + clientId);
            loginController.handleClientConnect(clientId);
        });
        networkManager.setGlobalOnClientDisconnected(networkClientId -> {
            ROOT_LOGGER.info("ServerApp (via SNM): Client disconnected. Network ID: " + networkClientId);
            loginController.handleClientDisconnect(networkClientId);
        });

        final ServerNetworkManager finalNetworkManager = networkManager; // For shutdown hook

        try {
            // Start all configured adapters
            networkManager.startAdapters(socketPort, rmiPort);
            ROOT_LOGGER.info("Server started. Listening for Socket on " + socketPort + " and RMI on " + rmiPort +
                    (networkManager.isRunning() ? ". All systems go!" : ". One or more systems may have failed to start." ) +
                    " Type 'quit' or 'exit' to stop.");


            // --- Shutdown Hook ---
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                ROOT_LOGGER.info("Shutdown hook triggered. Gracefully stopping server...");
                if (finalNetworkManager != null) { // Check if networkManager itself is not null
                    finalNetworkManager.stop();
                }
                if (serverEventBus != null) {
                    serverEventBus.shutdown();
                }
                ROOT_LOGGER.info("Server stopped.");
            }));

            // --- Console Command Handling for Shutdown ---
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while (finalNetworkManager != null && finalNetworkManager.isRunning() && (line = consoleReader.readLine()) != null) {
                if ("quit".equalsIgnoreCase(line.trim()) || "exit".equalsIgnoreCase(line.trim())) {
                    ROOT_LOGGER.info("Shutdown command received from console. Initiating shutdown...");
                    System.exit(0);
                    break;
                }
                ROOT_LOGGER.info("Unknown command received on server console: " + line);
            }
            if (finalNetworkManager == null || !finalNetworkManager.isRunning()) {
                ROOT_LOGGER.warning("Server network seems to have stopped or was not fully started. Main thread exiting.");
            }

        } catch (IOException e) {
            ROOT_LOGGER.log(Level.SEVERE, "Fatal I/O Error during server startup: " + e.getMessage(), e);
            if (serverEventBus != null) serverEventBus.shutdown();
            System.exit(1);
        }
    }
}