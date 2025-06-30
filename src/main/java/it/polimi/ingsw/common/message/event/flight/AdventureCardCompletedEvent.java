package it.polimi.ingsw.common.message.event.flight;

import it.polimi.ingsw.common.message.event.AbstractEvent;
import it.polimi.ingsw.common.message.event.ClientEventContext;
import it.polimi.ingsw.common.message.event.EventType;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardState;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Event broadcast when an adventure card has been completely resolved.
 */
public class AdventureCardCompletedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(AdventureCardCompletedEvent.class.getName());
    private final AdventureCard card;
    private final Map<PlayerId, AdventureCardState.PlayerChoice> playerChoices;

    /**
     * constructor
     *
     * @param gameId the game ID
     * @param card the adventure card
     * @param playerChoices the choice of every player
     */

    public AdventureCardCompletedEvent(String gameId, AdventureCard card, 
                                     Map<PlayerId, AdventureCardState.PlayerChoice> playerChoices) {
        super(EventType.ADVENTURE_CARD_COMPLETED, gameId, null);
        this.card = card;
        this.playerChoices = playerChoices;
        LOGGER.fine("AdventureCardCompletedEvent instantiated for game: " + gameId + ", card: " + card.getClass().getSimpleName());
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
     * @return  the choice of every player
     */

    public Map<PlayerId, AdventureCardState.PlayerChoice> getPlayerChoices() {
        return playerChoices;
    }

    /**
     *
     * @param context The client event context
     */

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update UI to show card resolution is complete
            // Clear any active choice dialogs
            // Show results summary if applicable
            
            // Update client state - card is no longer active
            if (context.getClientState() != null) {
                // Clear current adventure card state
                // This would be implemented in ClientState
            }
        });
    }

    /**
     *
     * @return the string: AdventureCardCompletedEvent{gameId= .., card= .., choiceCount= ..}
     */

    @Override
    public String toString() {
        return "AdventureCardCompletedEvent{" +
                "gameId='" + gameId + "'" +
                ", card=" + (card != null ? card.getClass().getSimpleName() : "null") +
                ", choiceCount=" + (playerChoices != null ? playerChoices.size() : 0) +
                '}';
    }
}
