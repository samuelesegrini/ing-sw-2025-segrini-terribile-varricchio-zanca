package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.RequestFaceUpTileResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.common.message.event.ComponentTakenEvent;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

/**
 * Request sent by a player to take a specific face-up tile from the communal pile.
 * Unlike TakeTileRequest which takes a random face-down tile, this targets a specific visible tile.
 */
public class RequestFaceUpTileRequest extends AbstractRequest {

    private final String tileId;

    /**
     * constructor
     *
     * @param tileId the requested tile ID
     */

    public RequestFaceUpTileRequest(String tileId) {
        super();
        this.tileId = tileId;
    }

    /**
     *
     * @return the requested tile ID
     */

    public String getTileId() {
        return tileId;
    }

    /**
     *
     * @return a failure message or a success
     */

    @Override
    public ValidationResult validate() {
        if (tileId == null || tileId.trim().isEmpty()) {
            return ValidationResult.failure("Tile ID cannot be null or empty");
        }
        return ValidationResult.success();
    }

    /**
     *
     * @param context The execution context providing access to server resources
     * @return an ErrorResponse or a RequestFaceUpTileResponse
     */

    @Override
    public Response execute(RequestContext context) {
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), ErrorResponse.VALIDATION_ERROR);
        }

        // Get game session
        String gameId = context.getGameId();
        PlayerId playerId = context.getPlayerId();
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
            
            // Check if player can hold more tiles first
            var player = gameSession.getPlayer(playerId);
            if (player == null) {
                return createErrorResponse("Player not found", ErrorResponse.INTERNAL_ERROR);
            }
            
            // Check if player already has a held component (max 1 in this model)
            if (player.getHeldComponent() != null) {
                return createErrorResponse("Player already holding a component", "MAX_COMPONENTS_REACHED");
            }
            
            // Check if tile is available in face-up pile before taking it
            var componentDeck = gameSession.getGameModel().getComponentDeck();
            boolean tileAvailable = componentDeck.getFaceUpComponents().stream()
                .anyMatch(comp -> comp.getId().equals(tileId));
            
            if (!tileAvailable) {
                return createErrorResponse("Tile not available in face-up pile", "TILE_NOT_AVAILABLE");
            }
            
            // Take the component from face-up pile
            var component = componentDeck.takeFaceUpComponentById(tileId);
            if (component == null) {
                return createErrorResponse("Failed to take tile from face-up pile", "TILE_NOT_AVAILABLE");
            }
            
            // Set the component as the player's held component
            player.setHeldComponent(component);
            
            // Fire the ComponentTakenEvent manually using the same pattern as GameModel.takeComponent()
            var gameModel = gameSession.getGameModel();
            if (gameModel.getPropertyChangeSupport() != null) {
                ComponentTakenEvent event = new ComponentTakenEvent(
                    gameSession.getGameId(), 
                    component, 
                    player, 
                    componentDeck
                );
                gameModel.getPropertyChangeSupport().firePropertyChange("eventPublished", null, event);
            }
            
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