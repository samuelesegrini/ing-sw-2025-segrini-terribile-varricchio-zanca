package it.polimi.ingsw.client.ui.core;

import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default notification service implementation that logs messages to System.err.
 * Used as a fallback when a specific UI context is not yet available.
 */
public class DefaultNotificationService implements NotificationService {

    private static final Logger LOGGER = Logger.getLogger(DefaultNotificationService.class.getName());

    @Override
    public void showInfo(String title, String message) {
        LOGGER.log(Level.INFO, "[DEFAULT INFO] {0}: {1}", new Object[]{title, message});
    }

    @Override
    public void showSuccess(String title, String message) {
        LOGGER.log(Level.INFO, "[DEFAULT SUCCESS] {0}: {1}", new Object[]{title, message});
    }

    @Override
    public void showWarning(String title, String message) {
        LOGGER.log(Level.WARNING, "[DEFAULT WARNING] {0}: {1}", new Object[]{title, message});
    }

    @Override
    public void showError(String title, String message) {
        LOGGER.log(Level.SEVERE, "[DEFAULT ERROR] {0}: {1}", new Object[]{title, message});
    }

    @Override
    public void showLoading(String message) {
        LOGGER.log(Level.INFO, "[DEFAULT LOADING] {0}", message);
    }

    @Override
    public void hideLoading() {
        // No-op for default
    }

    @Override
    public void showNotification(Notification notification) {
        String prefix = switch (notification.getType()) {
            case INFO -> "[DEFAULT INFO] ";
            case WARNING -> "[DEFAULT WARNING] ";
            case ERROR -> "[DEFAULT ERROR] ";
            case SUCCESS -> "[DEFAULT SUCCESS] ";
            case CRITICAL -> "[DEFAULT CRITICAL] ";
        };
        LOGGER.log(Level.INFO, "{0}{1}: {2}", new Object[]{prefix, notification.getTitle(), notification.getMessage()});
    }

    @Override
    public boolean showConfirmation(String title, String message) {
        LOGGER.log(Level.INFO, "[DEFAULT CONFIRMATION] {0}: {1} (Defaulting to false)", new Object[]{title, message});
        return false; // Always return false for default confirmation
    }
}
