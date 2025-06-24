package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.ComponentData;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.TakeTileResponse;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
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
        // The server should now associate this tile with the player, perhaps in a "held tile" state.
        // For now, we just return it to the client.

        // Create complete component data including connectors
        ComponentData componentData = new ComponentData(
            drawnComponent.getId(),
            drawnComponent.getType(),
            drawnComponent.getConnectors(),
            drawnComponent.getDirection()
        );

        // Send response to requester
        TakeTileResponse response = new TakeTileResponse(getCorrelationId(), componentData);

        // Broadcast event to all clients
        if (context.getEventPublisher() != null) {
            String nickname = null;
            if (context.getPlayerRegistry() != null) {
                nickname = context.getPlayerRegistry().getPlayerNickname(context.getPlayerId());
            }
            context.getEventPublisher().publishEvent(
                new it.polimi.ingsw.common.message.event.ComponentReservedEvent(
                    session.getGameId(),
                    componentData,
                    context.getPlayerId(),
                    nickname != null ? nickname : context.getPlayerId(),
                    System.currentTimeMillis() + 60000 // 1 min reservation for example
                )
            );
        }
        return response;
    }
}