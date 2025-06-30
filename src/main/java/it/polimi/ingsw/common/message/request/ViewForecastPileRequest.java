package it.polimi.ingsw.common.message.request;

public class ViewForecastPileRequest extends AbstractRequest {
    private final int pileIndex;

    public ViewForecastPileRequest(String gameId, int pileIndex) {
        super(gameId);
        this.pileIndex = pileIndex;
    }

    public int getPileIndex() {
        return pileIndex;
    }
}
