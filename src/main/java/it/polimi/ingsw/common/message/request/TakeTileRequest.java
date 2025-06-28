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
        System.out.println("[SERVER DEBUG] TakeTileRequest - context.getPlayerId(): " + context.getPlayerId());
        Player player = session.getPlayer(context.getPlayerId());
        System.out.println("[SERVER DEBUG] TakeTileRequest - Retrieved player: " + (player != null ? player.getId() : "null"));
        if (player == null) {
            return createErrorResponse("Player not found", ErrorResponse.INTERNAL_ERROR);
        }
        
        // Add component to player's hand/held tiles
        System.out.println("[SERVER DEBUG] Before addComponent - Player " + player.getId() + " held components: " + player.getHeldComponents().size());
        System.out.println("[SERVER DEBUG] Player object hash: " + System.identityHashCode(player));
        player.addComponent(drawnComponent);
        System.out.println("[SERVER DEBUG] After addComponent - Player " + player.getId() + " held components: " + player.getHeldComponents().size());
        System.out.println("[SERVER DEBUG] Component added: " + drawnComponent.getType().name() + " (ID: " + drawnComponent.getId() + ")");

        // Check if player still has component right before sending
        System.out.println("[SERVER DEBUG] Double-check before response - Player " + player.getId() + " held components: " + player.getHeldComponents().size());
        System.out.println("[SERVER DEBUG] Player object hash before response: " + System.identityHashCode(player));

        // Publish event FIRST - this is the single source of truth for state updates
        context.publishEvent(
            new it.polimi.ingsw.common.message.event.ComponentTakenEvent(
                session.getGameId(),
                drawnComponent,        // Full Component model
                player,                // Full Player model
                session.getGameModel().getComponentDeck() // Updated ComponentDeck model
            )
        );

        // Return lightweight response - event contains the state update
        TakeTileResponse response = new TakeTileResponse(getCorrelationId());
        
        return response;
    }
}