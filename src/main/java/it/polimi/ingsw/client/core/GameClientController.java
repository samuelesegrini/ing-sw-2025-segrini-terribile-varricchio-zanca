package it.polimi.ingsw.client.core;

import it.polimi.ingsw.client.controller.ClientLoginHandler;
import it.polimi.ingsw.client.model.ClientViewModel;
import it.polimi.ingsw.client.network.ClientNetworkInterface;
import it.polimi.ingsw.client.network.ClientNetworkManager;
import it.polimi.ingsw.client.network.SocketClientAdapter;
import it.polimi.ingsw.client.ui.UIFactory;
import it.polimi.ingsw.client.ui.UserInterface;
import it.polimi.ingsw.common.event.EventBus;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Main client controller responsible for orchestrating the Galaxy Trucker client application.
 * Manages the lifecycle of core client components including UI, network, and event systems.
 * Extends JavaFX Application to enable GUI functionality when in GUI mode.
 */
public class GameClientController extends Application {
    private static final Logger ROOT_LOGGER = Logger.getLogger("it.polimi.ingsw.client");
    
    private EventBus clientEventBus;
    private ClientViewModel clientViewModel;
    private ClientNetworkManager networkManager;
    private ClientLoginHandler clientLoginHandler;
    private UserInterface userInterface;
    private ExecutorService backgroundTaskExecutor;
    private static UIMode requestedUIMode = UIMode.GUI;
    
    /**
     * Enum defining the available user interface modes for the application.
     */
    public enum UIMode {
        GUI, TUI
    }
    
    /**
     * Sets the UI mode to use for the application.
     * Must be called before starting the application.
     * 
     * @param mode The UI mode to use (GUI or TUI)
     */
    public static void setUIMode(UIMode mode) {
        requestedUIMode = mode;
        ClientViewModel.setGUIMode(mode == UIMode.GUI);
    }
    
    /**
     * Returns the currently selected UI mode.
     * 
     * @return The current UI mode setting
     */
    public static UIMode getUIMode() {
        return requestedUIMode;
    }
    
    /**
     * JavaFX application lifecycle method - called when the application starts.
     * Initializes all client components and starts the appropriate UI.
     * 
     * @param primaryStage The primary JavaFX stage (only used in GUI mode)
     * @throws Exception If initialization fails
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        setupLogging();
        
        this.backgroundTaskExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("client-background-task");
            t.setDaemon(true);
            return t;
        });
        
        clientEventBus = new EventBus(2, "client-main-eb");
        clientViewModel = new ClientViewModel(clientEventBus);
        clientLoginHandler = new ClientLoginHandler(clientEventBus, clientViewModel);
        
        clientViewModel.setBackgroundTaskExecutor(backgroundTaskExecutor);
        clientLoginHandler.setBackgroundTaskExecutor(backgroundTaskExecutor);
        
        userInterface = UIFactory.createUserInterface(requestedUIMode, primaryStage);
        
        userInterface.setClientController(this);
        userInterface.setViewModel(clientViewModel);
        userInterface.initialize();
        
        userInterface.start();
    }
    
    /**
     * Configures application-wide logging settings.
     * Sets up appropriate log levels for different components.
     */
    private void setupLogging() {
        ROOT_LOGGER.setLevel(Level.INFO);
        ConsoleHandler logHandler = new ConsoleHandler();
        logHandler.setFormatter(new SimpleFormatter());
        logHandler.setLevel(Level.INFO);
        ROOT_LOGGER.addHandler(logHandler);
        ROOT_LOGGER.setUseParentHandlers(false);
        
        Logger.getLogger(EventBus.class.getName()).setLevel(Level.INFO);
        Logger.getLogger(ClientViewModel.class.getName()).setLevel(Level.INFO);
        Logger.getLogger(ClientLoginHandler.class.getName()).setLevel(Level.INFO);
        Logger.getLogger(ClientNetworkManager.class.getName()).setLevel(Level.INFO);
        Logger.getLogger(SocketClientAdapter.class.getName()).setLevel(Level.INFO);
    }
    
    /**
     * Establishes the network connection based on user input and initiates the login process.
     * Creates the appropriate network adapter based on the selected technology.
     * 
     * @param host The server hostname or IP address
     * @param port The server port
     * @param nickname The user's desired nickname
     * @param technology The network technology to use ("Socket" or "RMI")
     */
    public void setupNetworkAndConnect(String host, int port, String nickname, String technology) {
        ClientNetworkInterface adapter;
        if ("Socket".equalsIgnoreCase(technology)) {
            adapter = new SocketClientAdapter();
        } else if ("RMI".equalsIgnoreCase(technology)) {
            clientViewModel.setStatusMessage("RMI not supported yet.");
            clientViewModel.handleConnectionFailed("RMI not supported.");
            ROOT_LOGGER.warning("RMI connection attempt, but RMI is not implemented.");
            return;
        } else {
            clientViewModel.setStatusMessage("Unsupported network technology: " + technology);
            clientViewModel.handleConnectionFailed("Unsupported technology.");
            ROOT_LOGGER.warning("Unsupported network technology selected: " + technology);
            return;
        }
        
        if (networkManager != null && networkManager.isConnected()) {
            networkManager.disconnect();
        }
        
        networkManager = new ClientNetworkManager(adapter, clientEventBus);
        clientViewModel.setNetworkManager(networkManager);
        clientLoginHandler.setNetworkManager(networkManager);
        
        clientLoginHandler.initiateConnectionAndLogin(host, port, nickname);
    }
    
    /**
     * Requests application shutdown, which can be called from any component.
     * Initiates graceful termination of all resources.
     */
    public void requestShutdown() {
        ROOT_LOGGER.info("Shutdown requested");
        shutdown();
        
        if (requestedUIMode == UIMode.GUI) {
            Platform.exit();
        }
        
        System.exit(0);
    }
    
    /**
     * Performs the actual shutdown operations by cleaning up all resources.
     * Shuts down network connections, event bus, and thread pools.
     */
    public void shutdown() {
        ROOT_LOGGER.info("GameClientController shutting down...");
        
        if (userInterface != null) {
            userInterface.shutdown();
        }
        
        if (networkManager != null && networkManager.isConnected()) {
            networkManager.disconnect();
        }
        
        if (clientEventBus != null) {
            clientEventBus.shutdown();
        }
        
        if (backgroundTaskExecutor != null) {
            backgroundTaskExecutor.shutdown();
            try {
                if (!backgroundTaskExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    backgroundTaskExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                backgroundTaskExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Main entry point for the JavaFX GUI application.
     * Only used when running in GUI mode.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--tui") || arg.equalsIgnoreCase("-t")) {
                setUIMode(UIMode.TUI);
            }
        }
        
        if (requestedUIMode == UIMode.GUI) {
            launch(args);
        } else {
            System.err.println("For TUI mode, use Launcher.startTUIMode() instead of GameClientController.main()");
        }
    }
} 