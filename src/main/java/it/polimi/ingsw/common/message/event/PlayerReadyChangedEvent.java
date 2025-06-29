package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.general.GameModel;

import java.util.logging.Logger;

/**
 * Event broadcast when a player's ready status changes in the game lobby.
 * Updates the lobby UI to show which players are ready to start the game.
 */
public class PlayerReadyChangedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(PlayerReadyChangedEvent.class.getName());
    private final PlayerId playerId;
    private final String playerNickname;
    private final boolean ready;

    public PlayerReadyChangedEvent(String gameId, PlayerId playerId, String playerNickname, boolean ready) {
        super(EventType.PLAYER_READY_CHANGED, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.ready = ready;
        LOGGER.fine("PlayerReadyChangedEvent instantiated for game: " + gameId + ", player: " + playerNickname + ", ready: " + ready);
    }
    

    public PlayerId getPlayerId() {
        return playerId;
    }
    

    public String getPlayerNickname() {
        return playerNickname;
    }

    public boolean isReady() {
        return ready;
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Send to ALL players in the game, including the requester (single source of truth)
        boolean shouldSend = super.shouldSendTo(clientId, context);
        LOGGER.finer("EVENT FILTERING - PlayerReadyChangedEvent shouldSendTo clientId: " + clientId + " = " + shouldSend + " (including requester)");
        return shouldSend;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            LOGGER.fine("Handling PlayerReadyChangedEvent for game: " + gameId + ", player: " + playerNickname + ", ready: " + ready);
            
            // Update state for ALL players - this is the single source of truth
            boolean isLocalPlayer = context.isLocalPlayer(playerId);
            
            // Update the player ready status in client state
            if (context.getClientState() != null) {
                context.getClientState().setPlayerReadyStatus(playerId.toString(), ready);
                // Trigger UI refresh to show the updated state
                context.getClientState().refreshCurrentViewOnly();
            }
            
            // Show notification for all players
            if (context.getNotificationService() != null) {
                String message;
                if (isLocalPlayer) {
                    message = ready ? "You are now ready to start the game" : "You are no longer ready";
                    LOGGER.fine("Displaying ready status notification for local player: " + message);
                } else {
                    message = ready 
                        ? playerNickname + " is ready to start"
                        : playerNickname + " is no longer ready";
                    LOGGER.fine("Displaying ready status notification for other player: " + message);
                }
                
                context.getNotificationService().showNotification(
                    new it.polimi.ingsw.client.ui.Notification(
                        "Player Status",
                        message,
                        it.polimi.ingsw.client.ui.NotificationType.INFO
                    )
                );
            }

            context.getController().getUI().onPlayerReadyChangedEvent(this);
        });
    }
}