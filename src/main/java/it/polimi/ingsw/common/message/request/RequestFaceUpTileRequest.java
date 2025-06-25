package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.RequestFaceUpTileResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.common.message.event.ComponentTakenEvent;

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
    public Response execute(RequestContext context) {
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), ErrorResponse.VALIDATION_ERROR);
        }

        // Get game session
        String gameId = context.getGameId();
        String playerId = context.getPlayerId();
        String playerNickname = context.getPlayerNickname();
        
        // Check if the tile is available in face-up pile
        try {
            // Get the component from available face-up tiles
            // This would be implemented by the GameSession
            var gameSession = context.getGameSession();
            if (gameSession == null) {
                return createErrorResponse("Not in a game", ErrorResponse.INVALID_STATE);
            }
            
            // Check if building phase is active
            if (gameSession.getGameModel().getCurrentPhase() != it.polimi.ingsw.server.model.enums.GamePhase.BUILDING) {
                return createErrorResponse("Not in building phase", ErrorResponse.INVALID_STATE);
            }
            
            // Check if tile is available in face-up pile
            var component = gameSession.getFaceUpComponent(tileId);
            if (component == null) {
                return createErrorResponse("Tile not available in face-up pile", "TILE_NOT_AVAILABLE");
            }
            
            // Check if player can hold more tiles (max 2 reserved)
            var player = gameSession.getPlayer(playerId);
            if (player == null) {
                return createErrorResponse("Player not found", ErrorResponse.INTERNAL_ERROR);
            }
            
            if (gameSession.getPlayerHeldComponents(playerId).size() >= 2) {
                return createErrorResponse("Cannot hold more than 2 components", "MAX_COMPONENTS_REACHED");
            }
            
            // Reserve the component for the player
            gameSession.reserveFaceUpComponent(tileId, playerId);
            
            // Broadcast component taken event to all clients
            context.getEventPublisher().publishEvent(
                new ComponentTakenEvent(gameId, component, playerId, playerNickname)
            );
            
            // Return response with tile details
            return new RequestFaceUpTileResponse(getCorrelationId(), 
                new RequestFaceUpTileResponse.Tile(tileId, component.getType().toString(), 
                    component.getConnectors().size(), 0));
                    
        } catch (Exception e) {
            return createErrorResponse("Failed to reserve face-up tile: " + e.getMessage(), 
                ErrorResponse.INTERNAL_ERROR);
        }
    }
}