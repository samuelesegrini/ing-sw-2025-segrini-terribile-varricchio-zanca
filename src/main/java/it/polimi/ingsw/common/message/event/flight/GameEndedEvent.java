package it.polimi.ingsw.common.message.event.flight;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Event broadcast when a game ends.
 * Updates client state and displays final results or end reason to players.
 */
public class GameEndedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(GameEndedEvent.class.getName());
    private final String reason;
    private final Map<String, Integer> finalScores;

    public GameEndedEvent(String gameId, String reason, Map<String, Integer> finalScores) {
        super(EventType.GAME_ENDED, gameId, null);
        this.reason = reason;
        this.finalScores = finalScores != null ? new HashMap<>(finalScores) : null;
        LOGGER.fine("GameEndedEvent instantiated for game: " + gameId + " with reason: " + reason);
    }

    public String getReason() {
        return reason;
    }

    public Map<String, Integer> getFinalScores() {
        return finalScores != null ? new HashMap<>(finalScores) : null;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update client state - game has ended
            ClientState clientState = context.getClientState();
            if (clientState != null && gameId.equals(clientState.getCurrentGameId())) {
                clientState.setGameModel(null);
                
                // Use ViewNavigator for consistent navigation
                if (context.getController() != null && 
                    context.getController().getUIContext() != null && 
                    context.getController().getUIContext().getViewNavigator() != null) {
                    
                    boolean success = context.getController().getUIContext().getViewNavigator()
                        .navigateTo(ClientState.ViewState.LOBBY, "Game ended - returning to lobby");
                    
                    if (!success) {
                        String reason = context.getController().getUIContext().getViewNavigator()
                            .getNavigationFailureReason(ClientState.ViewState.LOBBY);
                        LOGGER.severe("GameEndedEvent: ViewNavigator failed, using fallback - Reason: " + reason);
                        clientState.setCurrentView(ClientState.ViewState.LOBBY);
                    }
                } else {
                    LOGGER.severe("GameEndedEvent: ViewNavigator not available, using direct navigation fallback");
                    clientState.setCurrentView(ClientState.ViewState.LOBBY);
                }
            }

            // Show game results or end notification
            if (context.getNotificationService() != null) {
                if (finalScores != null && !finalScores.isEmpty()) {
                    // Show game results with scores
                    StringBuilder scoreText = new StringBuilder("Final Scores:\n");
                    finalScores.entrySet().stream()
                            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                            .forEach(entry -> scoreText.append(entry.getKey())
                                    .append(": ").append(entry.getValue()).append("\n"));
                    
                    context.getNotificationService().showNotification(new Notification(
                            "Game Ended",
                            scoreText.toString(),
                            NotificationType.INFO
                    ));
                } else {
                    // Show simple end notification
                    context.getNotificationService().showNotification(new Notification(
                            "Game Ended",
                            reason != null ? reason : "The game has ended",
                            NotificationType.WARNING
                    ));
                }
            }

//            // Show results in UI if available
//            if (context.getGameUI() != null) {
//                // Note: This would need to be implemented based on the actual UI interface
//                // context.getGameUI().showGameResults(finalScores, reason);
//            }
        });
    }
}