package it.polimi.ingsw.common.message.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Event when ship validation is completed.
 */
public class ShipValidationEvent extends AbstractEvent {
    private final String playerId;
    private final String playerNickname;
    private final boolean isValid;
    private final List<String> errors;

    public ShipValidationEvent(String gameId, String playerId, String playerNickname,
                               boolean isValid, List<String> errors) {
        super(EventType.SHIP_VALIDATION_COMPLETED, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.isValid = isValid;
        this.errors = new ArrayList<>(errors);
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            if (context.isLocalPlayer(playerId)) {
                // Update local game state with validation results
                context.getClientState().setShipValidation(isValid, errors);
                
                // Update client model for UI binding
                context.getController().getClientState().setShipValidation(isValid, errors);
                
                if (context.getNotificationService() != null) {
                    if (isValid) {
                        context.getNotificationService().showNotification(
                                new it.polimi.ingsw.client.ui.Notification(
                                        "Ship Valid",
                                        "Your ship passed all validation checks!",
                                        it.polimi.ingsw.client.ui.NotificationType.SUCCESS
                                )
                        );
                    } else {
                        String errorMessage = "Please fix the following errors:\n" + String.join("\n", errors);
                        context.getNotificationService().showNotification(
                                new it.polimi.ingsw.client.ui.Notification(
                                        "Ship Invalid",
                                        errorMessage,
                                        it.polimi.ingsw.client.ui.NotificationType.ERROR
                                )
                        );
                    }
                }
            } else {
                // Other player's validation result
                if (isValid) {
                    // Mark player as ready in game state
                    context.getClientState().setPlayerReady(playerId, true);
                    
                    if (context.getNotificationService() != null) {
                        context.getNotificationService().showNotification(
                                new it.polimi.ingsw.client.ui.Notification(
                                        "Player Ready",
                                        playerNickname + " is ready!",
                                        it.polimi.ingsw.client.ui.NotificationType.INFO
                                )
                        );
                    }
                }
                
                // Notify UI of player status change
                context.getController().getClientState().updatePlayerReadyStatus(playerId, isValid);
            }
        });
    }
}
