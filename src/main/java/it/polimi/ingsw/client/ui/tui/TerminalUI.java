package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.core.GameClientController;
import it.polimi.ingsw.client.model.ClientViewModel;
import it.polimi.ingsw.client.ui.AbstractUserInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Terminal-based implementation of UserInterface
 */
public class TerminalUI extends AbstractUserInterface {
    private static final Logger LOGGER = Logger.getLogger(TerminalUI.class.getName());
    
    private ExecutorService inputExecutor;
    private boolean running = false;
    private BufferedReader reader;
    
    @Override
    public void initialize() {
        reader = new BufferedReader(new InputStreamReader(System.in));
        inputExecutor = Executors.newSingleThreadExecutor();
        
        // Print welcome message
        System.out.println("=======================================================");
        System.out.println("         Welcome to Galaxy Trucker - Terminal Mode     ");
        System.out.println("=======================================================");
    }
    
    @Override
    public void start() {
        running = true;
        inputExecutor.submit(this::inputLoop);
    }
    
    @Override
    protected void configureViewModelListeners() {
        // Set up listeners for ViewModel changes
        if (viewModel != null) {
            viewModel.appStatusProperty().addListener((obs, oldStatus, newStatus) -> {
                LOGGER.info("ViewModel status changed: " + oldStatus + " -> " + newStatus);
                handleAppStatusChange(newStatus);
            });
            
            viewModel.statusMessageProperty().addListener((obs, oldMsg, newMsg) -> {
                System.out.println("\n[STATUS] " + newMsg);
            });
        }
    }
    
    private void inputLoop() {
        try {
            while (running) {
                // Show prompt based on current state
                showPromptForCurrentState();
                
                // Read input
                String input = reader.readLine();
                if (input == null || "exit".equalsIgnoreCase(input.trim())) {
                    running = false;
                    if (clientController != null) {
                        clientController.requestShutdown();
                    }
                    break;
                }
                
                // Process input based on current state
                processInput(input);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading from console", e);
            showError("Input Error", "Failed to read from console: " + e.getMessage());
        } finally {
            // Ensure shutdown if the loop exits for any reason
            if (clientController != null) {
                clientController.requestShutdown();
            }
        }
    }
    
    private void showPromptForCurrentState() {
        if (viewModel == null) {
            System.out.print("\n> ");
            return;
        }
        
        ClientViewModel.AppStatus status = viewModel.appStatusProperty().get();
        switch (status) {
            case NOT_CONNECTED:
            case LOGIN_SCREEN:
                System.out.println("\nEnter connection details in format: <host> <port> <nickname> <technology>");
                System.out.println("Example: localhost 1234 Player1 Socket");
                System.out.print("\n> ");
                break;
                
            case CONNECTING:
            case LOGGING_IN:
                System.out.println("\nConnecting to server... please wait.");
                System.out.print("\n> ");
                break;
                
            case LOGGED_IN_BROWSING_LOBBIES:
                System.out.println("\nGame Browser Options:");
                System.out.println("  list - List available games");
                System.out.println("  create <n> <maxPlayers> - Create a new game");
                System.out.println("  join <gameId> - Join an existing game");
                System.out.println("  refresh - Refresh game list");
                System.out.print("\n> ");
                break;
                
            case GAME_LOBBY:
                System.out.println("\nLobby Options:");
                System.out.println("  players - List players in lobby");
                System.out.println("  start - Start the game (host only)");
                System.out.println("  leave - Leave the lobby");
                System.out.print("\n> ");
                break;
                
            default:
                System.out.println("\n[" + status + "] Enter command:");
                System.out.print("\n> ");
                break;
        }
    }
    
    private void processInput(String input) {
        if (viewModel == null || input == null || input.trim().isEmpty()) {
            return;
        }
        
        String[] parts = input.trim().split("\\s+");
        ClientViewModel.AppStatus status = viewModel.appStatusProperty().get();
        
        switch (status) {
            case NOT_CONNECTED:
            case LOGIN_SCREEN:
                if (parts.length >= 4) {
                    String host = parts[0];
                    int port;
                    try {
                        port = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        showError("Invalid Input", "Port must be a number");
                        return;
                    }
                    String nickname = parts[2];
                    String technology = parts[3];
                    
                    // Call the connect method
                    if (clientController != null) {
                        clientController.setupNetworkAndConnect(host, port, nickname, technology);
                    }
                } else {
                    showError("Invalid Input", "Required format: <host> <port> <nickname> <technology>");
                }
                break;
                
            // Add other state handlers here
            case LOGGED_IN_BROWSING_LOBBIES:
                processLobbyBrowserCommand(parts);
                break;
                
            case GAME_LOBBY:
                processGameLobbyCommand(parts);
                break;
                
            default:
                System.out.println("Command not recognized for current state: " + status);
                break;
        }
    }
    
    private void processLobbyBrowserCommand(String[] parts) {
        if (parts.length == 0) return;
        
        String command = parts[0].toLowerCase();
        switch (command) {
            case "list":
                showGameList();
                break;
                
            case "create":
                if (parts.length >= 3) {
                    try {
                        String name = parts[1];
                        int maxPlayers = Integer.parseInt(parts[2]);
                        // TODO: Implement create game functionality
                    } catch (NumberFormatException e) {
                        showError("Invalid Input", "Max players must be a number");
                    }
                } else {
                    showError("Invalid Input", "Required format: create <n> <maxPlayers>");
                }
                break;
                
            case "join":
                if (parts.length >= 2) {
                    String sessionId = parts[1];
                    // TODO: Implement join game functionality
                } else {
                    showError("Invalid Input", "Required format: join <gameId>");
                }
                break;
                
            case "refresh":
                // TODO: Implement refresh game list functionality
                break;
                
            default:
                System.out.println("Unknown command: " + command);
                break;
        }
    }
    
    private void processGameLobbyCommand(String[] parts) {
        if (parts.length == 0) return;
        
        String command = parts[0].toLowerCase();
        switch (command) {
            case "players":
                // TODO: Show players in current lobby
                break;
                
            case "start":
                // TODO: Implement start game functionality (host only)
                break;
                
            case "leave":
                // TODO: Implement leave lobby functionality
                break;
                
            default:
                System.out.println("Unknown command: " + command);
                break;
        }
    }
    
    private void showGameList() {
        // TODO: Implement showing the game list
        System.out.println("\nAvailable Games:");
        System.out.println("----------------------------------");
        // Placeholder for actual game list implementation
        System.out.println("No games available.");
        System.out.println("----------------------------------");
    }
    
    @Override
    public void handleAppStatusChange(ClientViewModel.AppStatus newStatus) {
        LOGGER.info("Terminal UI handling state change to: " + newStatus);
        // Clear the screen for better user experience
        clearScreen();
        
        // Show status message based on new state
        System.out.println("\n=== Status: " + newStatus + " ===");
        if (viewModel != null && viewModel.statusMessageProperty().get() != null) {
            System.out.println(viewModel.statusMessageProperty().get());
        }
        
        // Show appropriate prompt for the new state
        showPromptForCurrentState();
    }
    
    private void clearScreen() {
        // Simple clear screen implementation (not perfect but works in most terminals)
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
    
    @Override
    public void showConnectionPrompt() {
        if (viewModel != null) {
            viewModel.setAppStatus(ClientViewModel.AppStatus.LOGIN_SCREEN);
        }
    }
    
    @Override
    public void showError(String title, String message) {
        System.out.println("\n[ERROR] " + title + ": " + message);
    }
    
    @Override
    public void showInfo(String title, String message) {
        System.out.println("\n[INFO] " + title + ": " + message);
    }
    
    @Override
    public void shutdown() {
        super.shutdown();
        running = false;
        
        if (inputExecutor != null && !inputExecutor.isShutdown()) {
            LOGGER.info("Shutting down terminal input executor");
            inputExecutor.shutdown();
        }
        
        // Close any open resources
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error closing reader during shutdown", e);
            }
        }
    }
} 