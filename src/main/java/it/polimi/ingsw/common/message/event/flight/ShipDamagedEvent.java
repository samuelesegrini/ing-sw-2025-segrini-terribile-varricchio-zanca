package it.polimi.ingsw.common.message.event.flight;

// REMOVED: LocalGameState no longer needed
import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;

import java.util.logging.Logger;

/**
 * Event broadcast when a ship component is damaged or destroyed.
 * Updates the game state and provides visual/audio feedback to players.
 */
public class ShipDamagedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(ShipDamagedEvent.class.getName());
    private final String playerId;
    private final String playerNickname;
    private final int row, col;
    private final String damageSource;
    private final ComponentType componentLost;

    /**
     * constructor
     *
     * @param gameId the game ID
     * @param playerId the player ID
     * @param playerNickname the player nickname
     * @param row the row of the damage
     * @param col the column of the damage
     * @param damageSource what damaged the ship
     * @param componentLost type of the lost component
     */

    public ShipDamagedEvent(String gameId, String playerId, String playerNickname,
                            int row, int col, String damageSource, ComponentType componentLost) {
        super(EventType.SHIP_DAMAGED, gameId, PlayerId.fromString(playerId));
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.row = row;
        this.col = col;
        this.damageSource = damageSource;
        this.componentLost = componentLost;
        LOGGER.fine("ShipDamagedEvent instantiated for game: " + gameId + ", player: " + playerNickname + ", component: " + componentLost + " at (" + row + "," + col + ") by " + damageSource);
    }

    /**
     *
     * @return  the player ID
     */

    public String getPlayerId() {
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
     * @return the row where the damage is
     */

    public int getRow() {
        return row;
    }

    /**
     *
     * @return  the column where the damage is
     */

    public int getCol() {
        return col;
    }

    /**
     *
     * @return  what damaged the ship
     */

    public String getDamageSource() {
        return damageSource;
    }

    /**
     *
     * @return the type of the lost component
     */

    public ComponentType getComponentLost() {
        return componentLost;
    }

    /**
     *
     * @param context The client event context
     */

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            LOGGER.fine("Handling ShipDamagedEvent for player: " + playerId + ", component: " + componentLost);
            // Show notification about the damage
            if (context.getNotificationService() != null) {
                String message = context.isLocalPlayer(playerId)
                        ? "Your " + (componentLost != null ? componentLost.name().toLowerCase() : "component") + " was destroyed by " + damageSource
                        : playerNickname + " lost a " + (componentLost != null ? componentLost.name().toLowerCase() : "component");

                context.getNotificationService().showNotification(new Notification(
                        "Ship Damaged!",
                        message,
                        context.isLocalPlayer(playerId) ? NotificationType.WARNING : NotificationType.INFO
                ));
            }

            // Update UI if available
            // if (context.getGameUI() != null) {
            //     // Note: These would need to be implemented based on the actual UI interface
            //     // context.getGameUI().showDamageAnimation(playerId, row, col, damageSource);
            //     // context.getGameUI().refreshShipDisplay(playerId);
            //     // context.getGameUI().updateShipStats(playerId);
            // }
        });
    }
}