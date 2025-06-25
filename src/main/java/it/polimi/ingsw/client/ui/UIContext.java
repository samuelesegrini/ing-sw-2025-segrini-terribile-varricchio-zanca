package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.controller.ClientController;

/**
 * UI Context for dependency injection - provides UI components access to client state and services.
 * This replaces the singleton pattern with proper dependency injection.
 */
public class UIContext {
    
    private final ClientState clientState;
    private final ClientController clientController;
    
    public UIContext(ClientState clientState, ClientController clientController) {
        this.clientState = clientState;
        this.clientController = clientController;
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
    public String getLocalPlayerId() {
        return clientState.getPlayerId();
    }
}