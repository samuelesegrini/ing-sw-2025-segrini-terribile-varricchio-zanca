package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.core.NotificationService;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

/**
 * Context for handling events on the client.
 * Enhanced to support the new Simple Direct Model Architecture.
 */
public interface ClientEventContext {
    ClientState getClientState();
    
    PlayerId getLocalPlayerId();
    NotificationService getNotificationService();
    ClientController getController();

    boolean isLocalPlayer(PlayerId playerId);
    
    /**
     * Checks if a player ID string matches the local player (legacy compatibility).
     */
    default boolean isLocalPlayer(String playerIdString) {
        PlayerId localId = getLocalPlayerId();
        return localId != null && localId.toString().equals(playerIdString);
    }
    
    /**
     * Gets the local player ID as string (legacy compatibility).
     */
    default String getLocalPlayerIdString() {
        PlayerId playerId = getLocalPlayerId();
        return playerId != null ? playerId.toString() : null;
    }
    
    void runOnUIThread(Runnable action);
    
    /**
     * Gets the timer view for displaying timer updates.
     * Returns null if no timer view is available.
     */
    default Object getTimerView() {
        return null; // Default implementation returns null
    }
}