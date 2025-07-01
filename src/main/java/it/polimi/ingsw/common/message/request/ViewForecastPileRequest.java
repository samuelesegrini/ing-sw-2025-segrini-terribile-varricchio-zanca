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
            // Get the game session and game model
            var session = context.getGameSession();
            var playerId = context.getPlayerId();
            var gameModel = session.getGameModel();
            
            LOGGER.info("Processing view forecast pile request for player: " + playerId + ", pile: " + pileIndex);
            
            // Validate that we're in the building phase
            if (session.getCurrentPhase() != it.polimi.ingsw.server.model.enums.GamePhase.BUILDING) {
                return createErrorResponse("Forecast piles can only be viewed during building phase", "INVALID_PHASE");
            }
            
            // According to Galaxy Trucker rules: players must have at least one component to view forecast piles
            var player = gameModel.getPlayerById(playerId);
            if (player == null) {
                return createErrorResponse("Player not found", "PLAYER_NOT_FOUND");
            }
            
            // Check if player has started building (has at least one component placed)
            var ship = player.getShip();
            if (ship == null || !hasAnyComponentsPlaced(ship)) {
                return createErrorResponse("You must place at least one component before viewing forecast piles", "NO_COMPONENTS_PLACED");
            }
            
            // Convert pile index to PileIdentifier
            it.polimi.ingsw.server.model.util.PileIdentifier pileId = 
                it.polimi.ingsw.server.model.util.PileIdentifier.fromIndex(pileIndex);
            
            if (pileId == it.polimi.ingsw.server.model.util.PileIdentifier.UNKNOWN) {
                return createErrorResponse("Invalid pile index: " + pileIndex, "INVALID_PILE");
            }
            
            // Get the adventure deck
            var adventureDeck = gameModel.getAdventureDeck();
            if (adventureDeck == null) {
                return createErrorResponse("Adventure deck not available", "DECK_ERROR");
            }
            
            // Check if player can view this pile
            if (!adventureDeck.canPlayerViewPile(playerId, pileId)) {
                return createErrorResponse("Cannot view pile - already being viewed by another player", "PILE_BUSY");
            }
            
            // Get the cards from the pile
            List<it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard> adventureCards = 
                adventureDeck.viewPile(playerId, pileId);
            
            // Convert AdventureCard objects to AdventureCardInfo for client transmission
            List<AdventureCardInfo> cardInfos = adventureCards.stream()
                .map(card -> new AdventureCardInfo(
                    card.getId(),
                    card.getLevel(),
                    card.getDescription(),
                    card.getType()
                ))
                .collect(java.util.stream.Collectors.toList());
            
            LOGGER.info("Successfully retrieved " + cardInfos.size() + " cards from pile " + pileIndex + " for player " + playerId);
            return new ViewForecastPileResponse(getCorrelationId(), cardInfos);
            
        } catch (IllegalStateException e) {
            LOGGER.warning("Pile viewing conflict: " + e.getMessage());
            return createErrorResponse(e.getMessage(), "PILE_CONFLICT");
        } catch (Exception e) {
            LOGGER.severe("Error processing view forecast pile request: " + e.getMessage());
            return createErrorResponse("Failed to view forecast pile", "FORECAST_ERROR");
        }
    }
    
    /**
     * Helper method to check if a ship has any components placed on it.
     * According to Galaxy Trucker rules, players must have at least one component 
     * placed before they can view forecast piles.
     */
    private boolean hasAnyComponentsPlaced(it.polimi.ingsw.server.model.domain.ship.Ship ship) {
        var board = ship.getBoard();
        if (board == null) {
            return false;
        }
        
        // Check all positions on the ship grid for any placed components
        for (int row = 0; row < ship.getRows(); row++) {
            for (int col = 0; col < ship.getCols(); col++) {
                var component = ship.getComponentAt(row, col);
                if (component != null) {
                    return true; // Found at least one component
                }
            }
        }
        return false; // No components found
    }
}
