package it.polimi.ingsw.client.ui;

/**
 * Base interface for all UI implementations.
 */
public interface UI {
    void start();
    void shutdown();
    boolean isRunning();
    void showError(String title, String message);
    void showInfo(String title, String message);
}