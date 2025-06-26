package it.polimi.ingsw.server;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.common.message.EventPublisher;
import it.polimi.ingsw.common.message.EventPublisherImpl;
import it.polimi.ingsw.common.message.event.Event;
import it.polimi.ingsw.common.message.event.PlayerLeftGameEvent;
import it.polimi.ingsw.common.message.event.PlayerDisconnectedEvent;
import it.polimi.ingsw.common.message.event.PlayerReconnectedEvent;
import it.polimi.ingsw.server.controller.CommandDispatcher;
import it.polimi.ingsw.server.core.*;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.monitor.ConnectionMonitorService;
import it.polimi.ingsw.server.network.RMIServerAdapter;
import it.polimi.ingsw.server.network.ServerNetworkManager;
import it.polimi.ingsw.server.network.SocketServerAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

/**

 Main server application for Galaxy Trucker.
 Manages all server components and handles the game lifecycle.
 */
public class ServerApp {
    private static final Logger LOGGER = Logger.getLogger(ServerApp.class.getName());
    private static final int DEFAULT_SOCKET_PORT = 12345;
    private static final int DEFAULT_RMI_PORT = 1099;
    // Core components
    private ServerNetworkManager networkManager;
    private GameSessionManager sessionManager;
    private PlayerSessionRegistry playerRegistry;
    private CommandDispatcher commandDispatcher;
    private ConnectionMonitorService connectionMonitor;
    // Configuration
    private int socketPort = DEFAULT_SOCKET_PORT;
    private int rmiPort = DEFAULT_RMI_PORT;
    public static void main(String[] args) {
        ServerApp server = new ServerApp();
        server.parseArguments(args);

        // --- IMPORTANT: Load logging configuration FIRST ---
        try (InputStream is = ServerApp.class.getResourceAsStream("/server_logging.properties")) {
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
            } else {
                System.err.println("WARNING: server_logging.properties not found. Default JUL logging will be used.");
            }
        } catch (Exception e) {
            System.err.println("ERROR loading server logging configuration: " + e.getMessage());
            e.printStackTrace();
        }

        LOGGER.info("ServerApp started. Logging configured.");

        try {
            server.initialize();
            server.start();
            server.handleConsoleInput();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fatal error during server operation", e);
            System.exit(1);
        }
    }
    /**
     Parses command line arguments.
     */
    private void parseArguments(String[] args) {
        if (args.length >= 1) {
            try {
                socketPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid socket port argument, using default: " + socketPort);
            }
        }
        if (args.length >= 2) {
            try {
                rmiPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid RMI port argument, using default: " + rmiPort);
            }
        }
    }
    
    /**
     Initializes all server components.
     */
    private void initialize() throws IOException {
        LOGGER.info("Initializing Galaxy Trucker server...");

        int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2 + 2;
        networkManager = new ServerNetworkManager();
        playerRegistry = new PlayerSessionRegistry();

        initializeNetworkAdapters();

        Map<String, String> networkClientToGamePlayerMap = new ConcurrentHashMap<>();
        
        // Create session manager without event publisher initially
        sessionManager = new GameSessionManager(networkManager, playerRegistry, null);
        
        // Create command dispatcher which creates the event publisher
        commandDispatcher = new CommandDispatcher(
                sessionManager,
                playerRegistry,
                networkManager,
                networkClientToGamePlayerMap
        );
        
        // Now update session manager with the real event publisher
        sessionManager.setEventPublisher(commandDispatcher.getEventPublisher());
        networkManager.setCommandDispatcher(commandDispatcher);

        connectionMonitor = new ConnectionMonitorService(
                networkManager,
                playerRegistry,
                sessionManager,
                networkClientToGamePlayerMap,
                this::handleClientDisconnect
        );
        
        // Set connection monitor in network manager for pong handling
        networkManager.setConnectionMonitor(connectionMonitor);

        setupNetworkCallbacks();

        setupShutdownHook();
        LOGGER.info("Server initialization complete");
    }
    /**
     Initializes network adapters.
     */
    private void initializeNetworkAdapters() throws RemoteException {

        networkManager.addNetworkAdapter(new SocketServerAdapter());
        LOGGER.info("Socket adapter initialized");

        try {
            networkManager.addNetworkAdapter(new RMIServerAdapter());
            LOGGER.info("RMI adapter initialized");
        } catch (RemoteException e) {
            LOGGER.log(Level.WARNING, "Failed to initialize RMI adapter", e);

        }
    }
    /**
     Sets up network callbacks.
     */
    private void setupNetworkCallbacks() {
        networkManager.setGlobalOnClientConnected(clientId -> {
            LOGGER.info("Client connected: " + clientId);

        });
        networkManager.setGlobalOnClientDisconnected(clientId -> {
            LOGGER.info("Client disconnected: " + clientId);
            handleClientDisconnect(clientId);
        });
    }
    /**
     Handles client disconnection.
     */
    private void handleClientDisconnect(String clientId) {

        PlayerId playerId = playerRegistry.getPlayerIdForClient(clientId);
        if (playerId != null) {
            String nickname = playerRegistry.getPlayerNickname(playerId);

            // Check if player is in a game
            GameSession gameSession = sessionManager.getGameSessionForPlayer(playerId);
            if (gameSession != null) {
                // Determine if this is an active game or just lobby
                boolean isInActiveGame = gameSession.isStarted();

                // Publish player disconnected event (not left - they might reconnect)
                PlayerDisconnectedEvent event = new PlayerDisconnectedEvent(
                        gameSession.getGameId(),
                        playerId,
                        nickname,
                        isInActiveGame
                );
                publishEvent(event);

                // Don't remove from game immediately - allow reconnection
                // The player remains in the game session but marked as disconnected
                LOGGER.info("Player " + nickname + " disconnected from game " + gameSession.getGameId() + 
                           " (active: " + isInActiveGame + ") - session preserved for reconnection");
            }

            // Mark player as disconnected but keep session for reconnection
            playerRegistry.unregisterPlayer(clientId);
        }
    }
    /**
     Publishes an event to clients.
     */
    private void publishEvent(Event event) {
        EventPublisher publisher = commandDispatcher.getEventPublisher();
        if (publisher != null) {
            publisher.publishEvent(event);
        }
    }
    /**
     Starts the server.
     */
    private void start() throws IOException {
        LOGGER.info("Starting Galaxy Trucker server...");

        networkManager.startAdapters(socketPort, rmiPort);

        connectionMonitor.startMonitoring();
        LOGGER.info("=================================================");
        LOGGER.info("Galaxy Trucker Server Started Successfully!");
        LOGGER.info("Socket port: " + socketPort);
        LOGGER.info("RMI port: " + rmiPort);
        LOGGER.info("Ready to accept connections...");
        LOGGER.info("Type 'help' for available commands");
        LOGGER.info("=================================================");
    }
    /**
     Handles console input.
     */
    private void handleConsoleInput() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();

                switch (line) {
                    case "quit":
                    case "exit":
                        LOGGER.info("Shutdown command received");
                        shutdown();
                        System.exit(0);
                        break;

                    case "help":
                        printHelp();
                        break;

                    case "status":
                        printStatus();
                        break;

                    case "games":
                        printGames();
                        break;

                    case "players":
                        printPlayers();
                        break;

                    default:
                        if (!line.isEmpty()) {
                            LOGGER.info("Unknown command: " + line + ". Type 'help' for available commands.");
                        }
                        break;
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading console input", e);
        }
    }
    /**
     Prints help information.
     */
    private void printHelp() {
        System.out.println("Available commands:");
        System.out.println(" help - Show this help message");
        System.out.println(" status - Show server status");
        System.out.println(" games - List active games");
        System.out.println(" players - List connected players");
        System.out.println(" quit - Shutdown the server");
        System.out.println(" exit - Shutdown the server");
    }
    /**
     Prints server status.
     */
    private void printStatus() {
        System.out.println("Server Status:");
        System.out.println(" Network: " + (networkManager.isRunning() ? "Running" : "Stopped"));
        System.out.println(" Socket Port: " + socketPort);
        System.out.println(" RMI Port: " + rmiPort);
        System.out.println(" Connected Clients: " + playerRegistry.getAllClientIds().size());
        System.out.println(" Active Games: " + sessionManager.getAvailableGames().size());
    }
    /**
     Prints active games.
     */
    private void printGames() {
        List<GameModel> games = sessionManager.getAvailableGames();
        if (games.isEmpty()) {
            System.out.println("No active games");
        } else {
            System.out.println("Active Games:");
            for (GameModel game : games) {
                System.out.println(" Game ID: " + game.getGameId());
                System.out.println(" Name: " + (game.getGameName() != null ? game.getGameName() : "Unnamed"));
                System.out.println(" Players: " + game.getPlayers().size() + "/" + game.getMaxPlayers());
                System.out.println(" Level: " + game.getGameLevel());
                System.out.println();
            }
        }
    }
    /**
     Prints connected players.
     */
    private void printPlayers() {
        Set<String> clientIds = playerRegistry.getAllClientIds();
        if (clientIds.isEmpty()) {
            System.out.println("No connected players");
        } else {
            System.out.println("Connected Players:");
            for (String clientId : clientIds) {
                Map<String, String> info = playerRegistry.getPlayerInfo(clientId);
                if (info != null) {
                    System.out.println(" " + info.get("nickname") + " (ID: " + info.get("playerId") + ")");
                }
            }
        }
    }
    /**
     Sets up shutdown hook.
     */
    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown hook triggered");
            shutdown();
        }));
    }
    /**
     Shuts down the server gracefully.
     */
    private void shutdown() {
        LOGGER.info("Shutting down Galaxy Trucker server...");

        if (connectionMonitor != null) {
            connectionMonitor.stopMonitoring();
        }

        if (networkManager != null) {
            networkManager.stop();
        }

        if (commandDispatcher != null) {
            commandDispatcher.shutdown();
        }

        if (sessionManager != null) {
            sessionManager.shutdown();
        }
        LOGGER.info("Server shutdown complete");
    }
    // Getters for testing and integration
    public GameSessionManager getSessionManager() {
        return sessionManager;
    }
    public PlayerSessionRegistry getPlayerRegistry() {
        return playerRegistry;
    }
    public ServerNetworkManager getNetworkManager() {
        return networkManager;
    }
    public CommandDispatcher getCommandDispatcher() {
        return commandDispatcher;
    }
}