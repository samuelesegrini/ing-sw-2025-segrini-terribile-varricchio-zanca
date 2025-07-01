package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.ViewForecastPileResponse;
import it.polimi.ingsw.common.info.AdventureCardInfo;
import java.util.List;
import java.util.logging.Logger;

public class ViewForecastPileRequest extends AbstractRequest {
    private static final Logger LOGGER = Logger.getLogger(ViewForecastPileRequest.class.getName());
    
    private final int pileIndex;

    public ViewForecastPileRequest(int pileIndex) {
        super();
        this.pileIndex = pileIndex;
    }

    public int getPileIndex() {
        return pileIndex;
    }

    @Override
    public Response execute(RequestContext context) {
        try {
            // Get the game session
            var session = context.getGameSession();
            var playerId = context.getPlayerId();
            
            // For now, return an empty list since the specific implementation 
            // depends on the adventure deck structure
            List<AdventureCardInfo> cards = List.of();
            
            LOGGER.info("Processed view forecast pile request for player: " + playerId + ", pile: " + pileIndex);
            return new ViewForecastPileResponse(getCorrelationId(), cards);
            
        } catch (Exception e) {
            LOGGER.severe("Error processing view forecast pile request: " + e.getMessage());
            return createErrorResponse("Failed to view forecast pile", "FORECAST_ERROR");
        }
    }
}
