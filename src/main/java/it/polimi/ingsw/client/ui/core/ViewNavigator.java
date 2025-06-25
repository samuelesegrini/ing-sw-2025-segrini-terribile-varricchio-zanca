package it.polimi.ingsw.client.ui.core;

import it.polimi.ingsw.client.core.ClientState;

/**
 * Unified view navigation interface for all UI implementations.
 * Manages view transitions and state in a consistent way.
 * 
 * Design Pattern: Single Source of Truth for View Navigation
 * - All view changes MUST go through ViewNavigator.navigateTo()
 * - Direct ClientState.setCurrentView() should be prohibited in application code
 * - ViewNavigator handles validation, state updates, and notifications
 */
public interface ViewNavigator {
    
    /**
     * Navigates to a specific view state.
     * This is the ONLY method that should be used for view transitions.
     * 
     * @param viewState The target view state
     * @return true if navigation was successful, false otherwise
     */
    boolean navigateTo(ClientState.ViewState viewState);
    
    /**
     * Navigates to a specific view state with context information.
     * Useful for providing additional information about why the navigation occurred.
     * 
     * @param viewState The target view state
     * @param context Additional context information for the navigation
     * @return true if navigation was successful, false otherwise
     */
    boolean navigateTo(ClientState.ViewState viewState, String context);
    
    /**
     * Gets the current view state.
     */
    ClientState.ViewState getCurrentViewState();
    
    /**
     * Checks if navigation to a specific view is possible.
     * This method performs all the validation logic.
     */
    boolean canNavigateTo(ClientState.ViewState viewState);
    
    /**
     * Gets a human-readable reason why navigation to a view state is not possible.
     * Returns null if navigation is possible.
     */
    String getNavigationFailureReason(ClientState.ViewState viewState);
    
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
    
    /**
     * Navigation result with context information.
     */
    class NavigationResult {
        private final boolean success;
        private final String reason;
        private final ClientState.ViewState fromState;
        private final ClientState.ViewState toState;
        
        public NavigationResult(boolean success, String reason, ClientState.ViewState fromState, ClientState.ViewState toState) {
            this.success = success;
            this.reason = reason;
            this.fromState = fromState;
            this.toState = toState;
        }
        
        public boolean isSuccess() { return success; }
        public String getReason() { return reason; }
        public ClientState.ViewState getFromState() { return fromState; }
        public ClientState.ViewState getToState() { return toState; }
    }
}