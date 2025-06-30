package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.common.info.AdventureCardInfo;

import java.util.List;

public class ViewForecastPileResponse extends AbstractResponse {
    private final List<AdventureCardInfo> cards;

    public ViewForecastPileResponse(String gameId, List<AdventureCardInfo> cards) {
        super(gameId);
        this.cards = cards;
    }

    public List<AdventureCardInfo> getCards() {
        return cards;
    }
}
