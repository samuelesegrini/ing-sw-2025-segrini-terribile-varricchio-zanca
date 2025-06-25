package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.event.ComponentOfferedEvent;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.domain.ship.components.Component;

/**
 * Request to return a held tile to the communal pile, face-up.
 */
public class ReturnTileRequest extends AbstractRequest {
    private final String tileId;

    public ReturnTileRequest(String tileId) {
        this.tileId = tileId;
    }

    @Override
    public ValidationResult validate() {
        if (tileId == null || tileId.isBlank()) {
            return ValidationResult.failure("Tile ID cannot be empty", "tileId");
        }
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

        // Server logic:
        // 1. Verify the player is actually holding this tile.
        // 2. Remove tile from player's "held" state.
        // 3. Add tile to the face-up discard pile.

        ComponentDeck deck = session.getGameModel().getComponentDeck();
        Component componentToReturn = session.getComponentById(tileId); // Assumes a way to get a component by ID

        if(componentToReturn == null) {
            return createErrorResponse("Tile not found", ErrorResponse.NOT_FOUND);
        }

        deck.discard(componentToReturn);

        // Announce the component is available to all players
        ComponentOfferedEvent event = new ComponentOfferedEvent(
                session.getGameId(), 
                componentToReturn, 
                context.getPlayerId(),
                context.getPlayerNickname()
        );
        context.getEventPublisher().publishEvent(event);

        return createSuccessResponse();
    }
}