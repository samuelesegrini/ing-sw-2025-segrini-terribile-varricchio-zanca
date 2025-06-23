package it.polimi.ingsw.common.message;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.state.LocalGameState;
import it.polimi.ingsw.client.network.NetworkClient;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.client.ui.core.NotificationService;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.response.ClientContext;

/**
 * Unified context interface that provides all necessary client resources
 * for message handling. This combines ClientContext and ClientEventContext
 * to provide a single, comprehensive interface for all message types.
 */
public interface ClientMessageContext extends ClientContext, ClientEventContext {
    
    /**
     * Gets the network client for sending messages back to the server.
     * @return The network client
     */
    NetworkClient getNetworkClient();
    
    /**
     * Gets the client controller for performing actions.
     * @return The client controller
     */
    @Override
    ClientController getController();
    
    /**
     * Gets the client model for accessing and updating client state.
     * @return The client model
     */
    @Override
    ClientModel getModel();
    
    /**
     * Gets the current game state if available.
     * @return The local game state or null if not in game
     */
    @Override
    LocalGameState getGameState();
    
    /**
     * Gets the notification service for showing notifications.
     * @return The notification service or null if not available
     */
    @Override
    NotificationService getNotificationService();
    
    /**
     * Shows a notification to the user.
     * @param title The notification title
     * @param message The notification message  
     * @param type The notification type
     */
    @Override
    void showNotification(String title, String message, NotificationType type);
    
    /**
     * Shows an error message to the user.
     * @param title The error title
     * @param message The error message
     */
    @Override
    void showError(String title, String message);
    
    /**
     * Gets the local player ID.
     * @return The local player ID
     */
    @Override
    String getLocalPlayerId();
    
    /**
     * Gets the current player ID (alias for getLocalPlayerId for ClientContext compatibility).
     * @return The player ID
     */
    @Override
    default String getPlayerId() {
        return getLocalPlayerId();
    }
    
    /**
     * Gets the current game ID.
     * @return The game ID or null if not in a game
     */
    @Override
    String getGameId();
    
    /**
     * Checks if the given player ID is the local player.
     * @param playerId The player ID to check
     * @return true if it's the local player
     */
    @Override
    boolean isLocalPlayer(String playerId);
    
    /**
     * Runs the given action on the UI thread.
     * @param action The action to run
     */
    @Override
    void runOnUIThread(Runnable action);
}