package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.state.LocalGameState;
import it.polimi.ingsw.client.ui.core.NotificationService;

/**
 * Context for handling events on the client.
 */
public interface ClientEventContext {
    LocalGameState getGameState();
    String getLocalPlayerId();
//    AudioManager getAudioManager();
    NotificationService getNotificationService();
    ClientController getController();

    boolean isLocalPlayer(String playerId);
    void runOnUIThread(Runnable action);
}