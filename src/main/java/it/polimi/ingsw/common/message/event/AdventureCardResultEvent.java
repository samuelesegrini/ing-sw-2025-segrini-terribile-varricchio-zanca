package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.client.ui.Notification;
import it.polimi.ingsw.client.ui.NotificationType;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.Map;

/**
 * Event broadcast when an adventure card is resolved for a player during the flight phase.
 */
public class AdventureCardResultEvent extends AbstractEvent {
    private final PlayerId playerAffected;
    private final AdventureCard card;
    private final int cardNumber;

    private final int lostFlightDays;
    private final int lostCrew;
    private final Map<GoodType, Integer> lostGoods;
    private final int lostCredits;
    private final Map<GoodType, Integer> collectedGoods;
    private final int collectedCredits;

    public AdventureCardResultEvent(String gameId, PlayerId playerAffected, AdventureCard card, int cardNumber, int lostFlightDays, int lostCrew, Map<GoodType, Integer> lostGoods, int lostCredits, Map<GoodType, Integer> collectedGoods, int collectedCredits) {
        super(EventType.ADVENTURE_CARD_DRAWN, gameId, null);
        this.playerAffected = playerAffected;
        this.card = card;
        this.cardNumber = cardNumber;

        this.lostFlightDays = lostFlightDays;
        this.lostCrew = lostCrew;
        this.lostGoods = lostGoods;
        this.lostCredits = lostCredits;
        this.collectedGoods = collectedGoods;
        this.collectedCredits = collectedCredits;
    }

    public AdventureCard getCard() {
        return card;
    }

    public int getCardNumber() {
        return cardNumber;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        // TODO
    }

    /**
     * Represents an adventure card in the game.
     */
    public static class AdventureCard {
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
    public enum AdventureCardType { // TODO Non sono corrette (?)
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