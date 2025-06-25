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
        
        // Listen to client state changes to keep in sync
        clientState.addPropertyChangeListener(evt -> {
            if ("currentView".equals(evt.getPropertyName())) {
                ClientState.ViewState newState = (ClientState.ViewState) evt.getNewValue();
                onViewStateChanged(currentViewState, newState);
                currentViewState = newState;
            }
        });
    }
    
    @Override
    public boolean navigateTo(ClientState.ViewState viewState) {
        if (viewState == null) {
            LOGGER.warning("Cannot navigate to null view state");
            return false;
        }
        
        if (!canNavigateTo(viewState)) {
            LOGGER.warning("Cannot navigate to view state: " + viewState + " from current state: " + currentViewState);
            return false;
        }
        
        ClientState.ViewState oldState = currentViewState;
        
        try {
            // Update the client state, which will trigger property change events
            clientState.setCurrentView(viewState);
            LOGGER.info("Successfully navigated from " + oldState + " to " + viewState);
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
        if (viewState == null) {
            return false;
        }
        
        // Define navigation rules based on current state and model data
        switch (currentViewState) {
            case CONNECTION:
                // From connection, can only go to login if connected
                return viewState == ClientState.ViewState.LOGIN && clientState.isConnected();
                
            case LOGIN:
                // From login, can go to lobby if logged in
                return viewState == ClientState.ViewState.LOBBY && (clientState.getPlayerId() != null);
                
            case LOBBY:
                // From lobby, can go to game lobby or back to login
                return viewState == ClientState.ViewState.GAME_LOBBY ||
                       viewState == ClientState.ViewState.LOGIN;
                       
            case GAME_LOBBY:
                // From game lobby, can go to game when it starts or back to lobby
                return (viewState == ClientState.ViewState.GAME && clientState.getCurrentGameId() != null) ||
                       viewState == ClientState.ViewState.LOBBY;
                       
            case GAME:
                // From game, can go back to lobby or to login
                return viewState == ClientState.ViewState.LOBBY || 
                       viewState == ClientState.ViewState.LOGIN;
                       
            default:
                // For unknown states, allow navigation
                return true;
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