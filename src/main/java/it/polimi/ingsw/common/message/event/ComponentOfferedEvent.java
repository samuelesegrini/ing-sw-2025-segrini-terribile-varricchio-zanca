package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;

import java.util.logging.Logger;

/**
 * Event broadcast when a component tile is offered to a specific player.
 * This can happen in advanced building rules or special game scenarios.
 */
public class ComponentOfferedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(ComponentOfferedEvent.class.getName());
    private final String tileId;
    private final String tileType;
    private final String offeredToPlayerId;
    private final String offeredToPlayerNickname;
    private final String reason;
    private final long offerExpiresAt;

    public ComponentOfferedEvent(String gameId, Component component, Player player, ComponentDeck componentDeck) {
        super(EventType.COMPONENT_OFFERED, gameId, player.getId());
        this.tileId = component.getId();
        this.tileType = component.getType().toString();
        this.offeredToPlayerId = player.getId().toString();
        this.offeredToPlayerNickname = player.getNickname();
        this.reason = "Component returned to deck";
        this.offerExpiresAt = System.currentTimeMillis() + 30000; // 30 seconds from now
        LOGGER.fine("ComponentOfferedEvent instantiated for game: " + gameId + ", tile: " + tileType + ", offered to: " + offeredToPlayerNickname);
    }

    public ComponentOfferedEvent(String gameId, String tileId, String tileType, 
                                String offeredToPlayerId, String offeredToPlayerNickname,
                                String reason, long offerExpiresAt) {
        super(EventType.COMPONENT_OFFERED, gameId, PlayerId.fromString(offeredToPlayerId));
        this.tileId = tileId;
        this.tileType = tileType;
        this.offeredToPlayerId = offeredToPlayerId;
        this.offeredToPlayerNickname = offeredToPlayerNickname;
        this.reason = reason;
        this.offerExpiresAt = offerExpiresAt;
        LOGGER.fine("ComponentOfferedEvent instantiated for game: " + gameId + ", tile: " + tileType + ", offered to: " + offeredToPlayerNickname);
    }

    public String getTileId() {
        return tileId;
    }

    public String getTileType() {
        return tileType;
    }

    public String getOfferedToPlayerId() {
        return offeredToPlayerId;
    }

    public String getOfferedToPlayerNickname() {
        return offeredToPlayerNickname;
    }

    public String getReason() {
        return reason;
    }

    public long getOfferExpiresAt() {
        return offerExpiresAt;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update game state with component offer
            if (context.getClientState() != null) {
                // context.getClientState().addComponentOffer(tileId, offeredToPlayerId, offerExpiresAt);
            }

            context.getNewUI().onComponentOfferedEvent(this);

            // Show notification
            if (context.getNotificationService() != null) {
                String message;
                
                if (context.isLocalPlayer(offeredToPlayerId)) {
                    // Notification for the player receiving the offer
                    message = String.format("You have been offered a %s tile", tileType);
                    if (reason != null && !reason.isEmpty()) {
                        message += " (" + reason + ")";
                    }
                    LOGGER.fine("Displaying 'Component Offered' notification for local player: " + message);
                } else {
                    // Notification for other players
                    message = String.format("A %s tile was offered to %s", tileType, offeredToPlayerNickname);
                    LOGGER.fine("Displaying 'Component Offered' notification for other player: " + message);
                }
                
                context.getNotificationService().showInfo(
                        "Component Offered",
                        message
                );
            }

            // Show offer UI for the target player
            // if (context.isLocalPlayer(offeredToPlayerId) && context.getGameUI() != null) {
            //     // Note: This would need to be implemented based on the actual UI interface
            //     // context.getGameUI().showComponentOffer(tileId, tileType, reason, offerExpiresAt);
            // }
        });
    }
}