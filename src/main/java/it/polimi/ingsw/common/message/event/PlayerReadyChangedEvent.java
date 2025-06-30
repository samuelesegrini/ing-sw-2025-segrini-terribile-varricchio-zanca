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

    /**
     * constructor
     *
     * @param gameId the game ID
     * @param playerId the player ID
     * @param playerNickname the player nickname
     * @param ready
     */

    public PlayerReadyChangedEvent(String gameId, PlayerId playerId, String playerNickname, boolean ready) {
        super(EventType.PLAYER_READY_CHANGED, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.ready = ready;
        LOGGER.fine("PlayerReadyChangedEvent instantiated for game: " + gameId + ", player: " + playerNickname + ", ready: " + ready);
    }


    /**
     *
     * @return the player ID
     */

    public PlayerId getPlayerId() {
        return playerId;
    }


    /**
     *
     * @return the player's nickname
     */

    public String getPlayerNickname() {
        return playerNickname;
    }

    /**
     *
     * @return true if the player is ready
     */

    public boolean isReady() {
        return ready;
    }

    /**
     *
     * @param clientId The client to check
     * @param context The filter context
     *
     */

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Send to ALL players in the game, including the requester (single source of truth)
        boolean shouldSend = super.shouldSendTo(clientId, context);
        LOGGER.finer("EVENT FILTERING - PlayerReadyChangedEvent shouldSendTo clientId: " + clientId + " = " + shouldSend + " (including requester)");
        return shouldSend;
    }

    /**
     *
     * @param clientState The client state to update
     */

    @Override
    public void updateClientState(it.polimi.ingsw.client.core.ClientState clientState) {
        // Update player ready status in client state
        clientState.setPlayerReadyStatus(playerId.toString(), ready);
        clientState.incrementStateVersion();
    }

    /**
     *
     * @param context The client event context
     */

    @Override
    public void handleOnClient(ClientEventContext context) {
        // First update client state
        updateClientState(context.getClientState());
        
        context.runOnUIThread(() -> {
            LOGGER.fine("Handling PlayerReadyChangedEvent for game: " + gameId + ", player: " + playerNickname + ", ready: " + ready);
            
            // Then handle UI updates
            context.getController().getUI().onPlayerReadyChangedEvent(this);
            
            // Show notification for all players
            boolean isLocalPlayer = context.isLocalPlayer(playerId);
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
        });
    }
}