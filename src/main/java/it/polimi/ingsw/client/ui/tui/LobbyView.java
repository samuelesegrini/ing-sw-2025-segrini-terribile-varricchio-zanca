package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.common.message.request.CreateGameRequest;
import it.polimi.ingsw.server.model.enums.GameLevel;

import java.util.Scanner;

/**
 * TUI view for game lobby.
 */
public class LobbyView implements TuiView {
    private final ClientController controller;
    
    public LobbyView(ClientController controller) {
        this.controller = controller;
    }
    
    @Override
    public void display() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printHeader("Galaxy Trucker - Lobby");
        System.out.println("Available commands:");
        System.out.println("1. Create Game");
        System.out.println("2. Join Game");
        System.out.println("3. List Games");
        System.out.println("4. Quit");
        System.out.println();
    }
    
    @Override
    public void handleInput(String input) {
        switch (input.trim()) {
            case "1":
                handleCreateGame();
                break;
            case "2":
                ConsoleUtils.printInfo("Join game feature not implemented");
                break;
            case "3":
                ConsoleUtils.printInfo("Listing games...");
                // controller.listGames();
                break;
            case "4":
                ConsoleUtils.printInfo("Goodbye!");
                System.exit(0);
                break;
            default:
                ConsoleUtils.printError("Invalid command. Please enter 1-4.");
        }
    }
    
    private void handleCreateGame() {
        Scanner scanner = new Scanner(System.in);
        
        ConsoleUtils.printHeader("Create New Game");
        
        // Game Name
        System.out.print("Enter game name (or press Enter for 'New Game'): ");
        String gameName = scanner.nextLine().trim();
        if (gameName.isEmpty()) {
            gameName = "New Game";
        }
        
        // Max Players
        int maxPlayers = 4;
        while (true) {
            System.out.print("Enter max players (2-4) [default: 4]: ");
            String playersInput = scanner.nextLine().trim();
            if (playersInput.isEmpty()) {
                break; // Use default
            }
            try {
                maxPlayers = Integer.parseInt(playersInput);
                if (maxPlayers >= 2 && maxPlayers <= 4) {
                    break;
                } else {
                    ConsoleUtils.printError("Max players must be between 2 and 4.");
                }
            } catch (NumberFormatException e) {
                ConsoleUtils.printError("Please enter a valid number.");
            }
        }
        
        // Game Level
        GameLevel gameLevel = GameLevel.TEST_FLIGHT;
        while (true) {
            System.out.println();
            System.out.println("Select game level:");
            System.out.println("1. Test Flight (Beginner) - 8 simple adventure cards");
            System.out.println("2. Level II (Standard) - Mix of Level I and II cards with strategic planning");
            System.out.print("Enter choice (1-2) [default: 1]: ");
            
            String levelInput = scanner.nextLine().trim();
            if (levelInput.isEmpty() || levelInput.equals("1")) {
                gameLevel = GameLevel.TEST_FLIGHT;
                break;
            } else if (levelInput.equals("2")) {
                gameLevel = GameLevel.LEVEL_II;
                break;
            } else {
                ConsoleUtils.printError("Please enter 1 or 2.");
            }
        }
        
        // Create and send request
        CreateGameRequest request = new CreateGameRequest(maxPlayers, gameLevel, gameName);
        
        System.out.println();
        ConsoleUtils.printInfo("Creating game: " + gameName + 
                              " (Level: " + gameLevel + 
                              ", Max Players: " + maxPlayers + ")");
        
        controller.sendRequest(request);
    }
    
    @Override
    public String getPrompt() {
        return "Select option (1-4): ";
    }
    
    @Override
    public void refresh() {
        display();
    }
}