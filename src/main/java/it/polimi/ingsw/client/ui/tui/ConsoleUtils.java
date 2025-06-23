package it.polimi.ingsw.client.ui.tui;

/**
 * Utility methods for console output and formatting.
 */
public class ConsoleUtils {
    
    public static void clearScreen() {
        // ANSI escape sequence to clear screen
        System.out.print("\033[2J\033[H");
        System.out.flush();
    }
    
    public static void printHeader(String title) {
        System.out.println("=".repeat(50));
        System.out.println(" ".repeat((50 - title.length()) / 2) + title);
        System.out.println("=".repeat(50));
    }
    
    public static void printSeparator() {
        System.out.println("-".repeat(50));
    }
    
    public static void printError(String message) {
        System.out.println("[ERROR] " + message);
    }
    
    public static void printInfo(String message) {
        System.out.println("[INFO] " + message);
    }
    
    public static void printWarning(String message) {
        System.out.println("[WARNING] " + message);
    }
    
    public static void waitForEnter() {
        System.out.println("Press Enter to continue...");
        try {
            System.in.read();
        } catch (Exception e) {
            // Ignore
        }
    }
}