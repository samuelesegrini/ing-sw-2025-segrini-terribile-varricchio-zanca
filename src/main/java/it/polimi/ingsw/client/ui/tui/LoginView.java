package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.controller.ClientController;

/**
 * TUI view for player login.
 */
public class LoginView implements TuiView {
    private final ClientController controller;
    
    public LoginView(ClientController controller) {
        this.controller = controller;
    }
    
    @Override
    public void display() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printHeader("Galaxy Trucker - Login");
        System.out.println("Enter your nickname to join the game:");
        System.out.println();
    }
    
    @Override
    public void handleInput(String input) {
        String nickname = input.trim();
        if (!nickname.isEmpty()) {
            // controller.loginWithNickname(nickname);
        } else {
            ConsoleUtils.printError("Nickname cannot be empty");
        }
    }
    
    @Override
    public String getPrompt() {
        return "Nickname: ";
    }
    
    @Override
    public void refresh() {
        display();
    }
}