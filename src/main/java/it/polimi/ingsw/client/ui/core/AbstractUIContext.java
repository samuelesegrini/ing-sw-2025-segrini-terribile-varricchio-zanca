package it.polimi.ingsw.client.ui.core;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.ClientModel;

/**
 * Abstract base implementation of UIContext.
 * Provides common functionality for all UI implementations.
 */
public abstract class AbstractUIContext implements UIContext {
    
    protected final ClientController controller;
    protected final ClientModel model;
    protected final NotificationService notificationService;
    protected final ViewNavigator viewNavigator;
    protected final UIThreadService threadService;
    
    protected AbstractUIContext(ClientController controller) {
        this.controller = controller;
        this.model = controller.getModel();
        this.notificationService = createNotificationService();
        this.viewNavigator = createViewNavigator();
        this.threadService = createThreadService();
    }
    
    @Override
    public ClientController getController() {
        return controller;
    }
    
    @Override
    public ClientModel getModel() {
        return model;
    }
    
    @Override
    public NotificationService getNotificationService() {
        return notificationService;
    }
    
    @Override
    public ViewNavigator getViewNavigator() {
        return viewNavigator;
    }
    
    @Override
    public UIThreadService getThreadService() {
        return threadService;
    }
    
    /**
     * Factory method for creating the notification service.
     * Subclasses override this to provide UI-specific implementations.
     */
    protected abstract NotificationService createNotificationService();
    
    /**
     * Factory method for creating the view navigator.
     * Subclasses override this to provide UI-specific implementations.
     */
    protected abstract ViewNavigator createViewNavigator();
    
    /**
     * Factory method for creating the thread service.
     * Subclasses override this to provide UI-specific implementations.
     */
    protected abstract UIThreadService createThreadService();
}