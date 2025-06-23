package it.polimi.ingsw.client.ui.tui;

import java.util.Scanner;

/**
 * Manages user input for the TUI interface.
 */
public class UserInputManager {
    private final Scanner scanner;
    private final Runnable inputCallback;
    
    public UserInputManager() {
        this.scanner = new Scanner(System.in);
        this.inputCallback = null;
    }
    
    public UserInputManager(Runnable inputCallback) {
        this.scanner = new Scanner(System.in);
        this.inputCallback = inputCallback;
    }
    
    public String readLine() {
        return scanner.nextLine();
    }
    
    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }
    
    public int readInt(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.println("Please enter a valid number.");
            System.out.print(prompt);
            scanner.next();
        }
        int result = scanner.nextInt();
        scanner.nextLine(); // consume newline
        return result;
    }
    
    public boolean readBoolean(String prompt) {
        String input = readLine(prompt + " (y/n): ");
        return input.toLowerCase().startsWith("y");
    }
    
    public void close() {
        scanner.close();
    }
}