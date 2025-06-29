package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Event broadcast when a player successfully registers/logs in to the server.
 * Updates lobby displays to show active players.
 */
public class PlayerRegisteredEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(PlayerRegisteredEvent.class.getName());
    
    private final PlayerId playerId;
    private final String playerNickname;
    private final LocalDateTime registrationTime;

    public PlayerRegisteredEvent(PlayerId playerId, String playerNickname) {
        super(EventType.PLAYER_REGISTERED, null, playerId); // Global event (no specific game)
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.registrationTime = LocalDateTime.now();
        LOGGER.fine("PlayerRegisteredEvent created for player: " + playerNickname);
    }

    public PlayerId getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public LocalDateTime getRegistrationTime() {
        return registrationTime;
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Send to all clients for lobby updates
        return true;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            LOGGER.fine("Handling PlayerRegisteredEvent for player: " + playerNickname);
            
            // Update lobby with new active player
            if (context.getClientState() != null) {
                context.getClientState().addActivePlayer(playerId.toString(), playerNickname);
                context.getClientState().refreshCurrentViewOnly();
            }
        });
    }
}