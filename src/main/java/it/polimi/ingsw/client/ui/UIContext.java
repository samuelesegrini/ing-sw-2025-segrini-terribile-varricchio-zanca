package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.ui.core.NotificationService;
import it.polimi.ingsw.client.ui.core.UIThreadService;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

/**
 * UI Context for dependency injection - provides UI components access to client state and services.
 * This replaces the singleton pattern with proper dependency injection.
 * Simple Direct Model Architecture: Only uses ClientState.
 */
public class UIContext {
    
    private final ClientState clientState;
    private final ClientController clientController;
    private final NotificationService notificationService;
    private final UIThreadService threadService;
    
    public UIContext(ClientState clientState, ClientController clientController,
                     NotificationService notificationService, UIThreadService threadService) {
        this.clientState = clientState;
        this.clientController = clientController;
        this.notificationService = notificationService;
        this.threadService = threadService;
    }
    
    /**
     * Get the client state containing all game and lobby data.
     * @return ClientState instance
     */
    public ClientState getClientState() {
        return clientState;
    }
    
    /**
     * Get the client controller for sending requests to server.
     * @return ClientController instance
     */
    public ClientController getClientController() {
        return clientController;
    }
    
    /**
     * Get the client controller (alias for compatibility).
     * @return ClientController instance
     */
    public ClientController getController() {
        return clientController;
    }
    
    
    /**
     * Get the notification service.
     * @return NotificationService instance
     */
    public NotificationService getNotificationService() {
        return notificationService;
    }
    
    /**
     * Get the UI thread service.
     * @return UIThreadService instance
     */
    public UIThreadService getThreadService() {
        return threadService;
    }
    
    // Convenience methods for common operations
    
    /**
     * Check if currently connected to server.
     * @return true if connected
     */
    public boolean isConnected() {
        return clientState.isConnected();
    }
    
    /**
     * Check if currently in a game.
     * @return true if in game
     */
    public boolean isInGame() {
        return clientState.isInGame();
    }
    
    /**
     * Get the local player ID.
     * @return player ID or null if not authenticated
     */
    public PlayerId getLocalPlayerId() {
        return clientState.getPlayerId();
    }
}