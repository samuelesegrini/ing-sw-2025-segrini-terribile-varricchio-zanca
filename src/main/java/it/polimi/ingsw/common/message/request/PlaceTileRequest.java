package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.event.ComponentPlacedEvent;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.PlaceTileResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.ShipValidationService;
import it.polimi.ingsw.server.model.enums.GamePhase;

/**
 * Request to place a tile on the ship.
 */
public class PlaceTileRequest extends AbstractRequest {
    private final String tileId;
    private final int row;
    private final int col;
    private final int rotation; // 0, 1, 2, 3 for 0°, 90°, 180°, 270°

    public PlaceTileRequest(String tileId, int row, int col, int rotation) {
        super();
        this.tileId = tileId;
        this.row = row;
        this.col = col;
        this.rotation = rotation;
    }

    @Override
    public ValidationResult validate() {
        if (tileId == null || tileId.trim().isEmpty()) {
            return ValidationResult.failure("Tile ID is required", "tileId");
        }

        if (row < 0 || row >= 5) {
            return ValidationResult.failure("Row must be between 0 and 4", "row");
        }

        if (col < 0 || col >= 7) {
            return ValidationResult.failure("Column must be between 0 and 6", "col");
        }

        if (rotation < 0 || rotation > 3) {
            return ValidationResult.failure("Rotation must be 0, 1, 2, or 3", "rotation");
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
        GameSession session = context.getGameSession();
        if (session == null) {
            return createErrorResponse("Not in a game", ErrorResponse.INVALID_STATE);
        }

        // Check phase
        GameModel gameModel = session.getGameModel();
        if (gameModel.getCurrentPhase() != GamePhase.BUILDING) {
            return createErrorResponse("Not in building phase", ErrorResponse.INVALID_STATE);
        }

        // Get player
        PlayerId playerId = context.getPlayerId();
        System.out.println("[DEBUG] PlaceTileRequest - Player ID from context: " + playerId);
        System.out.println("[DEBUG] PlaceTileRequest - Client ID: " + context.getSenderId());
        Player player = session.getPlayer(playerId);
        System.out.println("[DEBUG] PlaceTileRequest - Player lookup result: " + (player != null ? player.getId() : "null"));
        if (player == null) {
            System.out.println("[DEBUG] PlaceTileRequest - Available players in session:");
            session.getGameModel().getPlayers().forEach(p -> 
                System.out.println("[DEBUG]   - Player: " + p.getId() + ", Nickname: " + p.getId().getNickname()));
            return createErrorResponse("Player not found", ErrorResponse.INTERNAL_ERROR);
        }

        // Get component
        Component component = session.getAvailableComponent(tileId);
        if (component == null) {
            return createErrorResponse("Component not available", ErrorResponse.NOT_FOUND);
        }

        // Apply rotation
        for (int i = 0; i < rotation; i++) {
            component.rotate();
        }

        // Try to place
        Ship ship = player.getShip();
        Position position = new Position(row, col);

        try {
            // ENHANCED: Use comprehensive real-time validation from ShipValidationService
            ShipValidationService.ValidationResult placementValidation = 
                ShipValidationService.validateComponentPlacement(ship, component, position);
            
            if (!placementValidation.isValid()) {
                // Return detailed error message from validation service
                String detailedError = String.join("; ", placementValidation.getErrors());
                return createErrorResponse(detailedError, ErrorResponse.INVALID_STATE);
            }

            // Place the component (validation already confirmed this is safe)
            ship.addComponent(component, position);

            // Mark component as used
            session.useComponent(tileId, playerId);

            // Update ship stats
            ship.updateStats();

            // Publish event FIRST - this is the single source of truth for state updates
            ComponentPlacedEvent event = new ComponentPlacedEvent(
                    session.getGameId(),
                    player,                    // Full Player model
                    component,                 // Full Component model  
                    player.getShip(),          // Updated Ship model
                    gameModel.getComponentDeck() // Updated ComponentDeck model
            );
            context.publishEvent(event);

            // Return lightweight response - event contains the state update
            return new PlaceTileResponse(getCorrelationId());

        } catch (IllegalArgumentException e) {
            // Revert rotation
            for (int i = 0; i < rotation; i++) {
                component.rotate(); // Rotate 3 more times to get back to original
                component.rotate();
                component.rotate();
            }

            return createErrorResponse(e.getMessage(), ErrorResponse.INVALID_STATE);
        }
    }
}