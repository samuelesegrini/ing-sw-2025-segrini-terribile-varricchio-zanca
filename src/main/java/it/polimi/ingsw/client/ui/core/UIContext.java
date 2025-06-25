package it.polimi.ingsw.client.ui.core;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.ClientState;

/**
 * UI Context provides access to core components for all UI implementations.
 * This is the single source of truth for UI-related dependencies.
 */
public interface UIContext {
    
    /**
     * Gets the client controller for handling user actions.
     */
    ClientController getController();
    
    /**
     * Gets the client state for data access.
     */
    ClientState getClientState();
    
    /**
     * Gets the notification service for user feedback.
     */
    NotificationService getNotificationService();
    
    /**
     * Gets the view navigator for managing view transitions.
     */
    ViewNavigator getViewNavigator();
    
    /**
     * Gets the threading service for UI thread management.
     */
    UIThreadService getThreadService();
}