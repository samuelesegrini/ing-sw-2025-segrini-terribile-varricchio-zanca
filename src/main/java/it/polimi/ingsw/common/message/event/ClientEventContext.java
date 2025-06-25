package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.core.NotificationService;

/**
 * Context for handling events on the client.
 * Enhanced to support the new Simple Direct Model Architecture.
 */
public interface ClientEventContext {
    ClientState getClientState();
    
    String getLocalPlayerId();
    NotificationService getNotificationService();
    ClientController getController();

    boolean isLocalPlayer(String playerId);
    void runOnUIThread(Runnable action);
}