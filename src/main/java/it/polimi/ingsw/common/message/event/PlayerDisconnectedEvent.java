package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.logging.Logger;

/**
 * Event broadcast when a player disconnects from a game session (network disconnection).
 * This is different from PlayerLeftGameEvent which is for intentional leaving.
 * Only notifies other players in the same game, showing that the player is temporarily unavailable.
 */
public class PlayerDisconnectedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(PlayerDisconnectedEvent.class.getName());
    private final PlayerId playerId;
    private final String playerNickname;
    private final boolean isInActiveGame; // Whether disconnection happened during active gameplay

    /**
     *
     * @param gameId the game ID
     * @param playerId the player ID
     * @param playerNickname the player nickname
     * @param isInActiveGame
     */

    public PlayerDisconnectedEvent(String gameId, PlayerId playerId, String playerNickname, boolean isInActiveGame) {
        super(EventType.PLAYER_DISCONNECTED, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.isInActiveGame = isInActiveGame;
        LOGGER.fine("PlayerDisconnectedEvent instantiated for game: " + gameId + ", player: " + playerNickname + ", in active game: " + isInActiveGame);
    }

    /**
     *
     * @return  the player ID
     */

    public PlayerId getPlayerId() {
        return playerId;
    }

    /**
     *
     * @return  the player nickname
     */

    public String getPlayerNickname() {
        return playerNickname;
    }

    /**
     *
     * @return true if the player is in an active game
     */

    public boolean isInActiveGame() {
        return isInActiveGame;
    }

    /**
     *
     * @param clientId The client to check
     * @param context The filter context
     *
     */

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Don't send to the disconnected player (they're not connected anyway)
        PlayerId playerIdForClient = context.getPlayerIdForClient(clientId);
        if (this.playerId.equals(playerIdForClient)) {
            LOGGER.finer("EVENT FILTERING - PlayerDisconnectedEvent NOT sent to disconnected player: " + clientId);
            return false;
        }
        
        // Use default game filtering for other clients in the same game
        return super.shouldSendTo(clientId, context);
    }

    /**
     *
     * @param context The client event context
     */

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            LOGGER.fine("Handling PlayerDisconnectedEvent for game: " + gameId + ", player: " + playerNickname);
            // Only show notification to other players in the game
            if (!context.isLocalPlayer(playerId) && context.getNotificationService() != null) {
                String message = isInActiveGame 
                    ? playerNickname + " disconnected from the game (may reconnect)"
                    : playerNickname + " disconnected from the lobby";
                    
                context.getNotificationService().showWarning(
                        "Player Disconnected",
                        message
                );
            }
        });
    }
}