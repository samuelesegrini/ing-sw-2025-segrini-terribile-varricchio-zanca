package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;

import java.util.UUID;

/**
 * Response containing the drawn adventure card information.
 */
public class DrawAdventureCardResponse extends AbstractResponse {
    private final AdventureCard card;

    public DrawAdventureCardResponse(UUID correlationId, AdventureCard card) {
        super(correlationId);
        this.card = card;
    }

    public AdventureCard getCard() {
        return card;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        if (isSuccess()) {
            String cardName = card != null ? card.getClass().getSimpleName() : "Unknown";
            context.showNotification(
                "Adventure Card",
                "Drew adventure card: " + cardName,
                it.polimi.ingsw.client.ui.NotificationType.INFO
            );
        } else {
            context.showError("Draw Card Error", getErrorMessage());
        }
    }

    @Override
    public String toString() {
        return "DrawAdventureCardResponse{" +
                "correlationId=" + getCorrelationId() +
                ", success=" + isSuccess() +
                ", card=" + (card != null ? card.getClass().getSimpleName() : "null") +
                '}';
    }
}