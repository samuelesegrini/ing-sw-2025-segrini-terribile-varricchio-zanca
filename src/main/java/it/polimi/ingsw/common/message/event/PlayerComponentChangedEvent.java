package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.components.Component;

import java.util.logging.Logger;

/**
 * Event broadcast when a player's held component changes.
 * Updates the UI to show what component the player is currently holding.
 */
public class PlayerComponentChangedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(PlayerComponentChangedEvent.class.getName());
    
    private final PlayerId playerId;
    private final String playerNickname;
    private final Component oldComponent;
    private final Component newComponent;

    public PlayerComponentChangedEvent(String gameId, PlayerId playerId, String playerNickname, 
                                     Component oldComponent, Component newComponent) {
        super(EventType.PLAYER_COMPONENT_CHANGED, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.oldComponent = oldComponent;
        this.newComponent = newComponent;
        LOGGER.fine("PlayerComponentChangedEvent created for player: " + playerNickname + 
                   ", component: " + (oldComponent != null ? oldComponent.getId() : "null") + 
                   " -> " + (newComponent != null ? newComponent.getId() : "null"));
    }

    public PlayerId getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public Component getOldComponent() {
        return oldComponent;
    }

    public Component getNewComponent() {
        return newComponent;
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Send to all players in the game to show held component changes
        return super.shouldSendTo(clientId, context);
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            LOGGER.fine("Handling PlayerComponentChangedEvent for player: " + playerNickname + 
                       ", new component: " + (newComponent != null ? newComponent.getId() : "none"));
            
            // Update client state with new held component
            if (context.getClientState() != null) {
                context.getClientState().setPlayerHeldComponent(playerId.toString(), newComponent);
                context.getClientState().refreshCurrentViewOnly();
            }
        });
    }
}