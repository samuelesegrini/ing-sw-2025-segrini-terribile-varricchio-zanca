package it.polimi.ingsw.client.ui.core;

import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;

/**
 * Unified notification service interface for all UI implementations.
 * Provides consistent notification handling across GUI and TUI.
 */
public interface NotificationService {
    
    /**
     * Shows an informational message to the user.
     */
    void showInfo(String title, String message);
    
    /**
     * Shows a success message to the user.
     */
    void showSuccess(String title, String message);
    
    /**
     * Shows a warning message to the user.
     */
    void showWarning(String title, String message);
    
    /**
     * Shows an error message to the user.
     */
    void showError(String title, String message);
    
    /**
     * Shows a loading message to the user.
     */
    void showLoading(String message);
    
    /**
     * Hides the current loading message.
     */
    void hideLoading();
    
    /**
     * Shows a custom notification.
     */
    void showNotification(Notification notification);
    
    /**
     * Shows a confirmation dialog and returns the user's choice.
     * Returns true if user confirms, false otherwise.
     */
    boolean showConfirmation(String title, String message);
}