package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

/**
 * Context for handling messages on the client side.
 */
public interface ClientContext {
    PlayerId getPlayerId();
    String getGameId();
    ClientState getClientState();
    it.polimi.ingsw.client.controller.ClientController getController();
    void showNotification(String title, String message, NotificationType type);
    void showError(String title, String message);
    
    /**
     * Gets the player ID as string (legacy compatibility).
     */
    default String getPlayerIdString() {
        PlayerId playerId = getPlayerId();
        return playerId != null ? playerId.toString() : null;
    }
    
    /**
     * Gets the timer view for displaying timer updates.
     * Returns null if no timer view is available.
     */
    default Object getTimerView() {
        return null; // Default implementation returns null
    }
}