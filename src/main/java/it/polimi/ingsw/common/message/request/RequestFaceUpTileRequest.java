package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.common.event.ComponentReservedEvent;

/**
 * Request sent by a player to take a specific face-up tile from the communal pile.
 * Unlike TakeTileRequest which takes a random face-down tile, this targets a specific visible tile.
 */
public class RequestFaceUpTileRequest extends AbstractRequest {

    private final String tileId;

    public RequestFaceUpTileRequest(String tileId) {
        super();
        this.tileId = tileId;
    }

    public String getTileId() {
        return tileId;
    }

    @Override
    public ValidationResult validate() {
        if (tileId == null || tileId.trim().isEmpty()) {
            return ValidationResult.failure("Tile ID cannot be null or empty");
        }
        return ValidationResult.success();
    }

    @Override
    public void execute(RequestContext context) {
        // ... logic to check if the tile is available ...
        if (tileAvailable) {
            // Remove tile from available, add to held
            // Send response to requester
            context.sendResponse(new RequestFaceUpTileResponse(getCorrelationId(), tileId, tileType));
            // Broadcast event to all clients
            context.getEventPublisher().publishEvent(
                new ComponentReservedEvent(gameId, tileId, tileType, context.getPlayerId(), playerNickname)
            );
        } else {
            // Send error response
            context.sendResponse(new ErrorResponse(getCorrelationId(), "Tile not available"));
        }
    }
}