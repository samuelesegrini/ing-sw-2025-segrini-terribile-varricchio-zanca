package it.polimi.ingsw.server;

import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.server.controller.LoginController;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.network.ServerNetworkInterface;
import it.polimi.ingsw.server.network.ServerNetworkManager;
import it.polimi.ingsw.server.network.SocketServerAdapter;

import java.io.BufferedReader; // Added
import java.io.InputStreamReader; // Added
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ServerApp {
    private static final Logger ROOT_LOGGER = Logger.getLogger("com.yourdomain.galaxytrucker");
    private static final int PORT = 12345;

    public static void main(String[] args) {
        ROOT_LOGGER.setLevel(Level.INFO);
        ConsoleHandler logHandler = new ConsoleHandler();
        logHandler.setFormatter(new SimpleFormatter());
        logHandler.setLevel(Level.INFO);
        ROOT_LOGGER.addHandler(logHandler);
        ROOT_LOGGER.setUseParentHandlers(false);
        Logger.getLogger(EventBus.class.getName()).setLevel(Level.INFO);


        EventBus serverEventBus = new EventBus(4, "server-main-eb");
        ServerNetworkInterface adapter = new SocketServerAdapter();
        ServerNetworkManager networkManager = new ServerNetworkManager(adapter, serverEventBus);

        // Instantiate GameSessionManager
        GameSessionManager sessionManager = new GameSessionManager(serverEventBus, networkManager);

        // Pass sessionManager to LoginController
        LoginController loginController = new LoginController(networkManager, serverEventBus, sessionManager);

        adapter.setOnClientDisconnected(networkClientId -> {
            ROOT_LOGGER.info("ServerApp: Client disconnected with network ID - " + networkClientId);
            // Now LoginController has the mapping and can handle it properly
            loginController.handleClientDisconnect(networkClientId);
        });

        try {
            networkManager.start(PORT);
            ROOT_LOGGER.info("Server started successfully on port " + PORT + ". Type 'quit' or 'exit' to stop.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                ROOT_LOGGER.info("Shutdown hook triggered. Gracefully stopping server...");
                if (networkManager.isRunning()) {
                    networkManager.stop();
                }
                serverEventBus.shutdown();
                ROOT_LOGGER.info("Server stopped.");
            }));

            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while (networkManager.isRunning() && (line = consoleReader.readLine()) != null) {
                if ("quit".equalsIgnoreCase(line.trim()) || "exit".equalsIgnoreCase(line.trim())) {
                    ROOT_LOGGER.info("Shutdown command received from console. Initiating shutdown...");
                    System.exit(0);
                    break;
                }
                ROOT_LOGGER.info("Unknown command received on server console: " + line);
            }
            if (!networkManager.isRunning()) {
                ROOT_LOGGER.warning("Server network seems to have stopped. Main thread exiting.");
            }

        } catch (IOException e) {
            ROOT_LOGGER.log(Level.SEVERE, "Failed to start or run server: " + e.getMessage(), e);
            networkManager.stop();
            serverEventBus.shutdown();
            System.exit(1);
        }
    }
}