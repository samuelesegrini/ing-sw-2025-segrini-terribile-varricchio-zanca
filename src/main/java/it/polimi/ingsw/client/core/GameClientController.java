package it.polimi.ingsw.client.core;

import it.polimi.ingsw.client.controller.ClientLoginHandler;
import it.polimi.ingsw.client.model.ClientViewModel;
import it.polimi.ingsw.client.network.ClientNetworkInterface;
import it.polimi.ingsw.client.network.ClientNetworkManager;
import it.polimi.ingsw.client.network.SocketClientAdapter;
import it.polimi.ingsw.client.network.RMIClientAdapter;
import it.polimi.ingsw.client.ui.UIFactory;
import it.polimi.ingsw.client.ui.UserInterface;
import it.polimi.ingsw.common.event.EventBus;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class GameClientController extends Application {
    private static final Logger ROOT_LOGGER = Logger.getLogger("it.polimi.ingsw.client");

    private EventBus clientEventBus;
    private ClientViewModel clientViewModel;
    private ClientNetworkManager networkManager; // Will hold the manager with the chosen adapter
    private ClientLoginHandler clientLoginHandler;
    private UserInterface userInterface;
    private ExecutorService backgroundTaskExecutor;
    private static UIMode requestedUIMode = UIMode.GUI; // Default

    public enum UIMode { GUI, TUI }

    public static void setUIMode(UIMode mode) {
        requestedUIMode = mode;
        ClientViewModel.setGUIMode(mode == UIMode.GUI); // Inform ViewModel
    }

    public static UIMode getUIMode() {
        return requestedUIMode;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        setupLogging();

        this.backgroundTaskExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r); t.setName("client-bg-task"); t.setDaemon(true); return t;
        });

        clientEventBus = new EventBus(2, "client-main-eb");
        clientViewModel = new ClientViewModel(clientEventBus);
        clientLoginHandler = new ClientLoginHandler(clientEventBus, clientViewModel);

        clientViewModel.setBackgroundTaskExecutor(backgroundTaskExecutor);
        clientLoginHandler.setBackgroundTaskExecutor(backgroundTaskExecutor);

        userInterface = UIFactory.createUserInterface(requestedUIMode, primaryStage);

        userInterface.setClientController(this); // Pass this GameClientController instance
        userInterface.setViewModel(clientViewModel);
        userInterface.initialize();
        userInterface.start(); // This should show the initial connection/login screen
    }

    private void setupLogging() {
        ROOT_LOGGER.setLevel(Level.INFO);
        ConsoleHandler logHandler = new ConsoleHandler();
        logHandler.setFormatter(new SimpleFormatter());
        logHandler.setLevel(Level.INFO);
        if (ROOT_LOGGER.getHandlers().length == 0) {
            ROOT_LOGGER.addHandler(logHandler);
        }
        ROOT_LOGGER.setUseParentHandlers(false);
        Logger.getLogger(EventBus.class.getName()).setLevel(Level.INFO);
        Logger.getLogger(ClientViewModel.class.getName()).setLevel(Level.INFO);
        Logger.getLogger(ClientLoginHandler.class.getName()).setLevel(Level.INFO);
        Logger.getLogger(ClientNetworkManager.class.getName()).setLevel(Level.INFO);
        Logger.getLogger(SocketClientAdapter.class.getName()).setLevel(Level.INFO);
        Logger.getLogger(RMIClientAdapter.class.getName()).setLevel(Level.INFO);
    }

    public void setupNetworkAndConnect(String host, int port, String nickname, String technology) {
        ClientNetworkInterface selectedAdapter;

        ROOT_LOGGER.info("Attempting to set up network with technology: " + technology);

        if ("Socket".equalsIgnoreCase(technology)) {
            selectedAdapter = new SocketClientAdapter();
            ROOT_LOGGER.info("Using Socket network adapter.");
        } else if ("RMI".equalsIgnoreCase(technology)) {
            try {
                selectedAdapter = new RMIClientAdapter(); // Can throw RemoteException
                ROOT_LOGGER.info("Using RMI network adapter.");
            } catch (RemoteException e) {
                String errorMsg = "Failed to initialize RMI Client Adapter: " + e.getMessage();
                ROOT_LOGGER.log(Level.SEVERE, errorMsg, e);
                if (clientViewModel != null) { // ViewModel should exist by now
                    clientViewModel.handleConnectionFailed(errorMsg);
                }
                return; // Stop further processing
            }
        } else {
            String errorMsg = "Unsupported network technology requested: " + technology;
            ROOT_LOGGER.warning(errorMsg);
            if (clientViewModel != null) {
                clientViewModel.handleConnectionFailed(errorMsg);
            }
            return;
        }

        // If there was an old network manager (e.g., due to reconnect attempt), disconnect it.
        if (this.networkManager != null && this.networkManager.isConnected()) {
            ROOT_LOGGER.info("Disconnecting existing network manager before creating a new one.");
            this.networkManager.disconnect();
        }

        // Create new NetworkManager with the selected adapter
        this.networkManager = new ClientNetworkManager(selectedAdapter, clientEventBus);

        // Inject the new networkManager into components that need it
        if (clientViewModel != null) {
            clientViewModel.setNetworkManager(this.networkManager);
        }
        if (clientLoginHandler != null) {
            clientLoginHandler.setNetworkManager(this.networkManager);
        }

        // Now, proceed with connection and login using the newly configured networkManager
        if (clientLoginHandler != null) {
            clientLoginHandler.initiateConnectionAndLogin(host, port, nickname);
        } else {
            ROOT_LOGGER.severe("ClientLoginHandler is null, cannot initiate connection.");
            if (clientViewModel != null) {
                clientViewModel.handleConnectionFailed("Internal error: Login handler not available.");
            }
        }
    }

    public void requestShutdown() {
        ROOT_LOGGER.info("Shutdown requested by UI or other component.");
        // Platform.runLater is important if called from non-FX thread to allow GUI cleanup
        Platform.runLater(this::shutdown);
    }

    // Actual shutdown logic
    private void shutdown() {
        ROOT_LOGGER.info("GameClientController performing shutdown...");

        if (userInterface != null) {
            userInterface.shutdown(); // UI specific cleanup (e.g. close stage)
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
        ROOT_LOGGER.info("GameClientController shutdown complete.");
        // For GUI mode, Platform.exit() ensures JavaFX thread terminates.
        // For TUI, System.exit(0) might be needed if daemon threads are still running.
        if (requestedUIMode == UIMode.GUI) {
            Platform.exit(); // Ensure JavaFX application thread exits if in GUI mode
        }
        System.exit(0); // Force exit if other non-daemon threads prevent JVM shutdown
    }

    @Override
    public void stop() throws Exception {
        // This is the JavaFX Application stop method, called when the last window is closed
        // or Platform.exit() is invoked.
        ROOT_LOGGER.info("JavaFX Application stop() method called. Initiating client shutdown.");
        shutdown(); // Call the general shutdown logic
        super.stop();
    }

    // Main method for launching JavaFX application if GUI mode is chosen by Launcher
    public static void main(String[] args) {
        // Launcher should handle UI mode selection and call Application.launch()
        // This main method here is primarily for JavaFX's launch mechanism.
        // If called directly, it defaults to GUI unless TUI is specified by arg for this main.
        boolean tuiMode = false;
        for(String arg : args) {
            if(arg.equalsIgnoreCase("--tui") || arg.equalsIgnoreCase("-t")) {
                tuiMode = true;
                break;
            }
        }
        if(tuiMode) {
            // This path is problematic if GameClientController.main is the entry point for TUI.
            // Launcher.java is the better entry point for TUI.
            System.err.println("Error: TUI mode should be launched via Launcher.java. GameClientController.main is for GUI.");
            System.err.println("Attempting TUI launch via GameClientController instance (not recommended practice)...");
            setUIMode(UIMode.TUI);
            GameClientController tuiApp = new GameClientController();
            try {
                // Stage is null for TUI mode, start() method should handle this.
                tuiApp.start(null);
            } catch (Exception e) {
                ROOT_LOGGER.log(Level.SEVERE, "Failed to start TUI mode from GameClientController.main", e);
            }
        } else {
            setUIMode(UIMode.GUI); // Ensure GUI mode if not TUI
            Application.launch(args); // Standard JavaFX launch
        }
    }
}