package it.polimi.ingsw.common.message.event.flight;

import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.logging.Logger;

/**
 * Event broadcast when a player's turn times out during adventure card resolution.
 */
public class AdventureCardTimeoutEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(AdventureCardTimeoutEvent.class.getName());
    private final AdventureCard card;

    /**
     * constructor
     *
     * @param gameId the game ID
     * @param playerId the player ID
     * @param card the adventure card during which the player's turn timed out
     */

    public AdventureCardTimeoutEvent(String gameId, PlayerId playerId, AdventureCard card) {
        super(EventType.ADVENTURE_CARD_TIMEOUT, gameId, playerId);
        this.card = card;
        LOGGER.fine("AdventureCardTimeoutEvent instantiated for game: " + gameId + ", player: " + playerId + ", card: " + card.getClass().getSimpleName());
    }

    /**
     *
     * @return the adventure card during which the player's turn timed out
     */

    public AdventureCard getCard() {
        return card;
    }

    /**
     *
     * @param context The client event context
     */

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            if (context.isLocalPlayer(sourcePlayerId)) {
                // Show timeout notification to the player who timed out
                if (context.getNotificationService() != null) {
                    LOGGER.fine("Displaying 'Your Turn' timeout notification for adventure card: " + card.getClass().getSimpleName());
                    context.getNotificationService().showNotification(
                        new Notification("Turn Timeout", 
                            "Your turn timed out - skipped automatically",
                            NotificationType.WARNING)
                    );
                }
            } else {
                // Show to other players
                if (context.getNotificationService() != null) {
                    String playerName = sourcePlayerId != null ? sourcePlayerId.getNickname() : "Unknown";
                    LOGGER.fine("Displaying 'Player Timeout' notification for adventure card: " + card.getClass().getSimpleName() + " for player: " + playerName);
                    context.getNotificationService().showNotification(
                        new Notification("Player Timeout", 
                            playerName + " timed out and was skipped",
                            NotificationType.INFO)
                    );
                }
            }
        });
    }

    /**
     *
     * @return a string: AdventureCardTimeoutEvent{gameId= .., playerId= .., card= .. }
     */

    @Override
    public String toString() {
        return "AdventureCardTimeoutEvent{" +
                "gameId='" + gameId + "'" +
                ", playerId=" + sourcePlayerId +
                ", card=" + (card != null ? card.getClass().getSimpleName() : "null") +
                '}';
    }
}