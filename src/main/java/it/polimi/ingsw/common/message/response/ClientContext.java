package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.client.core.ClientState;

/**
 * Context for handling messages on the client side.
 */
public interface ClientContext {
    String getPlayerId();
    String getGameId();
    ClientState getClientState();
    it.polimi.ingsw.client.controller.ClientController getController();
    void showNotification(String title, String message, NotificationType type);
    void showError(String title, String message);
}