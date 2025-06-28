package it.polimi.ingsw.common.message.request;

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

        // Use GameSession method which handles both business logic and event firing
        boolean success = session.returnComponent(context.getPlayerId(), tileId);
        if (!success) {
            return createErrorResponse("Tile not found", ErrorResponse.NOT_FOUND);
        }

        return createSuccessResponse();
    }
}