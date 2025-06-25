package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.event.ComponentReservedEvent;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.ReserveTileResponse;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.GamePhase;

/**
 * Request to reserve a component tile that is currently in the player's hand.
 * This moves the component from hand to a reservation area for later use.
 */
public class ReserveTileRequest extends AbstractRequest {
    private final String tileId;

    public ReserveTileRequest(String tileId) {
        super();
        this.tileId = tileId;
    }

    @Override
    public ValidationResult validate() {
        if (tileId == null || tileId.trim().isEmpty()) {
            return ValidationResult.failure("Tile ID is required", "tileId");
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
        if (session.getCurrentPhase() != GamePhase.BUILDING) {
            return createErrorResponse("Not in building phase", ErrorResponse.INVALID_STATE);
        }

        // Get player
        String playerId = context.getPlayerId();
        Player player = session.getPlayer(playerId);
        if (player == null) {
            return createErrorResponse("Player not found", ErrorResponse.INTERNAL_ERROR);
        }

        // For now, implement basic validation - full server logic can be added later
        // Get component by ID (simplified)
        Component component = session.getComponentById(tileId);
        if (component == null) {
            return createErrorResponse("Component not found", ErrorResponse.NOT_FOUND);
        }

        try {
            // Reserve component logic - move from hand to reserved area
            player.reserveComponent(component);

            // ENHANCED: Publish event with full server models
            ComponentReservedEvent event = new ComponentReservedEvent(
                session.getGameId(),
                component,             // Full Component model
                player,                // Full Player model
                session.getGameModel().getComponentDeck(), // Updated ComponentDeck model
                System.currentTimeMillis() + 300000 // 5 minutes reservation time
            );
            context.publishEvent(event);

            // ENHANCED: Return response with full server models
            return new ReserveTileResponse(
                getCorrelationId(),
                true,
                "Component reserved successfully",
                player,                // Full Player model
                component,             // Full Component model
                session.getGameModel().getComponentDeck() // Updated ComponentDeck model
            );

        } catch (Exception e) {
            return createErrorResponse("Failed to reserve component: " + e.getMessage(), ErrorResponse.INTERNAL_ERROR);
        }
    }

    public String getTileId() {
        return tileId;
    }
}