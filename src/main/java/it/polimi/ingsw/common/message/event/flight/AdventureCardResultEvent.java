package it.polimi.ingsw.common.message.event.flight;

import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Event broadcast when an adventure card is resolved for a player during the flight phase.
 */
public class AdventureCardResultEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(AdventureCardResultEvent.class.getName());
    private final PlayerId playerAffected;
    private final AdventureCard card;
    private final int cardNumber;

    private final int lostFlightDays;
    private final int lostCrew;
    private final Map<GoodType, Integer> lostGoods;
    private final int lostCredits;
    private final Map<GoodType, Integer> collectedGoods;
    private final int collectedCredits;

    /**
     * constructor
     *
     * @param gameId
     * @param playerAffected
     * @param card
     * @param cardNumber
     * @param lostFlightDays
     * @param lostCrew
     * @param lostGoods
     * @param lostCredits
     * @param collectedGoods
     * @param collectedCredits
     */

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
        LOGGER.fine("AdventureCardResultEvent instantiated for game: " + gameId + ", player: " + playerAffected + ", card: " + card.getName());
    }

    /**
     *
     * @return the card  resolved for a player during the flight phase
     */

    public AdventureCard getCard() {
        return card;
    }


    /**
     *
     * @return the number of the card
     */

    public int getCardNumber() {
        return cardNumber;
    }

    /**
     *
     * @param context The client event context
     */

    @Override
    public void handleOnClient(ClientEventContext context) {
        // TODO
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

        /**
         * constructor
         *
         * @param cardId
         * @param name
         * @param description
         * @param type
         * @param imageUrl
         */

        public AdventureCard(String cardId, String name, String description, 
                           AdventureCardType type, String imageUrl) {
            this.cardId = cardId;
            this.name = name;
            this.description = description;
            this.type = type;
            this.imageUrl = imageUrl;
            LOGGER.fine("AdventureCard instantiated: " + name + " (ID: " + cardId + ", Type: " + type + ")");
        }

        /**
         *
         * @return the card ID
         */

        public String getCardId() {
            return cardId;
        }

        /**
         *
         * @return the name of the card
         */

        public String getName() {
            return name;
        }

        /**
         *
         * @return the description of the card
         */

        public String getDescription() {
            return description;
        }

        /**
         *
         * @return the type of the card
         */

        public AdventureCardType getType() {
            return type;
        }

        /**
         *
         * @return the image url of the card
         */

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