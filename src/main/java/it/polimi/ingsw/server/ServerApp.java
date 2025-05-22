package it.polimi.ingsw.server;

import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.message.system.ClientLoginRequest;
import it.polimi.ingsw.server.controller.CommandDispatcher;
import it.polimi.ingsw.server.controller.action.*;
import it.polimi.ingsw.server.controller.notification.NotificationController;
import it.polimi.ingsw.server.core.*;
import it.polimi.ingsw.server.network.RMIServerAdapter;
import it.polimi.ingsw.server.network.ServerNetworkManager;
import it.polimi.ingsw.server.network.SocketServerAdapter;
import it.polimi.ingsw.common.message.system.PingMessage;
import it.polimi.ingsw.common.message.system.PongMessage;
import it.polimi.ingsw.common.message.setup.*;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ServerApp {
    private static final Logger ROOT_LOGGER = Logger.getLogger("it.polimi.ingsw.server");
    private static final int DEFAULT_SOCKET_PORT = 12345;
    private static final int DEFAULT_RMI_PORT = 1099;

    private static final Map<String, String> networkClientToGamePlayerMap = new ConcurrentHashMap<>();
    private static final Map<String, String> activePlayersByIdMap = new ConcurrentHashMap<>(); // gamePlayerId -> nickname

    private static EventBus serverEventBus;
    private static ExecutorService gameLogicExecutor;
    private static ServerNetworkManager networkManager;
    private static GameSessionManager sessionManager;
    private static PlayerSessionRegistry playerSessionRegistry;
    private static ConnectionMonitorService connectionMonitor;
    private static CommandDispatcher dispatcher;
    private static AuthenticationActionController authController;
    private static GameBrowserActionController gameBrowserController;
    private static GameLifecycleActionController gameLifecycleController;
    private static KeepAliveActionController keepAliveController;

    private static NotificationController notificationController;


    public static void main(String[] args) {
        setupLogging();
        configurePorts(args);

        //Initialize Core Components
        serverEventBus = new EventBus(Runtime.getRuntime().availableProcessors() * 2 + 2, "server-main-eb");
        gameLogicExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> { Thread t = new Thread(r); t.setName("game-logic-worker-" + t.threadId()); t.setDaemon(true); return t; }
        );

        networkManager = new ServerNetworkManager(serverEventBus);
        try {
            networkManager.addNetworkAdapter(new SocketServerAdapter());
            networkManager.addNetworkAdapter(new RMIServerAdapter());
        } catch (RemoteException e) {
            ROOT_LOGGER.log(Level.SEVERE, "Failed to initialize RMI adapter, RMI features may be unavailable.", e);
        }

        playerSessionRegistry = new PlayerSessionRegistry();
        sessionManager = new GameSessionManager(serverEventBus, networkManager, playerSessionRegistry);

        //Initialize Connection Monitor
        connectionMonitor = new ConnectionMonitorService(networkManager, serverEventBus, playerSessionRegistry, sessionManager, networkClientToGamePlayerMap);        // Pass CommandContext a reference to connectionMonitor

        //Initialize Action Controllers
        authController = new AuthenticationActionController();
        gameBrowserController = new GameBrowserActionController();
        gameLifecycleController = new GameLifecycleActionController();
        keepAliveController = new KeepAliveActionController();

        //Initialize Notification Controller (subscribes to eventBus)
        notificationController = new NotificationController(serverEventBus, networkManager, sessionManager, playerSessionRegistry);

        //Create and configure CommandDispatcher
        dispatcher = new CommandDispatcher(
                sessionManager, networkManager, serverEventBus,
                playerSessionRegistry, networkClientToGamePlayerMap, activePlayersByIdMap,
                gameLogicExecutor, connectionMonitor
        );
        networkManager.setCommandDispatcher(dispatcher);

        //Register All Command Handlers
        registerCommandHandlers();

        //Setup Global Network Callbacks
        setupGlobalNetworkCallbacks();

        //Start Server and Services
        try {
            networkManager.startAdapters(DEFAULT_SOCKET_PORT, DEFAULT_RMI_PORT);
            connectionMonitor.startMonitoring(); // Start pinging

            ROOT_LOGGER.info("Server started. Socket on " + DEFAULT_SOCKET_PORT + ", RMI on " + DEFAULT_RMI_PORT +
                    (networkManager.isRunning() ? ". All systems go!" : ". One or more systems failed.") +
                    " Type 'quit' or 'exit' to stop.");

            setupShutdownHook();
            handleConsoleInput();

        } catch (IOException e) {
            ROOT_LOGGER.log(Level.SEVERE, "Fatal I/O Error during server startup: " + e.getMessage(), e);
            shutdownServerComponents(true);
            System.exit(1);
        }
    }

    private static void setupLogging() {
        ROOT_LOGGER.setLevel(Level.INFO);
        ConsoleHandler logHandler = new ConsoleHandler();
        logHandler.setFormatter(new SimpleFormatter());
        logHandler.setLevel(Level.ALL);
        if (ROOT_LOGGER.getHandlers().length == 0) ROOT_LOGGER.addHandler(logHandler);
        ROOT_LOGGER.setUseParentHandlers(false);
        // Set levels for noisy components if needed, e.g.:
        Logger.getLogger(EventBus.class.getName()).setLevel(Level.INFO);
        Logger.getLogger(ConnectionMonitorService.class.getName()).setLevel(Level.INFO);
    }

    private static int socketPort = DEFAULT_SOCKET_PORT;
    private static int rmiPort = DEFAULT_RMI_PORT;

    private static void configurePorts(String[] args) {
        if (args.length >= 1) { try { socketPort = Integer.parseInt(args[0]); } catch (NumberFormatException e) { ROOT_LOGGER.warning("Invalid Socket port arg, using default: " + socketPort); } }
        if (args.length >= 2) { try { rmiPort = Integer.parseInt(args[1]); } catch (NumberFormatException e) { ROOT_LOGGER.warning("Invalid RMI port arg, using default: " + rmiPort); } }
    }


    private static void registerCommandHandlers() {
        // Authentication
        dispatcher.registerHandler(ClientLoginRequest.class, authController::handleLogin);

        // Game Browsing
        dispatcher.registerHandler(RequestGameListCommand.class, gameBrowserController::handleRequestGameList);

        // Game Lifecycle & Lobby
        dispatcher.registerHandler(CreateGameRequestCommand.class, gameLifecycleController::handleCreateGame);
        dispatcher.registerHandler(JoinGameRequestCommand.class, gameLifecycleController::handleJoinGame);
        dispatcher.registerHandler(LeaveGameRequestCommand.class, gameLifecycleController::handleLeaveGame);
        dispatcher.registerHandler(SetPlayerReadyCommand.class, gameLifecycleController::handleSetPlayerReady);
        dispatcher.registerHandler(StartGameRequestCommand.class, gameLifecycleController::handleStartGame);

        // Keep-Alive
        dispatcher.registerHandler(PingMessage.class, keepAliveController::handlePing);
        dispatcher.registerHandler(PongMessage.class, keepAliveController::handlePong);
    }

    private static void setupGlobalNetworkCallbacks() {
        networkManager.setGlobalOnClientConnected(networkClientId -> {
            ROOT_LOGGER.info("ServerApp Main Callback: Client connected " + networkClientId + ". Awaiting login command via Dispatcher.");
        });

        networkManager.setGlobalOnClientDisconnected(networkClientId -> {
            ROOT_LOGGER.info("ServerApp Main Callback: Client disconnected " + networkClientId + ". Notifying AuthController for cleanup.");
            authController.handlePlayerDisconnect(
                    networkClientId,
                    playerSessionRegistry,
                    networkClientToGamePlayerMap,
                    activePlayersByIdMap,
                    serverEventBus,
                    sessionManager
            );
        });
    }

    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ROOT_LOGGER.info("Shutdown hook triggered. Gracefully stopping server...");
            shutdownServerComponents(false);
            ROOT_LOGGER.info("Server stopped.");
        }));
    }

    private static void shutdownServerComponents(boolean isErrorExit) {
        if (connectionMonitor != null) connectionMonitor.stopMonitoring();
        if (networkManager != null) networkManager.stop();
        if (gameLogicExecutor != null && !gameLogicExecutor.isShutdown()) {
            try {
                gameLogicExecutor.shutdown();
                if (!gameLogicExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    gameLogicExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                gameLogicExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (serverEventBus != null) serverEventBus.shutdown();
    }

    private static void handleConsoleInput() {
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        try {
            while (networkManager != null && networkManager.isRunning() && (line = consoleReader.readLine()) != null) {
                if ("quit".equalsIgnoreCase(line.trim()) || "exit".equalsIgnoreCase(line.trim())) {
                    ROOT_LOGGER.info("Shutdown command received from console. Initiating shutdown...");
                    System.exit(0);
                    break;
                }
                ROOT_LOGGER.info("Unknown command on server console: " + line + ". Available: quit, exit.");
            }
        } catch (IOException e) {
            ROOT_LOGGER.log(Level.WARNING, "Error reading from console input.", e);
        } finally {
            if (networkManager == null || !networkManager.isRunning()) {
                ROOT_LOGGER.warning("Server network seems to have stopped or was not fully started. Main thread input loop exiting.");
            }
        }
    }
}
