package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.ui.core.NotificationService;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import java.util.Scanner;

/**
 * TUI-specific notification service implementation.
 */
public class TuiNotificationService implements NotificationService {
    
    private final TuiConsole console;
    private final Scanner scanner = new Scanner(System.in);
    
    public TuiNotificationService(TuiConsole console) {
        this.console = console;
    }
    
    @Override
    public void showInfo(String title, String message) {
        console.println("[INFO] " + title);
        if (message != null && !message.isEmpty()) {
            console.println("  " + message);
        }
    }
    
    @Override
    public void showSuccess(String title, String message) {
        console.println("[SUCCESS] " + title);
        if (message != null && !message.isEmpty()) {
            console.println("  " + message);
        }
    }
    
    @Override
    public void showWarning(String title, String message) {
        console.println("[WARNING] " + title);
        if (message != null && !message.isEmpty()) {
            console.println("  " + message);
        }
    }
    
    @Override
    public void showError(String title, String message) {
        console.println("[ERROR] " + title);
        if (message != null && !message.isEmpty()) {
            console.println("  " + message);
        }
    }
    
    @Override
    public void showLoading(String message) {
        console.println("[LOADING] " + message);
    }
    
    @Override
    public void hideLoading() {
        // No-op for TUI
    }
    
    @Override
    public void showNotification(Notification notification) {
        if (console == null) {
            System.err.println("[TUI ERROR] Console is null - cannot show notification: " + notification.getTitle());
            return;
        }
        
        String prefix = switch (notification.getType()) {
            case INFO -> "[INFO] ";
            case WARNING -> "[WARNING] ";
            case ERROR -> "[ERROR] ";
            case SUCCESS -> "[SUCCESS] ";
        };
        
        console.println(prefix + notification.getTitle());
        if (notification.getMessage() != null && !notification.getMessage().isEmpty()) {
            console.println("  " + notification.getMessage());
        }
    }
    
    @Override
    public boolean showConfirmation(String title, String message) {
        console.println("[CONFIRM] " + title);
        if (message != null && !message.isEmpty()) {
            console.println("  " + message);
        }
        System.out.print("Continue? (y/n): ");
        String response = scanner.nextLine().trim().toLowerCase();
        return response.startsWith("y");
    }
}