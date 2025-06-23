package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.client.ui.NotificationType;

/**
 * Context for handling messages on the client side.
 */
public interface ClientContext {
    String getPlayerId();
    String getGameId();
    ClientModel getModel();
    void showNotification(String title, String message, NotificationType type);
    void showError(String title, String message);
}