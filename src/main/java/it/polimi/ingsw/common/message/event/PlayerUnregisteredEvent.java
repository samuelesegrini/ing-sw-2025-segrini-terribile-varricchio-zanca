package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Event broadcast when a player disconnects from the server.
 * Updates lobby displays to remove inactive players.
 */
public class PlayerUnregisteredEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(PlayerUnregisteredEvent.class.getName());
    
    private final PlayerId playerId;
    private final String playerNickname;
    private final String reason;
    private final LocalDateTime unregistrationTime;

    /**
     * constructor
     *
     * @param playerId the player ID
     * @param playerNickname the player nickname
     * @param reason the reason
     */

    public PlayerUnregisteredEvent(PlayerId playerId, String playerNickname, String reason) {
        super(EventType.PLAYER_UNREGISTERED, null, playerId); // Global event (no specific game)
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.reason = reason;
        this.unregistrationTime = LocalDateTime.now();
        LOGGER.fine("PlayerUnregisteredEvent created for player: " + playerNickname + ", reason: " + reason);
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
     * @return  the player nickname
     */

    public String getPlayerNickname() {
        return playerNickname;
    }

    /**
     *
     * @return the reason
     */

    public String getReason() {
        return reason;
    }

    /**
     *
     * @return the unregistration time
     */

    public LocalDateTime getUnregistrationTime() {
        return unregistrationTime;
    }

    /**
     *
     * @param clientId The client to check
     * @param context The filter context
     * @return true
     */

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Send to all clients for lobby updates
        return true;
    }

    /**
     *
     * @param context The client event context
     */

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            LOGGER.fine("Handling PlayerUnregisteredEvent for player: " + playerNickname);
            
            // Remove player from lobby displays
            if (context.getClientState() != null) {
                context.getClientState().removeActivePlayer(playerId.toString());
                context.getClientState().refreshCurrentViewOnly();
            }
        });
    }
}