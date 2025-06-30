package it.polimi.ingsw.common.message.event.flight;

import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;

import java.util.logging.Logger;

/**
 * Event broadcast when a new adventure card is revealed during the flight phase.
 * Triggers the adventure card UI and initiates the card's effects.
 */
public class AdventureCardDrawnEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(AdventureCardDrawnEvent.class.getName());
    private final AdventureCard card;
    private final int cardNumber;
    private final int totalCards;

    /**
     * constructor
     *
     * @param gameId the game ID
     * @param card the adventure card
     * @param cardNumber
     * @param totalCards the total number of cards
     */

    public AdventureCardDrawnEvent(String gameId, AdventureCard card, int cardNumber, int totalCards) {
        super(EventType.ADVENTURE_CARD_DRAWN, gameId, null);
        this.card = card;
        this.cardNumber = cardNumber;
        this.totalCards = totalCards;
        LOGGER.fine("AdventureCardDrawnEvent instantiated for game: " + gameId + ", card: " + card.getName() + " (" + cardNumber + "/" + totalCards + ")");
    }

    /**
     *
     * @return  the adventure card
     */

    public AdventureCard getCard() {
        return card;
    }

    /**
     *
     * @return the card number
     */

    public int getCardNumber() {
        return cardNumber;
    }

    /**
     *
     * @return the total number of cards
     */

    public int getTotalCards() {
        return totalCards;
    }

    /**
     *
     * @param context The client event context
     */

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update game state with new adventure card
            if (context.getClientState() != null) {
                // context.getClientState().setCurrentAdventureCard(card);
                // context.getClientState().setCardProgress(cardNumber, totalCards);
            }

            // Show adventure card notification
            if (context.getNotificationService() != null) {
                LOGGER.fine("Displaying notification for drawn adventure card: " + card.getName());
                context.getNotificationService().showNotification(new Notification(
                        "Adventure Card " + cardNumber + "/" + totalCards,
                        card.getName() + " - " + card.getDescription(),
                        getNotificationTypeForCard(card.getType())
                ));
            }

            // Display adventure card in UI
            // if (context.getGameUI() != null) {
            //     // Note: This would need to be implemented based on the actual UI interface
            //     // context.getGameUI().showAdventureCard(card, cardNumber, totalCards);
            //     // context.getGameUI().startCardResolution(card);
            // }
        });
    }

    /**
     *
     * @param type
     * @return the type of the adventure card
     */

    private NotificationType getNotificationTypeForCard(AdventureType type) {
        return switch (type) {
            case OPEN_SPACE -> NotificationType.INFO;
            case PLANETS -> NotificationType.INFO;
            case METEOR_SWARM -> NotificationType.WARNING;
            case PIRATES, COMBAT_ZONE -> NotificationType.ERROR;
            case SLAVERS -> NotificationType.ERROR;
            default -> NotificationType.INFO;
        };
    }

}