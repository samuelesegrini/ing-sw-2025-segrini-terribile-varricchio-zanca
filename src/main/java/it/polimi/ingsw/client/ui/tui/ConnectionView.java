package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.controller.ClientController;

/**
 * TUI view for server connection.
 */
public class ConnectionView implements TuiView {
    private final ClientController controller;
    
    public ConnectionView(ClientController controller) {
        this.controller = controller;
    }
    
    @Override
    public void display() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printHeader("Galaxy Trucker - Connection");
        System.out.println("Enter server connection details:");
        System.out.println();
    }
    
    @Override
    public void handleInput(String input) {
        // Parse connection input (host:port)
        String[] parts = input.split(":");
        if (parts.length == 2) {
            String host = parts[0].trim();
            try {
                int port = Integer.parseInt(parts[1].trim());
                // controller.connectToServer(host, port);
            } catch (NumberFormatException e) {
                ConsoleUtils.printError("Invalid port number");
            }
        } else {
            ConsoleUtils.printError("Please enter in format: host:port");
        }
    }
    
    @Override
    public String getPrompt() {
        return "Enter server (host:port): ";
    }
    
    @Override
    public void refresh() {
        display();
    }
}