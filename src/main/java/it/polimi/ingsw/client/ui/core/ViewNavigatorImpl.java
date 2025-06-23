package it.polimi.ingsw.client.ui.core;

import it.polimi.ingsw.client.ClientModel;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Default implementation of ViewNavigator.
 * Manages view state transitions and notifies listeners.
 */
public class ViewNavigatorImpl implements ViewNavigator {
    
    private static final Logger LOGGER = Logger.getLogger(ViewNavigatorImpl.class.getName());
    
    private final ClientModel model;
    private final CopyOnWriteArrayList<ViewStateChangeListener> listeners = new CopyOnWriteArrayList<>();
    private ClientModel.ViewState currentViewState;
    
    public ViewNavigatorImpl(ClientModel model) {
        this.model = model;
        this.currentViewState = model.getCurrentView();
        
        // Listen to model changes to keep in sync
        model.addPropertyChangeListener(evt -> {
            if ("currentView".equals(evt.getPropertyName())) {
                ClientModel.ViewState newState = (ClientModel.ViewState) evt.getNewValue();
                onViewStateChanged(currentViewState, newState);
                currentViewState = newState;
            }
        });
    }
    
    @Override
    public boolean navigateTo(ClientModel.ViewState viewState) {
        if (viewState == null) {
            LOGGER.warning("Cannot navigate to null view state");
            return false;
        }
        
        if (!canNavigateTo(viewState)) {
            LOGGER.warning("Cannot navigate to view state: " + viewState + " from current state: " + currentViewState);
            return false;
        }
        
        ClientModel.ViewState oldState = currentViewState;
        
        try {
            // Update the model, which will trigger property change events
            model.setCurrentView(viewState);
            LOGGER.info("Successfully navigated from " + oldState + " to " + viewState);
            return true;
        } catch (Exception e) {
            LOGGER.severe("Failed to navigate to view state " + viewState + ": " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public ClientModel.ViewState getCurrentViewState() {
        return currentViewState;
    }
    
    @Override
    public boolean canNavigateTo(ClientModel.ViewState viewState) {
        if (viewState == null) {
            return false;
        }
        
        // Define navigation rules based on current state and model data
        switch (currentViewState) {
            case CONNECTION:
                // From connection, can only go to login if connected
                return viewState == ClientModel.ViewState.LOGIN && model.isConnected();
                
            case LOGIN:
                // From login, can go to lobby if logged in
                return viewState == ClientModel.ViewState.LOBBY && model.isLoggedIn();
                
            case LOBBY:
                // From lobby, can go to game or back to login
                return (viewState == ClientModel.ViewState.GAME && model.getCurrentGameId() != null) ||
                       viewState == ClientModel.ViewState.LOGIN;
                       
            case GAME:
                // From game, can go back to lobby or to login
                return viewState == ClientModel.ViewState.LOBBY || 
                       viewState == ClientModel.ViewState.LOGIN;
                       
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
    
    private void onViewStateChanged(ClientModel.ViewState oldState, ClientModel.ViewState newState) {
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