package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.state.LocalGameState;
import it.polimi.ingsw.client.ui.NotificationType;

import java.util.Map;

/**
 * Event broadcast when a player flips the building timer during ship construction.
 * Notifies all players that more time has been added to the building phase.
 */
public class BuildingTimerFlippedEvent extends AbstractEvent {
    private final String playerId;
    private final String playerNickname;
    private final long newTimeRemaining;
    private final int flipCount;

    public BuildingTimerFlippedEvent(String gameId, String playerId, String playerNickname, 
                                    long newTimeRemaining, int flipCount) {
        super(EventType.BUILDING_TIMER_FLIPPED, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.newTimeRemaining = newTimeRemaining;
        this.flipCount = flipCount;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public long getNewTimeRemaining() {
        return newTimeRemaining;
    }

    public int getFlipCount() {
        return flipCount;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update game state with new timer information
            var gameState = LocalGameState.getInstance();
            gameState.updateBuildingTimer(newTimeRemaining);
            gameState.setBuildingTimerFlipped(true);


            // Show notification
            if (context.getNotificationService() != null) {
                String message;
                
                if (context.isLocalPlayer(playerId)) {
                    // Notification for the player who flipped the timer
                    message = String.format("You flipped the timer! %d seconds remaining", newTimeRemaining / 1000);
                } else {
                    // Notification for other players
                    message = String.format("%s flipped the timer! %d seconds remaining", 
                            playerNickname, newTimeRemaining / 1000);
                }
                
                // Add warning if timer has been flipped multiple times
                NotificationType notificationType =
                    flipCount > 2 ? NotificationType.WARNING 
                                  : NotificationType.INFO;
                
                if (flipCount > 2) {
                    message += " (Timer flipped " + flipCount + " times)";
                }
                
                context.getNotificationService().showNotification(
                    new it.polimi.ingsw.client.ui.Notification(
                        "Timer Flipped",
                        message,
                        notificationType
                    )
                );
            }

            // Fire property change events for UI updates
            if (context.getController() != null && context.getController().getModel() != null) {
                context.getController().getModel().firePropertyChange("buildingTimerUpdated", 
                    null, newTimeRemaining);
                context.getController().getModel().firePropertyChange("buildingTimerFlipped", 
                    null, Map.of("playerId", playerId, "flipCount", flipCount));
            }
        });
    }
}