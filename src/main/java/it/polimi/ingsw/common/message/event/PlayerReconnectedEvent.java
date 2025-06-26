package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.logging.Logger;

/**
 * Event broadcast when a player reconnects to a game session.
 * Only notifies other players in the same game, showing that the player is back online.
 */
public class PlayerReconnectedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(PlayerReconnectedEvent.class.getName());
    private final PlayerId playerId;
    private final String playerNickname;
    private final boolean wasInActiveGame; // Whether reconnection happened during active gameplay

    public PlayerReconnectedEvent(String gameId, PlayerId playerId, String playerNickname, boolean wasInActiveGame) {
        super(EventType.PLAYER_RECONNECTED, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.wasInActiveGame = wasInActiveGame;
        LOGGER.fine("PlayerReconnectedEvent instantiated for game: " + gameId + ", player: " + playerNickname + ", was in active game: " + wasInActiveGame);
    }

    public PlayerId getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public boolean wasInActiveGame() {
        return wasInActiveGame;
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Don't send to the reconnected player (they get the reconnection response)
        PlayerId playerIdForClient = context.getPlayerIdForClient(clientId);
        if (this.playerId.equals(playerIdForClient)) {
            LOGGER.finer("EVENT FILTERING - PlayerReconnectedEvent NOT sent to reconnected player: " + clientId);
            return false;
        }
        
        // Use default game filtering for other clients in the same game
        return super.shouldSendTo(clientId, context);
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            LOGGER.fine("Handling PlayerReconnectedEvent for game: " + gameId + ", player: " + playerNickname);
            // Only show notification to other players in the game
            if (!context.isLocalPlayer(playerId) && context.getNotificationService() != null) {
                String message = wasInActiveGame 
                    ? playerNickname + " reconnected to the game"
                    : playerNickname + " reconnected to the lobby";
                    
                context.getNotificationService().showInfo(
                        "Player Reconnected",
                        message
                );
            }
        });
    }
}