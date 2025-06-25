package it.polimi.ingsw.client.core;

/**
 * Interface for UI components that can be refreshed when server models are updated.
 * This replaces the complex PropertyChangeListener pattern with a simple refresh mechanism.
 */
public interface UIRefreshable {
    
    /**
     * Refresh the UI component to reflect current server model state.
     * Called automatically when server models are updated.
     */
    void refresh();
}