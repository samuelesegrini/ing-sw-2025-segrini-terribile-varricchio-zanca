package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.common.info.AdventureCardInfo;

import java.util.List;

public class ViewForecastPileResponse extends AbstractResponse {
    private final List<AdventureCardInfo> cards;

    public ViewForecastPileResponse(java.util.UUID correlationId, List<AdventureCardInfo> cards) {
        super(correlationId);
        this.cards = cards;
    }

    public List<AdventureCardInfo> getCards() {
        return cards;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        // Handle view forecast pile response on client
        // TODO: Implement specific client handling logic
    }
}
