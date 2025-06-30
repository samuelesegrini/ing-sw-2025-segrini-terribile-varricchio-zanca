package it.polimi.ingsw.common.message.request.flight;

import it.polimi.ingsw.common.message.request.AbstractRequest;
import it.polimi.ingsw.common.message.request.RequestContext;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.DockResponse;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardController;
import it.polimi.ingsw.server.model.domain.adventure.card.AbandonedShipCard;
import it.polimi.ingsw.server.model.domain.adventure.card.AbandonedStationCard;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Request from a player to dock after either AbandonedShipCard or AbandonedStationCard.
 */
public class DockRequest extends AbstractRequest {
    private static final Logger LOGGER = Logger.getLogger(DockRequest.class.getName());
    
    private final boolean isDocking;
    private final String cardId; // To identify which card the player is responding to

    public DockRequest(boolean isDocking) {
        this(isDocking, null);
    }
    
    public DockRequest(boolean isDocking, String cardId) {
        this.isDocking = isDocking;
        this.cardId = cardId;
    }

    @Override
    public Response execute(RequestContext context) {
        try {
            PlayerId playerId = context.getPlayerId();
            if (playerId == null) {
                return new ErrorResponse(getCorrelationId(), "Player not authenticated", ErrorResponse.AUTHENTICATION_ERROR);
            }

            GameSession gameSession = context.getGameSession();
            if (gameSession == null) {
                return new ErrorResponse(getCorrelationId(), "Game session not found", ErrorResponse.NOT_FOUND);
            }

            GameModel gameModel = gameSession.getGameModel();
            if (gameModel.getCurrentPhase() != GamePhase.FLIGHT) {
                return new ErrorResponse(getCorrelationId(), "Docking only available during flight phase", ErrorResponse.INVALID_STATE);
            }

            Player player = gameModel.getPlayerById(playerId);
            if (player == null) {
                return new ErrorResponse(getCorrelationId(), "Player not found in game", ErrorResponse.NOT_FOUND);
            }

            // Get the current adventure card from the deck or controller
            Optional<AdventureCard> currentCardOpt = gameModel.getAdventureDeck().getCurrentCard();
            if (currentCardOpt.isEmpty()) {
                return new ErrorResponse(getCorrelationId(), "No active adventure card", ErrorResponse.INVALID_STATE);
            }
            
            AdventureCard currentCard = currentCardOpt.get();

            // Validate card type supports docking
            if (currentCard.getType() != AdventureType.ABANDONED_SHIP && 
                currentCard.getType() != AdventureType.ABANDONED_STATION) {
                return new ErrorResponse(getCorrelationId(), "Current card does not support docking", ErrorResponse.INVALID_STATE);
            }

            if (!isDocking) {
                // Player chooses not to dock - record the choice and continue
                LOGGER.info("Player " + player.getId().getNickname() + " chose not to dock at " + currentCard.getType());
                
                // Record the choice in adventure card controller if available
                AdventureCardController controller = gameSession.getAdventureCardController();
                if (controller != null) {
                    controller.recordPlayerChoice(player.getId(), "dock_choice", false);
                }
                
                return new DockResponse(getCorrelationId(), 0, 0, new HashMap<>(), 0);
            }

            // Player chooses to dock - process based on card type
            if (currentCard instanceof AbandonedShipCard) {
                return processDockAtAbandonedShip((AbandonedShipCard) currentCard, player, gameModel);
            } else if (currentCard instanceof AbandonedStationCard) {
                return processDockAtAbandonedStation((AbandonedStationCard) currentCard, player, gameModel);
            }

            return new ErrorResponse(getCorrelationId(), "Invalid card type for docking", ErrorResponse.INVALID_STATE);

        } catch (Exception e) {
            LOGGER.severe("Error processing dock request: " + e.getMessage());
            return new ErrorResponse(getCorrelationId(), "Internal server error during docking", ErrorResponse.INTERNAL_ERROR);
        }
    }

    /**
     * Processes docking at an abandoned ship.
     */
    private Response processDockAtAbandonedShip(AbandonedShipCard card, Player player, GameModel gameModel) {
        // Check if ship is already visited
        if (card.isVisited()) {
            return new ErrorResponse(getCorrelationId(), "Abandoned ship has already been salvaged", ErrorResponse.INVALID_STATE);
        }

        // Check crew requirement
        if (player.getShip().getCrew() <= card.getCrewLost()) {
            return new ErrorResponse(getCorrelationId(), 
                "Insufficient crew. Need more than " + card.getCrewLost() + " crew to salvage ship", ErrorResponse.VALIDATION_ERROR);
        }

        // Process the docking
        int lostFlightDays = card.getLostDays();
        int lostCrew = card.getCrewLost();
        int gainedCredits = card.getCreditsGained();

        // Apply effects
        gameModel.getFlightBoard().movePlayer(player, lostFlightDays, false);
        player.addCredits(gainedCredits);
        player.getShip().setCrew(player.getShip().getCrew() - lostCrew);
        card.setVisited();

        LOGGER.info("Player " + player.getId().getNickname() + 
                   " salvaged abandoned ship: lost " + lostCrew + " crew, gained " + gainedCredits + " credits");

        return new DockResponse(getCorrelationId(), lostFlightDays, lostCrew, new HashMap<>(), gainedCredits);
    }

    /**
     * Processes docking at an abandoned station.
     */
    private Response processDockAtAbandonedStation(AbandonedStationCard card, Player player, GameModel gameModel) {
        // Check if station is already visited
        if (card.isVisited()) {
            return new ErrorResponse(getCorrelationId(), "Abandoned station has already been looted", ErrorResponse.INVALID_STATE);
        }

        // Check crew requirement
        if (player.getShip().getCrew() < card.getMinCrewRequired()) {
            return new ErrorResponse(getCorrelationId(), 
                "Insufficient crew. Need at least " + card.getMinCrewRequired() + " crew to dock at station", ErrorResponse.VALIDATION_ERROR);
        }

        // Check if player has cargo space for goods
        Map<GoodType, Integer> stationGoods = card.getGoodQuantities();
        if (!player.getShip().addResources(stationGoods)) {
            // Not enough space - player needs to make room or decline
            return new ErrorResponse(getCorrelationId(), "Insufficient cargo space for station goods", ErrorResponse.VALIDATION_ERROR);
        }

        // If we get here, the goods were successfully added
        int lostFlightDays = card.getLostDays();
        
        // Apply effects
        gameModel.getFlightBoard().movePlayer(player, lostFlightDays, false);
        card.setVisited();

        LOGGER.info("Player " + player.getId().getNickname() + 
                   " looted abandoned station: lost " + lostFlightDays + " flight days, gained goods");

        return new DockResponse(getCorrelationId(), lostFlightDays, 0, stationGoods, 0);
    }

    public boolean isDocking() {
        return isDocking;
    }

    public String getCardId() {
        return cardId;
    }
}