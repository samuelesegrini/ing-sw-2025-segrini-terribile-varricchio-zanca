package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.logging.Logger;

/**
 * Event broadcast when it's a specific player's turn to make a choice for an adventure card.
 */
public class AdventureCardPlayerTurnEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(AdventureCardPlayerTurnEvent.class.getName());
    private final AdventureCard card;
    private final long remainingTime;

    public AdventureCardPlayerTurnEvent(String gameId, PlayerId playerId, AdventureCard card, long remainingTime) {
        super(EventType.ADVENTURE_CARD_PLAYER_TURN, gameId, playerId);
        this.card = card;
        this.remainingTime = remainingTime;
        LOGGER.fine("AdventureCardPlayerTurnEvent instantiated for game: " + gameId + ", player: " + playerId + ", card: " + card.getClass().getSimpleName());
    }

    public AdventureCard getCard() {
        return card;
    }

    public long getRemainingTime() {
        return remainingTime;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Only show to the player whose turn it is
            if (context.isLocalPlayer(sourcePlayerId)) {
                // Show notification that it's your turn
                if (context.getNotificationService() != null) {
                    LOGGER.fine("Displaying 'Your Turn' notification for adventure card: " + card.getClass().getSimpleName());
                    context.getNotificationService().showNotification(
                        new Notification("Your Turn", 
                            "Make your choice for " + card.getClass().getSimpleName() + 
                            " (" + (remainingTime / 1000) + "s remaining)",
                            NotificationType.WARNING)
                    );
                }
                
                // Enable adventure card interaction in UI
                // This would trigger the TUI or GUI to show choice options
            } else {
                // Show to other players that someone else is choosing
                if (context.getNotificationService() != null) {
                    String playerName = sourcePlayerId != null ? sourcePlayerId.getNickname() : "Unknown";
                    LOGGER.fine("Displaying 'Waiting for Player' notification for adventure card: " + card.getClass().getSimpleName() + " for player: " + playerName);
                    context.getNotificationService().showNotification(
                        new Notification("Waiting for Player", 
                            playerName + " is making their choice...",
                            NotificationType.INFO)
                    );
                }
            }
        });
    }

    @Override
    public String toString() {
        return "AdventureCardPlayerTurnEvent{" +
                "gameId='" + gameId + '\'' +
                ", playerId=" + sourcePlayerId +
                ", card=" + (card != null ? card.getClass().getSimpleName() : "null") +
                ", remainingTime=" + remainingTime +
                '}';
    }
}