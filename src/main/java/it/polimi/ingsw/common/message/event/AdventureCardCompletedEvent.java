package it.polimi.ingsw.common.message.event;

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

    public AdventureCardCompletedEvent(String gameId, AdventureCard card, 
                                     Map<PlayerId, AdventureCardState.PlayerChoice> playerChoices) {
        super(EventType.ADVENTURE_CARD_COMPLETED, gameId, null);
        this.card = card;
        this.playerChoices = playerChoices;
        LOGGER.fine("AdventureCardCompletedEvent instantiated for game: " + gameId + ", card: " + card.getClass().getSimpleName());
    }

    public AdventureCard getCard() {
        return card;
    }

    public Map<PlayerId, AdventureCardState.PlayerChoice> getPlayerChoices() {
        return playerChoices;
    }

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

    @Override
    public String toString() {
        return "AdventureCardCompletedEvent{" +
                "gameId='" + gameId + "'" +
                ", card=" + (card != null ? card.getClass().getSimpleName() : "null") +
                ", choiceCount=" + (playerChoices != null ? playerChoices.size() : 0) +
                '}';
    }
}
