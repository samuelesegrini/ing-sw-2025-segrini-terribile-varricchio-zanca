package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.player.PlayerId;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Event when ship validation is completed.
 */
public class ShipValidationEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(ShipValidationEvent.class.getName());
    private final String playerId;
    private final String playerNickname;
    private final boolean isValid;
    private final List<String> errors;

    public ShipValidationEvent(String gameId, String playerId, String playerNickname,
                               boolean isValid, List<String> errors) {
        super(EventType.SHIP_VALIDATION_COMPLETED, gameId, PlayerId.fromString(playerId));
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.isValid = isValid;
        this.errors = new ArrayList<>(errors);
        LOGGER.fine("ShipValidationEvent instantiated for game: " + gameId + ", player: " + playerNickname + ", isValid: " + isValid + ", errors: " + errors.size());
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Don't send to the requesting client (they get the response instead)
        PlayerId clientPlayerId = context.getPlayerIdForClient(clientId);
        if (PlayerId.fromString(playerId).equals(clientPlayerId)) {
            LOGGER.finer("EVENT FILTERING - ShipValidationEvent NOT sent to requesting client: " + clientId);
            return false; // Exclude the requesting client
        }
        
        // Use default game filtering for other clients
        return super.shouldSendTo(clientId, context);
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            if (context.isLocalPlayer(playerId)) {
                LOGGER.fine("Handling ShipValidationEvent for local player: " + playerId + ", isValid: " + isValid);
                // Update local game state with validation results
                context.getClientState().setShipValidation(isValid, errors);
                
                // Update client model for UI binding
                context.getController().getClientState().setShipValidation(isValid, errors);
                
                if (context.getNotificationService() != null) {
                    if (isValid) {
                        LOGGER.fine("Displaying 'Ship Valid' notification.");
                        context.getNotificationService().showNotification(
                                new it.polimi.ingsw.client.ui.Notification(
                                        "Ship Valid",
                                        "Your ship passed all validation checks!",
                                        it.polimi.ingsw.client.ui.NotificationType.SUCCESS
                                )
                        );
                    } else {
                        String errorMessage = "Please fix the following errors:\n" + String.join("\n", errors);
                        LOGGER.fine("Displaying 'Ship Invalid' notification with errors: " + errors);
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
                LOGGER.fine("Handling ShipValidationEvent for other player: " + playerId + ", isValid: " + isValid);
                // Other player's validation result
                if (isValid) {
                    // Mark player as ready in game state
                    context.getClientState().setPlayerReady(playerId, true);
                    
                    if (context.getNotificationService() != null) {
                        LOGGER.fine("Displaying 'Player Ready' notification for player: " + playerNickname);
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
