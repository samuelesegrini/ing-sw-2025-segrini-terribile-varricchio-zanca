package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.GenericSuccessResponse;
import java.util.logging.Logger;

/**
 * Request to stop viewing a forecast pile, making it available for other players.
 * This is sent when a player finishes looking at a forecast pile during the building phase.
 */
public class StopViewingPileRequest extends AbstractRequest {
    private static final Logger LOGGER = Logger.getLogger(StopViewingPileRequest.class.getName());

    public StopViewingPileRequest() {
        super();
    }

    @Override
    public Response execute(RequestContext context) {
        try {
            // Get the game session and game model
            var session = context.getGameSession();
            var playerId = context.getPlayerId();
            var gameModel = session.getGameModel();
            
            LOGGER.info("Processing stop viewing pile request for player: " + playerId);
            
            // Validate that we're in the building phase
            if (session.getCurrentPhase() != it.polimi.ingsw.server.model.enums.GamePhase.BUILDING) {
                return createErrorResponse("Forecast piles can only be manipulated during building phase", "INVALID_PHASE");
            }
            
            // Get the adventure deck
            var adventureDeck = gameModel.getAdventureDeck();
            if (adventureDeck == null) {
                return createErrorResponse("Adventure deck not available", "DECK_ERROR");
            }
            
            // Stop viewing the pile (this will remove the player from any pile they're currently viewing)
            adventureDeck.stopViewingPile(playerId);
            
            LOGGER.info("Successfully stopped viewing pile for player " + playerId);
            return new GenericSuccessResponse(getCorrelationId());
            
        } catch (Exception e) {
            LOGGER.severe("Error processing stop viewing pile request: " + e.getMessage());
            return createErrorResponse("Failed to stop viewing pile", "STOP_VIEW_ERROR");
        }
    }
}