package it.polimi.ingsw.client.ui.core;

import it.polimi.ingsw.client.core.ClientState;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Default implementation of ViewNavigator.
 * Manages view state transitions and notifies listeners.
 */
public class ViewNavigatorImpl implements ViewNavigator {
    
    private static final Logger LOGGER = Logger.getLogger(ViewNavigatorImpl.class.getName());
    
    private final ClientState clientState;
    private final CopyOnWriteArrayList<ViewStateChangeListener> listeners = new CopyOnWriteArrayList<>();
    private ClientState.ViewState currentViewState;
    
    public ViewNavigatorImpl(ClientState clientState) {
        this.clientState = clientState;
        this.currentViewState = clientState.getCurrentView();
        
        // View state changes handled via direct navigation calls, no property listeners needed
    }
    
    @Override
    public boolean navigateTo(ClientState.ViewState viewState) {
        return navigateTo(viewState, null);
    }
    
    @Override
    public boolean navigateTo(ClientState.ViewState viewState, String context) {
        if (viewState == null) {
            LOGGER.warning("Cannot navigate to null view state");
            return false;
        }
        
        if (!canNavigateTo(viewState)) {
            String reason = getNavigationFailureReason(viewState);
            LOGGER.warning("Cannot navigate to view state: " + viewState + " from current state: " + currentViewState + 
                         (reason != null ? " - Reason: " + reason : ""));
            return false;
        }
        
        ClientState.ViewState oldState = currentViewState;
        
        try {
            // Update the client state and notify listeners directly
            clientState.setCurrentView(viewState);
            onViewStateChanged(oldState, viewState);
            currentViewState = viewState;
            
            String contextMsg = context != null ? " (Context: " + context + ")" : "";
            LOGGER.info("Successfully navigated from " + oldState + " to " + viewState + contextMsg);
            return true;
        } catch (Exception e) {
            LOGGER.severe("Failed to navigate to view state " + viewState + ": " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public ClientState.ViewState getCurrentViewState() {
        return currentViewState;
    }
    
    @Override
    public boolean canNavigateTo(ClientState.ViewState viewState) {
        return getNavigationFailureReason(viewState) == null;
    }
    
    @Override
    public String getNavigationFailureReason(ClientState.ViewState viewState) {
        if (viewState == null) {
            return "Target view state is null";
        }
        
        // Define navigation rules based on current state and model data
        switch (currentViewState) {
            case CONNECTION:
                // From connection, can go to login if connected, or directly to lobby if authenticated
                if (viewState == ClientState.ViewState.LOGIN) {
                    if (!clientState.isConnected()) {
                        return "Not connected to server";
                    }
                    return null; // Navigation allowed
                }
                if (viewState == ClientState.ViewState.LOBBY) {
                    if (!clientState.isConnected()) {
                        return "Not connected to server";
                    }
                    if (clientState.getPlayerId() == null) {
                        return "Not authenticated (no player ID)";
                    }
                    return null; // Navigation allowed
                }
                return "From CONNECTION view, can only navigate to LOGIN or LOBBY (if authenticated)";
                
            case LOGIN:
                // From login, can go to lobby if logged in
                if (viewState == ClientState.ViewState.LOBBY) {
                    if (clientState.getPlayerId() == null) {
                        return "Not authenticated (no player ID)";
                    }
                    return null; // Navigation allowed
                }
                return "From LOGIN view, can only navigate to LOBBY";
                
            case LOBBY:
                // From lobby, can go to game lobby or back to login
                if (viewState == ClientState.ViewState.GAME_LOBBY || viewState == ClientState.ViewState.LOGIN) {
                    return null; // Navigation allowed
                }
                return "From LOBBY view, can only navigate to GAME_LOBBY or LOGIN";
                       
            case GAME_LOBBY:
                // From game lobby, can go to game when it starts or back to lobby
                if (viewState == ClientState.ViewState.GAME) {
                    if (clientState.getCurrentGameId() == null) {
                        return "Not in a game (no game ID)";
                    }
                    return null; // Navigation allowed
                } else if (viewState == ClientState.ViewState.LOBBY) {
                    return null; // Navigation allowed
                }
                return "From GAME_LOBBY view, can only navigate to GAME or LOBBY";
                       
            case GAME:
                // From game, can go back to lobby or to login
                if (viewState == ClientState.ViewState.LOBBY || viewState == ClientState.ViewState.LOGIN) {
                    return null; // Navigation allowed
                }
                return "From GAME view, can only navigate to LOBBY or LOGIN";
                       
            default:
                // For unknown states, allow navigation
                return null;
        }
    }
    
    @Override
    public void addViewStateChangeListener(ViewStateChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    @Override
    public void removeViewStateChangeListener(ViewStateChangeListener listener) {
        listeners.remove(listener);
    }
    
    private void onViewStateChanged(ClientState.ViewState oldState, ClientState.ViewState newState) {
        LOGGER.info("View state changed from " + oldState + " to " + newState);
        
        for (ViewStateChangeListener listener : listeners) {
            try {
                listener.onViewStateChanged(oldState, newState);
            } catch (Exception e) {
                LOGGER.severe("Error notifying view state change listener: " + e.getMessage());
            }
        }
    }
}