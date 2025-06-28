package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Event when ship validation is completed.
 * ENHANCED VERSION: Carries full server models instead of just basic data.
 */
public class ShipValidationEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(ShipValidationEvent.class.getName());
    private final String playerId;
    private final String playerNickname;
    private final boolean isValid;
    private final List<String> errors;
    private final Player player;       // Full Player model
    private final GameModel gameModel; // Full GameModel

    // Enhanced constructor with server models
    public ShipValidationEvent(String gameId, String playerId, String playerNickname,
                               boolean isValid, List<String> errors, Player player, GameModel gameModel) {
        super(EventType.SHIP_VALIDATION_COMPLETED, gameId, PlayerId.fromString(playerId));
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.isValid = isValid;
        this.errors = new ArrayList<>(errors);
        this.player = player;
        this.gameModel = gameModel;
        LOGGER.fine("ShipValidationEvent instantiated for game: " + gameId + ", player: " + playerNickname + ", isValid: " + isValid + ", errors: " + errors.size());
    }
    
    // Legacy constructor for backward compatibility
    public ShipValidationEvent(String gameId, String playerId, String playerNickname,
                               boolean isValid, List<String> errors) {
        this(gameId, playerId, playerNickname, isValid, errors, null, null);
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public GameModel getGameModel() {
        return gameModel;
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Send to ALL players in the game, including the requester (single source of truth)
        boolean shouldSend = super.shouldSendTo(clientId, context);
        LOGGER.finer("EVENT FILTERING - ShipValidationEvent shouldSendTo clientId: " + clientId + " = " + shouldSend + " (including requester)");
        return shouldSend;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update state for ALL players - this is the single source of truth
            boolean isLocalPlayer = context.isLocalPlayer(playerId);
            
            LOGGER.fine("Handling ShipValidationEvent for player: " + playerNickname + 
                       " (local: " + isLocalPlayer + "), isValid: " + isValid);
            
            // Update client state with validation results and server models
            if (context.getClientState() != null) {
                context.getClientState().setShipValidation(isValid, errors);
                
                if (player != null) {
                    context.getClientState().updatePlayer(player);
                }
                
                if (gameModel != null) {
                    context.getClientState().setGameModel(gameModel);
                }
                
                // Mark player as ready if validation passed
                if (isValid) {
                    context.getClientState().setPlayerReady(playerId, true);
                }
                
                // Trigger UI refresh
                context.getClientState().refreshCurrentViewOnly();
            }
            
            // Show notifications for all players
            if (context.getNotificationService() != null) {
                if (isLocalPlayer) {
                    // Local player notification
                    if (isValid) {
                        LOGGER.fine("Displaying 'Ship Valid' notification for local player.");
                        context.getNotificationService().showNotification(
                                new it.polimi.ingsw.client.ui.Notification(
                                        "Ship Validated",
                                        "Your ship is ready for flight!",
                                        it.polimi.ingsw.client.ui.NotificationType.SUCCESS
                                )
                        );
                    } else {
                        String errorDetails = String.join("\n- ", errors);
                        LOGGER.fine("Displaying 'Ship Invalid' notification with errors: " + errors);
                        context.getNotificationService().showNotification(
                                new it.polimi.ingsw.client.ui.Notification(
                                        "Ship Invalid",
                                        "Your ship has the following problems:\n- " + errorDetails,
                                        it.polimi.ingsw.client.ui.NotificationType.ERROR
                                )
                        );
                    }
                } else {
                    // Other player notification
                    if (isValid) {
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
            }
        });
    }
}
