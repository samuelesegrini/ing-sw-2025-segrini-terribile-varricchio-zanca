package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.general.BuildingTimer;

import java.util.logging.Logger;

/**
 * Event broadcast when a player flips the building timer during ship construction.
 * Implements Galaxy Trucker three-stage timer system with proper stage transitions.
 */
public class BuildingTimerFlippedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(BuildingTimerFlippedEvent.class.getName());
    private final String playerId;
    private final String playerNickname;
    private final long timeRemaining;
    private final int totalFlips;
    private final BuildingTimer.TimerStage currentStage;

    public BuildingTimerFlippedEvent(String gameId, String playerId, String playerNickname,
                                   long timeRemaining, int totalFlips, BuildingTimer.TimerStage currentStage) {
        super(EventType.BUILDING_TIMER_FLIPPED, gameId, PlayerId.fromString(playerId));
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.timeRemaining = timeRemaining;
        this.totalFlips = totalFlips;
        this.currentStage = currentStage;
        LOGGER.fine("BuildingTimerFlippedEvent instantiated for game: " + gameId + ", player: " + playerNickname + 
                   ", stage: " + currentStage + ", time: " + timeRemaining + ", total flips: " + totalFlips);
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public long getTimeRemaining() {
        return timeRemaining;
    }

    public int getTotalFlips() {
        return totalFlips;
    }

    public BuildingTimer.TimerStage getCurrentStage() {
        return currentStage;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            updateTimerDisplay(context);
            showNotification(context);
        });
    }

    private void updateTimerDisplay(ClientEventContext context) {
        // Update the timer UI component
        Object timerView = context.getTimerView();
        if (timerView != null) {
            try {
                // Use reflection to call updateStage if the method exists
                java.lang.reflect.Method updateMethod = timerView.getClass().getMethod(
                    "updateStage", 
                    it.polimi.ingsw.server.model.domain.general.BuildingTimer.TimerStage.class,
                    long.class, 
                    int.class
                );
                updateMethod.invoke(timerView, currentStage, timeRemaining, totalFlips);
            } catch (Exception e) {
                // Silently ignore if the method doesn't exist or fails
            }
        }
    }

    private void showNotification(ClientEventContext context) {
        if (context.getNotificationService() == null) {
            return;
        }

        String message;
        NotificationType type;
        boolean isLocalPlayer = context.isLocalPlayer(playerId);

        switch (currentStage) {
            case FIRST_TIMER -> {
                message = isLocalPlayer ? "You started the first timer! ⏰" 
                                       : playerNickname + " started the first timer! ⏰";
                type = NotificationType.INFO;
            }
            case SECOND_TIMER -> {
                message = isLocalPlayer ? "You flipped to the second timer! ⏰⏰" 
                                       : playerNickname + " flipped to the second timer! ⏰⏰";
                type = NotificationType.WARNING;
            }
            case BUILDING_ENDED -> {
                if ("AUTO_TIMEOUT".equals(playerId)) {
                    message = "⏰ Building phase ended automatically - time's up!";
                } else {
                    message = isLocalPlayer ? "You ended the building phase! 🏁" 
                                           : playerNickname + " ended the building phase! 🏁";
                }
                type = NotificationType.CRITICAL;
            }
            case FIRST_EXPIRED -> {
                message = "⏰ First timer expired - anyone can flip to second timer";
                type = NotificationType.WARNING;
            }
            case SECOND_EXPIRED -> {
                message = "⏰⏰ Second timer expired - finished players can end building";
                type = NotificationType.CRITICAL;
            }
            default -> {
                message = "Timer updated by " + playerNickname;
                type = NotificationType.INFO;
            }
        }

        // Add time remaining for active stages
        if (timeRemaining > 0) {
            message += " (" + (timeRemaining / 1000) + "s remaining)";
        }

        context.getNotificationService().showNotification(
            new it.polimi.ingsw.client.ui.Notification(
                "Building Timer",
                message,
                type
            )
        );

        LOGGER.fine("Displayed timer notification: " + message + " for stage: " + currentStage);
    }
}