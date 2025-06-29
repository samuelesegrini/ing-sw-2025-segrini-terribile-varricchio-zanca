package it.polimi.ingsw.common.message.event.flight;

import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;

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

    public AdventureCardDrawnEvent(String gameId, AdventureCard card, int cardNumber, int totalCards) {
        super(EventType.ADVENTURE_CARD_DRAWN, gameId, null);
        this.card = card;
        this.cardNumber = cardNumber;
        this.totalCards = totalCards;
        LOGGER.fine("AdventureCardDrawnEvent instantiated for game: " + gameId + ", card: " + card.getName() + " (" + cardNumber + "/" + totalCards + ")");
    }

    public AdventureCard getCard() {
        return card;
    }

    public int getCardNumber() {
        return cardNumber;
    }

    public int getTotalCards() {
        return totalCards;
    }

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

    private NotificationType getNotificationTypeForCard(AdventureCardType type) {
        return switch (type) {
            case OPEN_SPACE -> NotificationType.INFO;
            case PLANETS -> NotificationType.INFO;
            case METEORS -> NotificationType.WARNING;
            case COMBAT -> NotificationType.ERROR;
            case SLAVERS -> NotificationType.ERROR;
            default -> NotificationType.INFO;
        };
    }

    /**
     * Represents an adventure card in the game.
     */
    public static class AdventureCard {
        private static final Logger LOGGER = Logger.getLogger(AdventureCard.class.getName());
        private final String cardId;
        private final String name;
        private final String description;
        private final AdventureCardType type;
        private final String imageUrl;

        public AdventureCard(String cardId, String name, String description, 
                           AdventureCardType type, String imageUrl) {
            this.cardId = cardId;
            this.name = name;
            this.description = description;
            this.type = type;
            this.imageUrl = imageUrl;
            LOGGER.fine("AdventureCard instantiated: " + name + " (ID: " + cardId + ", Type: " + type + ")");
        }

        public String getCardId() {
            return cardId;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public AdventureCardType getType() {
            return type;
        }

        public String getImageUrl() {
            return imageUrl;
        }
    }

    /**
     * Types of adventure cards in Galaxy Trucker.
     */
    public enum AdventureCardType {
        OPEN_SPACE,     // Safe travel, gain resources
        PLANETS,        // Choose planets to visit for goods
        METEORS,        // Dangerous obstacles that can damage ships
        COMBAT,         // Fight pirates or other enemies
        SLAVERS,        // Special combat encounter with crew consequences
        ABANDONED_SHIP, // Salvage opportunities
        ASTEROID_FIELD, // Navigation challenges
        SABOTAGE,       // Equipment malfunctions
        ALIEN_ENCOUNTER // Diplomatic or hostile encounters
    }
}