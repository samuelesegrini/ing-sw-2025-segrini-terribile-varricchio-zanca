package it.polimi.ingsw.client.core;

import it.polimi.ingsw.client.network.NetworkClient;
import it.polimi.ingsw.client.ui.UIManager;
import it.polimi.ingsw.client.ui.UIType;
import it.polimi.ingsw.client.controller.ClientController;

import java.io.InputStream;
import java.util.Scanner;
import java.util.logging.*;
import java.util.Arrays;

/**
 * Main client application entry point.
 * Handles initialization and UI type selection.
 */
public class ClientApp {
    private static final Logger LOGGER = Logger.getLogger(ClientApp.class.getName());

    private NetworkClient networkClient;
    private ClientState clientState;
    private ClientController controller;
    private UIManager uiManager;

    public static void main(String[] args) {
        // --- IMPORTANT: Load logging configuration FIRST ---
        try (InputStream is = ClientApp.class.getResourceAsStream("/client_logging.properties")) {
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
            } else {
                // This warning will go to System.err, not the configured logger,
                // as logging might not be fully set up yet.
                System.err.println("WARNING: client_logging.properties not found. Default JUL logging will be used.");
            }
        } catch (Exception e) {
            System.err.println("ERROR loading client logging configuration: " + e.getMessage());
            e.printStackTrace();
        }

        // Now, any Logger.getLogger() calls will use the configuration from client_logging.properties
        LOGGER.info("ClientApp started. Logging configured.");

        UIType uiType = parseUIType(args);
        if (uiType == null) {
            printUsage();
            System.exit(1);
        }

        ClientApp app = new ClientApp();
        app.start(uiType);
    }
    

    public void start(UIType uiType) {
        LOGGER.info("Starting Galaxy Trucker Client with " + uiType + " interface");

        try {
            // Initialize components following Simple Direct Model Architecture
            clientState = new ClientState();
            networkClient = new NetworkClient();
            
            // Create controller without UIContext first
            controller = new ClientController(networkClient, clientState);
            uiManager = new UIManager(uiType, controller);

            // Set up callbacks
            networkClient.setMessageHandler(controller::handleMessage);

            // Start UI
            uiManager.start();

            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            LOGGER.info("Client started successfully");
        } catch (Exception e) {
            LOGGER.severe("Failed to start client: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static UIType parseUIType(String[] args) {
        if (args.length == 0) {
            return selectUITypeInteractively();
        }

        return Arrays.stream(args)
                .filter(arg -> arg.startsWith("--"))
                .map(arg -> arg.substring(2).toLowerCase())
                .filter(arg -> "gui".equals(arg) || "tui".equals(arg))
                .map(arg -> "gui".equals(arg) ? UIType.GUI : UIType.TUI)
                .findFirst()
                .orElse(null);
    }

    private static UIType selectUITypeInteractively() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=================================");
        System.out.println("    GALAXY TRUCKER CLIENT");
        System.out.println("=================================");
        System.out.println();
        System.out.println("Select interface type:");
        System.out.println("1. Graphical User Interface (GUI)");
        System.out.println("2. Text User Interface (TUI)");
        System.out.print("\nChoice (1 or 2): ");

        while (true) {
            String input = scanner.nextLine().trim();

            if ("1".equals(input)) {
                return UIType.GUI;
            } else if ("2".equals(input)) {
                return UIType.TUI;
            } else {
                System.out.print("Invalid choice. Please enter 1 or 2: ");
            }
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java ClientApp [--gui|--tui]");
        System.out.println("  --gui  : Start with Graphical User Interface");
        System.out.println("  --tui  : Start with Text User Interface");
        System.out.println("  (no args): Interactive selection");
    }

    public void shutdown() {
        LOGGER.info("Shutting down client");

        if (networkClient != null) {
            networkClient.disconnect();
        }

        if (uiManager != null && uiManager.isRunning()) {
            uiManager.shutdown();
        }
    }
}