package it.polimi.ingsw.client.ui.tui;

/**
 * Simple console utilities for basic TUI operations.
 */
public class SimpleConsoleUtils {
    
    public static void println(String message) {
        System.out.println(message);
    }
    
    public static void print(String message) {
        System.out.print(message);
    }
    
    public static void printLine() {
        System.out.println();
    }
    
    public static void printBorder() {
        System.out.println("----------------------------------------");
    }
    
    public static void printTitle(String title) {
        printBorder();
        System.out.println(title);
        printBorder();
    }
}