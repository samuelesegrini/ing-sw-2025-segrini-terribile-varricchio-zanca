package it.polimi.ingsw.common.message.request.flight;

import it.polimi.ingsw.common.message.request.AbstractRequest;
import it.polimi.ingsw.common.message.request.RequestContext;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.DrawAdventureCardResponse;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.common.message.event.AdventureCardDrawnEvent;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardController;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;

import java.util.Optional;

/**
 * Request to draw the next adventure card during the flight phase.
 */
public class DrawAdventureCardRequest extends AbstractRequest {

    public DrawAdventureCardRequest() {
        super();
    }

    @Override
    public ValidationResult validate() {
        return ValidationResult.success();
    }

    @Override
    public Response execute(RequestContext context) {
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), ErrorResponse.VALIDATION_ERROR);
        }

        GameSession session = context.getGameSession();
        if (session == null) {
            return createErrorResponse("Not in a game", ErrorResponse.INVALID_STATE);
        }

        if (session.getCurrentPhase() != GamePhase.FLIGHT) {
            return createErrorResponse("Can only draw adventure cards during flight phase", 
                                     ErrorResponse.INVALID_STATE);
        }

        try {
            GameModel gameModel = session.getGameModel();
            
            // Draw card using existing complete system
            Optional<AdventureCard> cardOpt = gameModel.getAdventureDeck().drawNextCard();
            if (cardOpt.isEmpty()) {
                // No more cards, end flight phase
                gameModel.changePhase(GamePhase.END);
                return createErrorResponse("Flight phase ended - no more cards", ErrorResponse.INVALID_STATE);
            }

            AdventureCard card = cardOpt.get();

            // Create event-compatible card representation
            AdventureCardDrawnEvent.AdventureCard eventCard = new AdventureCardDrawnEvent.AdventureCard(
                card.getId(),
                card.getClass().getSimpleName(),
                card.getDescription(),
                convertToEventCardType(card.getType()),
                "" // imageUrl - could be derived from card type
            );

            // Start turn-based card resolution using the new controller
            AdventureCardController cardController = session.getAdventureCardController();
            if (cardController != null) {
                cardController.startCardResolution(card);
            }
            
            // Publish event FIRST - this is the single source of truth for state updates
            context.publishEvent(new AdventureCardDrawnEvent(
                context.getGameId(), 
                eventCard,
                gameModel.getAdventureDeck().getMainFlightDeckView().size() - gameModel.getAdventureDeck().getRemainingCardsInFlightDeck(),
                gameModel.getAdventureDeck().getMainFlightDeckView().size()
            ));

            // Return lightweight response - event contains the state update
            return new DrawAdventureCardResponse(getCorrelationId());
        } catch (Exception e) {
            return createErrorResponse("Failed to draw adventure card: " + e.getMessage(), 
                                     ErrorResponse.INTERNAL_ERROR);
        }
    }

    /**
     * Converts domain AdventureType to event AdventureCardType.
     */
    private AdventureCardDrawnEvent.AdventureCardType convertToEventCardType(AdventureType type) {
        return switch (type) {
            case OPEN_SPACE -> AdventureCardDrawnEvent.AdventureCardType.OPEN_SPACE;
            case PLANETS -> AdventureCardDrawnEvent.AdventureCardType.PLANETS;
            case METEOR_SWARM -> AdventureCardDrawnEvent.AdventureCardType.METEORS;
            case PIRATES, WAR_ZONE -> AdventureCardDrawnEvent.AdventureCardType.COMBAT;
            case SLAVERS -> AdventureCardDrawnEvent.AdventureCardType.SLAVERS;
            case ABANDONED_SHIP, ABANDONED_STATION -> AdventureCardDrawnEvent.AdventureCardType.ABANDONED_SHIP;
            default -> AdventureCardDrawnEvent.AdventureCardType.OPEN_SPACE;
        };
    }
}