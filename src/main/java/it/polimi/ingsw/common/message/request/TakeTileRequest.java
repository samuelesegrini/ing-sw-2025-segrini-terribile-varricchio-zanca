package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.TakeTileResponse;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.enums.GamePhase;

import java.util.Optional;

/**
 * Request to take a random, face-down component tile from the communal pile.
 */
public class TakeTileRequest extends AbstractRequest {

    @Override
    public Response execute(RequestContext context) {
        GameSession session = context.getGameSession();
        if (session == null) {
            return createErrorResponse("Not in a game", ErrorResponse.INVALID_STATE);
        }
        if (session.getCurrentPhase() != GamePhase.BUILDING) {
            return createErrorResponse("Not in building phase", ErrorResponse.INVALID_STATE);
        }

        ComponentDeck deck = session.getGameModel().getComponentDeck();
        Optional<Component> drawnComponentOpt = deck.draw();

        if (drawnComponentOpt.isEmpty()) {
            return createErrorResponse("No tiles left in the deck", ErrorResponse.INVALID_STATE);
        }

        Component drawnComponent = drawnComponentOpt.get();
        
        // Get player and update their state
        Player player = session.getPlayer(context.getPlayerId());
        if (player == null) {
            return createErrorResponse("Player not found", ErrorResponse.INTERNAL_ERROR);
        }
        
        // Add component to player's hand/held tiles
        player.addHeldComponent(drawnComponent);

        // ENHANCED: Send response with full server models
        TakeTileResponse response = new TakeTileResponse(
            getCorrelationId(),
            true,
            "Component taken successfully", 
            player,                    // Full Player model
            drawnComponent,            // Full Component model
            session.getGameModel().getComponentDeck() // Updated ComponentDeck model
        );

        // ENHANCED: Broadcast event with full server models
        context.publishEvent(
            new it.polimi.ingsw.common.message.event.ComponentTakenEvent(
                session.getGameId(),
                drawnComponent,        // Full Component model
                player,                // Full Player model
                session.getGameModel().getComponentDeck() // Updated ComponentDeck model
            )
        );
        
        return response;
    }
}