package it.polimi.ingsw.client.ui.core;

import it.polimi.ingsw.client.ClientModel;

/**
 * Unified view navigation interface for all UI implementations.
 * Manages view transitions and state in a consistent way.
 */
public interface ViewNavigator {
    
    /**
     * Navigates to a specific view state.
     * @param viewState The target view state
     * @return true if navigation was successful, false otherwise
     */
    boolean navigateTo(ClientModel.ViewState viewState);
    
    /**
     * Gets the current view state.
     */
    ClientModel.ViewState getCurrentViewState();
    
    /**
     * Checks if navigation to a specific view is possible.
     */
    boolean canNavigateTo(ClientModel.ViewState viewState);
    
    /**
     * Registers a view state change listener.
     */
    void addViewStateChangeListener(ViewStateChangeListener listener);
    
    /**
     * Removes a view state change listener.
     */
    void removeViewStateChangeListener(ViewStateChangeListener listener);
    
    /**
     * Callback interface for view state changes.
     */
    interface ViewStateChangeListener {
        void onViewStateChanged(ClientModel.ViewState oldState, ClientModel.ViewState newState);
    }
}