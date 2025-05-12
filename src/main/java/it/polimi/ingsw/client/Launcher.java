package it.polimi.ingsw.client;

import it.polimi.ingsw.client.core.GameClientController;
import java.util.Scanner;

/**
 * Main entry point for the Galaxy Trucker game client application.
 * Handles UI mode selection (GUI or TUI) before starting the appropriate client interface.
 */
public class Launcher {
    
    /**
     * Application entry point that processes command-line arguments
     * and determines which UI mode to start.
     * 
     * @param args Command line arguments (--gui/-g for GUI mode, --tui/-t for TUI mode)
     */
    public static void main(String[] args) {
        boolean hasUIChoice = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--gui") || arg.equalsIgnoreCase("-g")) {
                GameClientController.setUIMode(GameClientController.UIMode.GUI);
                hasUIChoice = true;
            } else if (arg.equalsIgnoreCase("--tui") || arg.equalsIgnoreCase("-t")) {
                GameClientController.setUIMode(GameClientController.UIMode.TUI);
                hasUIChoice = true;
            }
        }
        
        if (!hasUIChoice) {
            promptForUIMode();
        }
        
        if (GameClientController.getUIMode() == GameClientController.UIMode.GUI) {
            try {
                GameClientController.main(args);
            } catch (Exception e) {
                System.err.println("Error starting GUI: " + e.getMessage());
                e.printStackTrace();
                
                System.out.println("Falling back to TUI mode...");
                GameClientController.setUIMode(GameClientController.UIMode.TUI);
                startTUIMode(args);
            }
        } else {
            startTUIMode(args);
        }
    }
    
    /**
     * Starts the application in Terminal User Interface mode.
     * 
     * @param args Command line arguments to pass to the application
     */
    private static void startTUIMode(String[] args) {
        try {
            GameClientController app = new GameClientController();
            app.start(null);
        } catch (Exception e) {
            System.err.println("Error starting TUI: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Prompts the user to select a UI mode when none is specified in command-line arguments.
     * Presents a simple console menu for choosing between GUI and TUI.
     */
    private static void promptForUIMode() {
        System.out.println("======================================");
        System.out.println("  Galaxy Trucker - Client Application ");
        System.out.println("======================================");
        System.out.println("Select user interface mode:");
        System.out.println("1. Graphical User Interface (GUI)");
        System.out.println("2. Terminal User Interface (TUI)");
        System.out.print("\nEnter choice (1/2): ");
        
        Scanner scanner = new Scanner(System.in);
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "2":
                System.out.println("Starting in Terminal mode...");
                GameClientController.setUIMode(GameClientController.UIMode.TUI);
                break;
                
            case "1":
            default:
                System.out.println("Starting in Graphical mode...");
                GameClientController.setUIMode(GameClientController.UIMode.GUI);
                break;
        }
        
        // Scanner is not closed to avoid closing System.in
    }
} 