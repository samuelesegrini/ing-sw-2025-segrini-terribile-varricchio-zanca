package it.polimi.ingsw.client.ui.core;

import it.polimi.ingsw.client.ClientModel;
import java.beans.PropertyChangeListener;

/**
 * Unified view interface for all UI implementations.
 * Defines the contract for views regardless of UI type (GUI/TUI).
 *
 * - initialize(UIContext context): Should only be called once per view instance.
 * - refresh(): Called when the model changes, to update the view.
 * - dispose(): Cleans up resources and listeners.
 */
public interface UIView extends PropertyChangeListener {
    
    /**
     * Gets the view state this view represents.
     */
    ClientModel.ViewState getViewState();
    
    /**
     * Gets a human-readable title for this view.
     */
    String getTitle();
    
    /**
     * Initializes the view with the given context. This should only be called once.
     */
    void initialize(UIContext context);
    
    /**
     * Shows/activates the view, making it visible to the user.
     */
    void show();
    
    /**
     * Hides/deactivates the view.
     */
    void hide();
    
    /**
     * Checks if the view is currently active/visible.
     */
    boolean isActive();
    
    /**
     * Updates the view with new data from the model.
     * This is typically called automatically when the model properties this view listens to change.
     */
    void refresh();
    
    /**
     * Cleans up resources when the view is disposed.
     */
    void dispose();
}