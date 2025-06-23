package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.controller.ClientController;

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
                ConsoleUtils.printInfo("Creating game...");
                // controller.createGame();
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
    
    @Override
    public String getPrompt() {
        return "Select option (1-4): ";
    }
    
    @Override
    public void refresh() {
        display();
    }
}