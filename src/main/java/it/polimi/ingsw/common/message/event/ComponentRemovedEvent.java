package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.components.Component;

import java.util.logging.Logger;

/**
 * Event broadcast when a component is removed from a ship.
 * Updates the UI to show the updated ship configuration.
 */
public class ComponentRemovedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(ComponentRemovedEvent.class.getName());
    
    private final PlayerId playerId;
    private final String playerNickname;
    private final Component component;
    private final Position position;
    private final String reason;

    public ComponentRemovedEvent(String gameId, PlayerId playerId, String playerNickname,
                               Component component, Position position, String reason) {
        super(EventType.COMPONENT_REMOVED, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.component = component;
        this.position = position;
        this.reason = reason;
        LOGGER.fine("ComponentRemovedEvent created for player: " + playerNickname + 
                   ", component: " + component.getId() + " at " + position + ", reason: " + reason);
    }

    public PlayerId getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public Component getComponent() {
        return component;
    }

    public Position getPosition() {
        return position;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Send to all players in the game to show ship changes
        return super.shouldSendTo(clientId, context);
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            LOGGER.fine("Handling ComponentRemovedEvent for player: " + playerNickname + 
                       ", component: " + component.getId() + " removed from " + position);
            
            // Update client state by removing component from ship
            if (context.getClientState() != null) {
                context.getClientState().removeShipComponent(playerId.toString(), position);
                context.getClientState().refreshCurrentViewOnly();
            }
        });
    }
}