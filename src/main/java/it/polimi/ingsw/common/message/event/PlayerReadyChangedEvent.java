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
        // Don't send to the requesting client (they already have the response)
        PlayerId playerId = context.getPlayerIdForClient(clientId);
        if (this.playerId.equals(playerId)) {
            LOGGER.finer("EVENT FILTERING - PlayerReadyChangedEvent NOT sent to requesting client: " + clientId);
            return false;
        }
        
        // Use default game filtering for other clients
        return super.shouldSendTo(clientId, context);
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            LOGGER.fine("Handling PlayerReadyChangedEvent for game: " + gameId + ", player: " + playerNickname + ", ready: " + ready);
            // Show notification for other players (not the one who changed status)
            if (!context.isLocalPlayer(playerId) && context.getNotificationService() != null) {
                String message = ready 
                    ? playerNickname + " is ready to start"
                    : playerNickname + " is no longer ready";
                    
                context.getNotificationService().showInfo(
                        "Player Status",
                        message
                );
            }
            
            // Note: Player ready status is handled server-side and will be reflected
            // in game lobby updates through other events or responses
        });
    }
}