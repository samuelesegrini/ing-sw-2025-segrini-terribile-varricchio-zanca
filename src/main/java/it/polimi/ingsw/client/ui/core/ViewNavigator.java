package it.polimi.ingsw.client.ui.core;

import it.polimi.ingsw.client.core.ClientState;

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
    boolean navigateTo(ClientState.ViewState viewState);
    
    /**
     * Gets the current view state.
     */
    ClientState.ViewState getCurrentViewState();
    
    /**
     * Checks if navigation to a specific view is possible.
     */
    boolean canNavigateTo(ClientState.ViewState viewState);
    
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
        void onViewStateChanged(ClientState.ViewState oldState, ClientState.ViewState newState);
    }
}